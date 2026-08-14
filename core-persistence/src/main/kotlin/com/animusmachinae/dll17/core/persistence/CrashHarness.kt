package com.animusmachinae.dll17.core.persistence

import com.animusmachinae.dll17.core.continuity.EncryptedRecordStore
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

/**
 * Real process death, in a real child JVM.
 *
 * `Runtime.halt` is used rather than `exit`: it skips shutdown hooks and
 * finalizers, so nothing gets a chance to flush, close a channel, or tidy up a
 * staging file. That is what an Android process kill looks like from the inside.
 *
 * ### What this proves, and what it does not
 *
 * It proves the behaviour of the *file layout* across an abrupt process death:
 * a torn frame is dropped, a staging file is discarded, an atomic rename is
 * all-or-nothing, an interrupted rewrap resumes.
 *
 * It does **not** prove that an acknowledged write survives sudden power loss.
 * Killing a process does not empty the OS page cache, so bytes written without
 * an `fsync` are still readable afterwards. Only a real power cut, or a device
 * that lies about `fsync`, distinguishes those cases, and neither is available
 * to a test suite. The durability claim rests on forcing every commit before
 * acknowledging it — the policy, stated in `PersistenceBackendContractV1` — and
 * this harness proves the recovery logic around it rather than the claim itself.
 */
public object CrashHarness {

    /** What the child should do before it dies. */
    public enum class Mode {
        /** Append and force N records, then halt. All must survive. */
        COMMIT_THEN_DIE,

        /** Write a header and half a body, no trailer, then halt. Torn tail. */
        TEAR_FRAME_THEN_DIE,

        /** Write the compaction staging file, then halt before the rename. */
        STAGE_COMPACTION_THEN_DIE,

        /** Write the checkpoint staging file, then halt before the rename. */
        STAGE_CHECKPOINT_THEN_DIE,

        /**
         * Write V1 key state and records, stage the migrated V2 bytes, then halt
         * *before* the rename. The first durable boundary of the V1 migration.
         */
        MIGRATE_STAGED_THEN_DIE,

        /**
         * The same, but complete the migration and halt immediately after the
         * rename, before anything else can run. The second durable boundary.
         */
        MIGRATE_RENAMED_THEN_DIE,
    }

    /** Fixed inputs for the migration modes, so the parent can reconstruct them. */
    public const val MIGRATION_ORGANISM: Long = 0x0D11_0013L
    public const val MIGRATION_FINGERPRINT: Long = 0xC333_3333L
    public const val MIGRATION_RECORDS: Int = 7

    public fun migrationContainer(): InProcessDeviceKeyContainer =
        InProcessDeviceKeyContainer(MIGRATION_FINGERPRINT, ByteArray(32) { (it * 19 + 11).toByte() })

    public fun migrationDataKey(): ByteArray =
        ByteArray(com.animusmachinae.dll17.core.crypto.ChaCha20Poly1305.KEY_SIZE) {
            (it * 23 + 5).toByte()
        }

    public class ChildResult(public val exitCode: Int, public val output: String)

    /** Runs the child in a separate JVM and waits for it to die. */
    public fun runChild(mode: Mode, directory: File, records: Int): ChildResult {
        val java = File(File(System.getProperty("java.home"), "bin"), "java").absolutePath
        val classpath = System.getProperty("java.class.path")
        val process = ProcessBuilder(
            java,
            "-cp",
            classpath,
            "com.animusmachinae.dll17.core.persistence.CrashChild",
            mode.name,
            directory.absolutePath,
            records.toString(),
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        val code = process.waitFor()
        return ChildResult(code, output)
    }

    /** The payload the child writes, so the parent can assert on it exactly. */
    public fun payload(index: Int): ByteArray =
        ByteArray(96) { ((index * 31 + it * 7) % 251).toByte() }
}

/** The child process entry point. Never called in-process. */
public object CrashChild {

    @JvmStatic
    public fun main(args: Array<String>) {
        val mode = CrashHarness.Mode.valueOf(args[0])
        val directory = File(args[1])
        val records = args[2].toInt()
        directory.mkdirs()

        when (mode) {
            CrashHarness.Mode.COMMIT_THEN_DIE -> {
                val medium = SegmentedJournalMedium(directory)
                for (i in 0 until records) {
                    medium.append(i.toLong() + 1L, CrashHarness.payload(i))
                }
                println("committed $records")
                // No close, no flush, no shutdown hook.
                Runtime.getRuntime().halt(9)
            }

            CrashHarness.Mode.TEAR_FRAME_THEN_DIE -> {
                val medium = SegmentedJournalMedium(directory)
                for (i in 0 until records) {
                    medium.append(i.toLong() + 1L, CrashHarness.payload(i))
                }
                medium.close()
                // Now append a deliberately incomplete frame straight to the file:
                // a valid header and half a body, with no trailer. This is what a
                // write that reached the device and stopped looks like.
                val file = File(directory, PersistenceBackendContract.JOURNAL_FILE)
                FileChannel.open(file.toPath(), StandardOpenOption.WRITE, StandardOpenOption.APPEND)
                    .use { channel ->
                        val body = CrashHarness.payload(records)
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
                println("torn after $records")
                Runtime.getRuntime().halt(9)
            }

            CrashHarness.Mode.STAGE_COMPACTION_THEN_DIE -> {
                val medium = SegmentedJournalMedium(directory)
                for (i in 0 until records) {
                    medium.append(i.toLong() + 1L, CrashHarness.payload(i))
                }
                medium.close()
                // A compaction that wrote its staging file and died before the
                // rename. The staging file is complete and must still be ignored,
                // because only the rename makes it authoritative.
                File(directory, PersistenceBackendContract.JOURNAL_COMPACTION_FILE)
                    .writeBytes(ByteArray(1024) { 0x7F })
                println("staged compaction after $records")
                Runtime.getRuntime().halt(9)
            }

            CrashHarness.Mode.STAGE_CHECKPOINT_THEN_DIE -> {
                File(directory, PersistenceBackendContract.CHECKPOINT_STAGING_FILE)
                    .writeBytes(ByteArray(512) { 0x5A })
                println("staged checkpoint")
                Runtime.getRuntime().halt(9)
            }

            CrashHarness.Mode.MIGRATE_STAGED_THEN_DIE,
            CrashHarness.Mode.MIGRATE_RENAMED_THEN_DIE,
            -> {
                val keys = LocalKeyStore(
                    directory,
                    CrashHarness.migrationContainer(),
                    CrashHarness.MIGRATION_ORGANISM,
                )
                val created = keys.create(CrashHarness.migrationDataKey())
                SegmentedJournalMedium(directory).use { medium ->
                    val store = EncryptedRecordStore(
                        medium,
                        keys.keyContainer(created),
                        CrashHarness.MIGRATION_ORGANISM,
                    )
                    for (i in 1..CrashHarness.MIGRATION_RECORDS) {
                        store.append(i.toLong(), 1L, 700, 1, CrashHarness.payload(i))
                    }
                }
                // Put the organism back into the pre-V2 world it would be found in
                // on a device that has been running the shipped V1 build.
                keys.writeV1ForMigrationTest(created)

                if (mode == CrashHarness.Mode.MIGRATE_STAGED_THEN_DIE) {
                    // The migrated bytes reach the staging file and the process
                    // dies before the rename makes them authoritative.
                    File(directory, PersistenceBackendContract.KEYSTATE_STAGING_FILE)
                        .writeBytes(created.migrated().canonicalBytes())
                    println("staged migration")
                } else {
                    keys.load()
                    println("renamed migration")
                }
                Runtime.getRuntime().halt(9)
            }
        }
    }
}
