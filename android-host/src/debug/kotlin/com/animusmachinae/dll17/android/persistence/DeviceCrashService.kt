package com.animusmachinae.dll17.android.persistence

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.animusmachinae.dll17.core.continuity.EncryptedRecordStore
import com.animusmachinae.dll17.core.crypto.ChaCha20Poly1305
import com.animusmachinae.dll17.core.persistence.LocalKeyStore
import com.animusmachinae.dll17.core.persistence.PersistenceBackendContract
import com.animusmachinae.dll17.core.persistence.SegmentedJournalMedium
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

/**
 * Real process death, in a real Android process.
 *
 * The desktop fault matrix kills a child JVM with `Runtime.halt`. This is the
 * same experiment on the platform the organism actually ships to: a separate
 * Android process, holding app-private storage open, killed without running a
 * shutdown hook, a finalizer, `onDestroy`, or any of the tidying the framework
 * would otherwise do on the way down.
 *
 * ### What this proves, and what it does not
 *
 * It proves the behaviour of the file layout across abrupt process death on
 * Android: a torn frame is dropped, a staging file is discarded, an atomic
 * rename is all-or-nothing, an interrupted rewrap resolves deterministically.
 *
 * It does **not** prove that an acknowledged write survives sudden power loss,
 * and nothing in this package claims otherwise. Killing a process leaves the
 * kernel page cache untouched, so bytes that were written and never forced are
 * still readable to the next process. Distinguishing those cases needs the power
 * removed from the device at the relevant instant, which no test on the device
 * can arrange from inside it.
 *
 * ### Synchronisation
 *
 * The caller cannot join an Android process. Instead the child writes a marker
 * file as its last act before halting, and the caller waits for the marker and
 * then for the process to disappear. The marker lives outside the storage
 * directory under test so it cannot perturb what is being measured.
 */
public class DeviceCrashService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mode = intent?.getStringExtra(EXTRA_MODE)?.let { Mode.valueOf(it) }
        val directory = intent?.getStringExtra(EXTRA_DIRECTORY)?.let(::File)
        val marker = intent?.getStringExtra(EXTRA_MARKER)?.let(::File)
        val records = intent?.getIntExtra(EXTRA_RECORDS, 0) ?: 0
        if (mode == null || directory == null || marker == null) {
            Log.e(TAG, "malformed crash request; refusing")
            return START_NOT_STICKY
        }
        directory.mkdirs()
        try {
            perform(mode, directory, records)
            marker.writeText("${mode.name}:$records:ok")
        } catch (failure: Throwable) {
            marker.writeText("${mode.name}:$records:failed:${failure.message}")
        }
        // No `stopSelf`, no `onDestroy`, no flush. START_NOT_STICKY is set so
        // the framework does not helpfully restart what we deliberately killed.
        Runtime.getRuntime().halt(9)
        return START_NOT_STICKY
    }

    private fun perform(mode: Mode, directory: File, records: Int) {
        when (mode) {
            Mode.COMMIT_THEN_DIE -> {
                val medium = SegmentedJournalMedium(directory)
                for (i in 0 until records) medium.append(i.toLong() + 1L, payload(i))
            }

            Mode.TEAR_FRAME_THEN_DIE -> {
                SegmentedJournalMedium(directory).use { medium ->
                    for (i in 0 until records) medium.append(i.toLong() + 1L, payload(i))
                }
                // A valid header and half a body, with no trailer: what a write
                // that reached the device and stopped looks like.
                val body = payload(records)
                FileChannel.open(
                    File(directory, PersistenceBackendContract.JOURNAL_FILE).toPath(),
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND,
                ).use { channel ->
                    val buffer = ByteBuffer.allocate(
                        PersistenceBackendContract.FRAME_HEADER_BYTES + body.size / 2,
                    )
                    buffer.putInt(PersistenceBackendContract.RECORD_MAGIC)
                    buffer.putInt(body.size)
                    buffer.putLong(records.toLong() + 1L)
                    buffer.put(body, 0, body.size / 2)
                    buffer.flip()
                    while (buffer.hasRemaining()) channel.write(buffer)
                    channel.force(true)
                }
            }

            Mode.STAGE_COMPACTION_THEN_DIE -> {
                SegmentedJournalMedium(directory).use { medium ->
                    for (i in 0 until records) medium.append(i.toLong() + 1L, payload(i))
                }
                // A complete staging file that was never renamed. It must still
                // be ignored: only the rename makes it authoritative.
                File(directory, PersistenceBackendContract.JOURNAL_COMPACTION_FILE)
                    .writeBytes(ByteArray(1024) { 0x7F })
            }

            Mode.STAGE_CHECKPOINT_THEN_DIE -> {
                File(directory, PersistenceBackendContract.CHECKPOINT_STAGING_FILE)
                    .writeBytes(ByteArray(512) { 0x5A })
            }

            Mode.BEGIN_REWRAP_THEN_DIE -> {
                // Writes real encrypted history under a Keystore-derived key,
                // then dies between the two durable steps of a rotation. The
                // surviving state therefore carries both the current wrap and a
                // pending one, and the records it protects were written by a
                // process that no longer exists.
                val container = AndroidKeystoreDeviceKeyContainer.openOrCreate(REWRAP_ALIAS)
                val store = LocalKeyStore(directory, container, ORGANISM)
                val state = if (store.exists) {
                    store.load()
                } else {
                    store.create(ByteArray(ChaCha20Poly1305.KEY_SIZE) { (it * 7 + 3).toByte() })
                }
                SegmentedJournalMedium(directory).use { medium ->
                    val records = EncryptedRecordStore(medium, store.keyContainer(state), ORGANISM)
                    for (i in 1..RECORDS_BEFORE_REWRAP) {
                        records.append(i.toLong(), 1L, 700, 1, payload(i))
                    }
                }
                store.beginRewrap(state, state.keyEpoch + 1)
            }
        }
    }

    public enum class Mode {
        COMMIT_THEN_DIE,
        TEAR_FRAME_THEN_DIE,
        STAGE_COMPACTION_THEN_DIE,
        STAGE_CHECKPOINT_THEN_DIE,
        BEGIN_REWRAP_THEN_DIE,
    }

    public companion object {
        private const val TAG = "DLL17-crash"

        public const val EXTRA_MODE: String = "mode"
        public const val EXTRA_DIRECTORY: String = "directory"
        public const val EXTRA_MARKER: String = "marker"
        public const val EXTRA_RECORDS: String = "records"

        /** The rewrap fixture's own container alias, so it cannot disturb others. */
        public const val REWRAP_ALIAS: String = "dll17.test.rewrap.v1"
        public const val ORGANISM: Long = 0x0D11_0012L

        /** Encrypted records the rewrap fixture writes before it dies. */
        public const val RECORDS_BEFORE_REWRAP: Int = 5

        /** The payload the child writes, so the caller can assert on it exactly. */
        public fun payload(index: Int): ByteArray =
            ByteArray(96) { ((index * 31 + it * 7) % 251).toByte() }

        public fun request(
            context: Context,
            mode: Mode,
            directory: File,
            marker: File,
            records: Int,
        ) {
            marker.delete()
            val intent = Intent(context, DeviceCrashService::class.java)
                .putExtra(EXTRA_MODE, mode.name)
                .putExtra(EXTRA_DIRECTORY, directory.absolutePath)
                .putExtra(EXTRA_MARKER, marker.absolutePath)
                .putExtra(EXTRA_RECORDS, records)
            context.startService(intent)
        }
    }
}
