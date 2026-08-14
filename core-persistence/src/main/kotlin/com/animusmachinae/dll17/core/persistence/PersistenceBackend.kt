package com.animusmachinae.dll17.core.persistence

import com.animusmachinae.dll17.core.continuity.ContinuityContract
import com.animusmachinae.dll17.core.continuity.DurableMedium
import com.animusmachinae.dll17.core.continuity.InterruptedWrite
import com.animusmachinae.dll17.core.continuity.StorageFault
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

/**
 * `PersistenceBackendContractV1`, frozen identifiers and layout.
 *
 * The backend was selected from measurement, not convention — see
 * `docs/architecture/PersistenceBackendContractV1.md` and
 * `qualification/evidence/R012/backend_benchmark.txt`. On the reference machine
 * a general-purpose SQL engine cost five times the p99 commit latency of a plain
 * append-only log and 4.4 times the storage, for a workload that never issues a
 * query. The log is what this module implements.
 */
public object PersistenceBackendContract {

    public const val CONTRACT_ID: String = "PersistenceBackendContractV1"
    public const val CONTRACT_VERSION: Int = 1

    /** The selected backend. */
    public const val BACKEND_ID: String = "SEGMENTED_APPEND_LOG_V1"

    /** File names. Fixed, because recovery has to find them without a catalogue. */
    public const val JOURNAL_FILE: String = "journal.dll17"
    public const val JOURNAL_COMPACTION_FILE: String = "journal.compacting"
    public const val CHECKPOINT_FILE: String = "checkpoint.dll17"
    public const val CHECKPOINT_STAGING_FILE: String = "checkpoint.staging"
    public const val KEYSTATE_FILE: String = "keystate.dll17"
    public const val KEYSTATE_STAGING_FILE: String = "keystate.staging"
    public const val QUARANTINE_MARKER_FILE: String = "quarantine.dll17"

    /**
     * Every framed record carries this magic and its own length twice: once
     * before the body and once after. The trailing length is what makes a torn
     * tail detectable without trusting the file length, which is metadata the
     * filesystem may have updated before the body reached the device.
     */
    public const val RECORD_MAGIC: Int = 0x444C_3137
    public const val FRAME_HEADER_BYTES: Int = 16
    public const val FRAME_TRAILER_BYTES: Int = 8

    /** Maximum single record. Larger is a programming error, not a storage event. */
    public const val MAX_RECORD_BYTES: Int = 1 shl 20

    /** Inherited from the frozen R002 contract rather than restated. */
    public const val JOURNAL_BYTE_BUDGET: Long = ContinuityContract.JOURNAL_BYTE_BUDGET

    /**
     * Reserve held back from the budget so that a compaction can always run.
     *
     * Without it a full journal is unrecoverable: compaction needs to write the
     * surviving records before it can delete anything, and a medium with no room
     * to do that has no way out of `PRESSURE` except losing history.
     */
    public const val EMERGENCY_RESERVE_BYTES: Long = 512L * 1024L

    /** `fsync` policy. `true` means metadata as well as data. */
    public const val FORCE_METADATA_ON_COMMIT: Boolean = true
}

/** Why the backend refused to open or continue. */
public enum class BackendFault(public val ordinal32: Int) {
    NONE(1),
    UNREADABLE_DIRECTORY(2),
    TORN_TAIL_BEYOND_TAIL(3),
    CORRUPT_FRAME(4),
    SEQUENCE_REGRESSION(5),
    CAPACITY_EXHAUSTED(6),
    WRITE_REFUSED(7),
}

/**
 * The production durable medium: a single-writer append-only log on the
 * filesystem, with copy-forward compaction and an `fsync` on every commit.
 *
 * Why not a database. Canonical persistence here never issues a query. It
 * appends framed opaque ciphertext, replays the whole surviving history at
 * startup, and drops everything below a checkpoint. A SQL engine's index,
 * planner, page cache, rollback journal and its own crash-recovery machinery are
 * all cost with no corresponding use — and its crash semantics would sit
 * underneath ours, so a durability claim would depend on two engines agreeing
 * rather than one `fsync` returning.
 *
 * What acknowledgement means here, exactly: [append] returns normally only after
 * the frame's bytes and the file's metadata have been forced to the device. That
 * is the boundary `ContinuityDurabilityContractV1` requires, and it is the only
 * thing this class promises.
 */
public class SegmentedJournalMedium(
    private val directory: File,
    override val capacityBytes: Long = PersistenceBackendContract.JOURNAL_BYTE_BUDGET,
) : DurableMedium, AutoCloseable {

    private val journalPath = File(directory, PersistenceBackendContract.JOURNAL_FILE)
    private var channel: FileChannel
    private var used: Long = 0L
    private var highestSequence: Long = Long.MIN_VALUE

    /**
     * Set when the medium has refused an operation it cannot recover from.
     * Deliberately sticky: a faulted medium stays faulted until [selfTest]
     * passes, because the alternative is retrying into a failing device.
     */
    public var fault: BackendFault = BackendFault.NONE
        private set

    init {
        if (!directory.exists() && !directory.mkdirs()) {
            throw StorageFault("cannot create storage directory ${directory.absolutePath}")
        }
        if (!directory.isDirectory) {
            throw StorageFault("${directory.absolutePath} is not a directory")
        }
        recoverInterruptedCompaction()
        channel = open(journalPath)
        used = scanAndTruncateTornTail()
    }

    // ------------------------------------------------------------- framing

    private fun open(file: File): FileChannel = try {
        FileChannel.open(
            file.toPath(),
            StandardOpenOption.CREATE,
            StandardOpenOption.READ,
            StandardOpenOption.WRITE,
        )
    } catch (io: IOException) {
        fault = BackendFault.UNREADABLE_DIRECTORY
        throw StorageFault("cannot open ${file.absolutePath}: ${io.message}")
    }

    private fun frame(sequence: Long, record: ByteArray): ByteBuffer {
        val size = PersistenceBackendContract.FRAME_HEADER_BYTES + record.size +
            PersistenceBackendContract.FRAME_TRAILER_BYTES
        return ByteBuffer.allocate(size).apply {
            putInt(PersistenceBackendContract.RECORD_MAGIC)
            putInt(record.size)
            putLong(sequence)
            put(record)
            putInt(record.size)
            putInt(PersistenceBackendContract.RECORD_MAGIC)
            flip()
        }
    }

    /**
     * Reads the log, stopping at the first frame that is not wholly present.
     *
     * A partial frame at the end of the file is a write that started and never
     * finished; nothing acknowledged it, so it is not history. Anything
     * structurally broken *before* the end is different — that is history the
     * medium has lost, and it is a fault rather than a truncation.
     */
    private fun scanAndTruncateTornTail(): Long {
        val size = channel.size()
        if (size == 0L) return 0L
        val all = readWholeFile()

        var lastGoodEnd = 0L
        var previousSequence = Long.MIN_VALUE
        while (true) {
            when (val frame = nextFrame(all, previousSequence)) {
                is FrameScan.Complete -> {
                    previousSequence = frame.sequence
                    highestSequence = frame.sequence
                    lastGoodEnd = all.position().toLong()
                }
                // A torn tail is a write that started and never finished. It can
                // only be at the end of the file, so it is truncated away and the
                // next append starts from a clean boundary.
                FrameScan.TornTail -> break
            }
        }

        if (lastGoodEnd < size) {
            channel.truncate(lastGoodEnd)
            channel.force(true)
        }
        channel.position(lastGoodEnd)
        return lastGoodEnd
    }

    /** The outcome of trying to read one frame. Corruption throws rather than returning. */
    private sealed interface FrameScan {
        class Complete(val sequence: Long, val body: ByteArray) : FrameScan
        object TornTail : FrameScan
    }

    /**
     * Reads one frame, or classifies why it could not.
     *
     * The distinction that matters: **a wrong magic or an impossible length is
     * corruption, always, wherever it appears.** A partial write cannot produce
     * them, because the magic is the first thing written and a length is either
     * wholly present or wholly absent. Only running out of bytes is a torn tail.
     *
     * Treating a broken first frame as "an empty journal" would be the worst
     * possible failure here: it would present a corrupt installation as a device
     * with no organism on it.
     */
    private fun nextFrame(buffer: ByteBuffer, previousSequence: Long): FrameScan {
        if (buffer.remaining() < PersistenceBackendContract.FRAME_HEADER_BYTES) {
            return FrameScan.TornTail
        }
        val start = buffer.position()
        val magic = buffer.int
        val length = buffer.int
        val sequence = buffer.long
        if (magic != PersistenceBackendContract.RECORD_MAGIC) {
            fault = BackendFault.CORRUPT_FRAME
            throw StorageFault(
                "frame at byte $start has magic ${"%08x".format(magic)}; a partial write " +
                    "cannot produce that, so this is corruption rather than a torn tail",
            )
        }
        if (length < 0 || length > PersistenceBackendContract.MAX_RECORD_BYTES) {
            fault = BackendFault.CORRUPT_FRAME
            throw StorageFault("frame at byte $start declares an impossible length of $length")
        }
        if (buffer.remaining() < length + PersistenceBackendContract.FRAME_TRAILER_BYTES) {
            buffer.position(start)
            return FrameScan.TornTail
        }
        val body = ByteArray(length)
        buffer.get(body)
        val trailingLength = buffer.int
        val trailingMagic = buffer.int
        if (trailingLength != length || trailingMagic != PersistenceBackendContract.RECORD_MAGIC) {
            fault = BackendFault.CORRUPT_FRAME
            throw StorageFault("frame at byte $start has a broken trailer")
        }
        if (previousSequence != Long.MIN_VALUE && sequence <= previousSequence) {
            fault = BackendFault.SEQUENCE_REGRESSION
            throw StorageFault("sequence $sequence follows $previousSequence in the log")
        }
        return FrameScan.Complete(sequence, body)
    }

    private fun readWholeFile(): ByteBuffer {
        val size = channel.size()
        val all = ByteBuffer.allocate(size.toInt())
        channel.position(0)
        while (all.hasRemaining() && channel.read(all) > 0) Unit
        all.flip()
        return all
    }

    // ------------------------------------------------------------- medium

    override val usedBytes: Long get() = used

    override fun append(sequence: Long, record: ByteArray) {
        if (fault != BackendFault.NONE) throw StorageFault("medium is faulted: $fault")
        if (record.size > PersistenceBackendContract.MAX_RECORD_BYTES) {
            throw IllegalArgumentException("record of ${record.size} bytes exceeds the frame limit")
        }
        if (sequence <= highestSequence) {
            throw IllegalArgumentException(
                "sequence $sequence does not advance past $highestSequence",
            )
        }
        val buffer = frame(sequence, record)
        val frameBytes = buffer.remaining().toLong()
        if (used + frameBytes > capacityBytes) {
            // Deliberately *not* a sticky fault. A full journal must still be
            // readable and compactable, or there is no way out of it except
            // losing history — which is the failure the emergency reserve and
            // this refusal exist to prevent.
            throw StorageFault(
                "capacity exhausted: ${used + frameBytes} bytes required, $capacityBytes available",
            )
        }

        val startPosition = channel.position()
        try {
            while (buffer.hasRemaining()) channel.write(buffer)
            channel.force(PersistenceBackendContract.FORCE_METADATA_ON_COMMIT)
        } catch (io: IOException) {
            // Roll the file back to the last acknowledged boundary. An unfinished
            // write must leave nothing that a later reader could mistake for a
            // record, and the trailing length would already have caught it, but
            // leaving the bytes there would also consume budget forever.
            try {
                channel.truncate(startPosition)
                channel.force(true)
                channel.position(startPosition)
            } catch (ignored: IOException) {
                fault = BackendFault.WRITE_REFUSED
            }
            throw InterruptedWrite("write interrupted at sequence $sequence: ${io.message}")
        }
        used += frameBytes
        highestSequence = sequence
    }

    /**
     * Appends without forcing. Class O only.
     *
     * `ContinuityDurabilityContractV1` allows ordinary canonical progress to be
     * batched, and forbids it for witnessed transitions. That is a durability
     * *class* decision, not a storage optimisation: the tail of a Class O batch
     * is recoverable if it is lost, and a Class W transition is not. Nothing here
     * decides which class a record is; the caller does, and [append] is the only
     * path that acknowledges.
     */
    public fun appendWithoutForce(sequence: Long, record: ByteArray) {
        if (fault != BackendFault.NONE) throw StorageFault("medium is faulted: $fault")
        if (sequence <= highestSequence) {
            throw IllegalArgumentException("sequence $sequence does not advance past $highestSequence")
        }
        val buffer = frame(sequence, record)
        val frameBytes = buffer.remaining().toLong()
        if (used + frameBytes > capacityBytes) {
            throw StorageFault("capacity exhausted at sequence $sequence")
        }
        while (buffer.hasRemaining()) channel.write(buffer)
        used += frameBytes
        highestSequence = sequence
    }

    /** Forces everything appended so far. Closes a Class O batch. */
    public fun forceNow() {
        channel.force(PersistenceBackendContract.FORCE_METADATA_ON_COMMIT)
    }

    override fun records(): List<Pair<Long, ByteArray>> {
        if (fault != BackendFault.NONE) throw StorageFault("medium is faulted: $fault")
        val size = channel.size()
        if (size == 0L) return emptyList()
        val all = readWholeFile()
        val out = ArrayList<Pair<Long, ByteArray>>()
        var previousSequence = Long.MIN_VALUE
        loop@ while (true) {
            when (val frame = nextFrame(all, previousSequence)) {
                is FrameScan.Complete -> {
                    previousSequence = frame.sequence
                    out += frame.sequence to frame.body
                }
                // The constructor already truncated any torn tail, so reaching
                // one here means the file ended cleanly.
                FrameScan.TornTail -> break@loop
            }
        }
        channel.position(size)
        return out
    }

    /**
     * Copy-forward compaction.
     *
     * Survivors are written to a staging file, forced, and then atomically
     * renamed over the journal. At no point does a single file contain a
     * partially rewritten history, so an interruption at any instant leaves
     * either the old complete journal or the new complete one.
     */
    override fun prune(throughSequence: Long) {
        if (fault != BackendFault.NONE) throw StorageFault("medium is faulted: $fault")
        val survivors = records().filter { it.first > throughSequence }
        val staging = File(directory, PersistenceBackendContract.JOURNAL_COMPACTION_FILE)
        var rewritten = 0L
        FileChannel.open(
            staging.toPath(),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE,
        ).use { out ->
            for ((sequence, record) in survivors) {
                val buffer = frame(sequence, record)
                rewritten += buffer.remaining().toLong()
                while (buffer.hasRemaining()) out.write(buffer)
            }
            out.force(true)
        }
        channel.close()
        Files.move(
            staging.toPath(),
            journalPath.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
        forceDirectory()
        channel = open(journalPath)
        used = rewritten
        highestSequence = survivors.lastOrNull()?.first ?: Long.MIN_VALUE
        channel.position(rewritten)
    }

    /**
     * If a compaction was interrupted, a staging file survives. It is discarded
     * rather than adopted: the staging file is only known-complete once the
     * rename has happened, and a rename either happened or did not.
     */
    private fun recoverInterruptedCompaction() {
        val staging = File(directory, PersistenceBackendContract.JOURNAL_COMPACTION_FILE)
        if (staging.exists()) staging.delete()
    }

    /** Forces the directory entry so a rename survives power loss. */
    private fun forceDirectory() {
        try {
            FileChannel.open(directory.toPath(), StandardOpenOption.READ).use { it.force(true) }
        } catch (unsupported: IOException) {
            // Some filesystems refuse to open a directory as a channel. The
            // rename is still atomic; only the durability of the *directory*
            // entry is then the filesystem's business rather than ours.
        }
    }

    /**
     * Durability self-test, required before leaving `STORAGE_FAULT`.
     *
     * Writes a probe frame, forces it, reads it back and removes it. It exercises
     * the same path a commit does, because a self-test that only checks free
     * space would pass on a device that has stopped accepting writes.
     */
    override fun selfTest(): Boolean {
        val probe = File(directory, "selftest.dll17")
        return try {
            val payload = ByteArray(64) { (it * 7 % 251).toByte() }
            FileChannel.open(
                probe.toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            ).use { out ->
                val buffer = ByteBuffer.wrap(payload)
                while (buffer.hasRemaining()) out.write(buffer)
                out.force(true)
            }
            val readBack = probe.readBytes()
            probe.delete()
            val ok = readBack.contentEquals(payload)
            if (ok) fault = BackendFault.NONE
            ok
        } catch (io: IOException) {
            probe.delete()
            false
        }
    }

    /** Bytes still available before the emergency reserve is touched. */
    public val headroomBytes: Long
        get() = capacityBytes - PersistenceBackendContract.EMERGENCY_RESERVE_BYTES - used

    override fun close() {
        channel.close()
    }
}
