package com.animusmachinae.dll17.bench

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.sql.Connection
import java.sql.DriverManager

/**
 * D011 persistence backend evaluation.
 *
 * The canonical plan (R012.8) permits benchmarking candidate backends in
 * isolation and forbids choosing one implicitly inside production code. This is
 * the isolation: nothing here is imported by a production module, and the
 * selected backend links none of it.
 *
 * The workload is the real one, not a generic storage benchmark. R002 already
 * fixed what canonical persistence has to do:
 *
 * * a single writer, no concurrent readers competing for the same records;
 * * append-only records that are never updated in place and never queried by
 *   anything except sequence order;
 * * a durability acknowledgement that must mean the bytes survive process death,
 *   because `ContinuityDurabilityContractV1` says a visible material mutation may
 *   not survive as truth unless its durable boundary was acknowledged;
 * * whole-history replay at startup rather than selective reads;
 * * periodic pruning of records below a checkpoint.
 *
 * So the question is not "which database is best" but "what is the cheapest
 * honest fsync-per-commit append, and does a general-purpose engine earn its
 * cost on this shape of work".
 */
public object PersistenceBackendBenchmark {

    private const val COMMITS = 2_000
    private const val RECORD_BYTES = 512
    private const val WARMUP = 200

    /** One candidate under evaluation. */
    private interface Candidate {
        val id: String
        val description: String
        fun open(dir: File)
        fun append(sequence: Long, record: ByteArray)
        fun readAll(): Int
        fun prune(throughSequence: Long)
        fun footprintBytes(): Long
        fun close()
    }

    // ------------------------------------------------------------- candidates

    /**
     * C1. Segmented append-only log, one `force(true)` per commit.
     *
     * `force(true)` rather than `force(false)`: metadata matters here because a
     * growing file's length is metadata, and an acknowledged append whose length
     * update was lost is an acknowledged record that vanishes.
     */
    private class SegmentedLogForce : Candidate {
        override val id = "C1_APPEND_LOG_FORCE_METADATA"
        override val description = "append-only log, FileChannel.force(true) per commit"
        private lateinit var dir: File
        private lateinit var channel: FileChannel

        override fun open(dir: File) {
            this.dir = dir
            channel = FileChannel.open(
                File(dir, "journal.log").toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.READ,
            )
        }

        override fun append(sequence: Long, record: ByteArray) {
            val buffer = ByteBuffer.allocate(12 + record.size)
            buffer.putLong(sequence).putInt(record.size).put(record).flip()
            while (buffer.hasRemaining()) channel.write(buffer)
            channel.force(true)
        }

        override fun readAll(): Int {
            channel.position(0)
            var count = 0
            val head = ByteBuffer.allocate(12)
            while (true) {
                head.clear()
                if (channel.read(head) < 12) break
                head.flip()
                head.long
                val size = head.int
                val body = ByteBuffer.allocate(size)
                if (channel.read(body) < size) break
                count++
            }
            return count
        }

        override fun prune(throughSequence: Long) {
            // Copy-forward compaction: read surviving records, write a new file,
            // fsync it, atomically replace. The only crash-safe way to shrink an
            // append-only file.
            val survivors = ArrayList<Pair<Long, ByteArray>>()
            channel.position(0)
            val head = ByteBuffer.allocate(12)
            while (true) {
                head.clear()
                if (channel.read(head) < 12) break
                head.flip()
                val sequence = head.long
                val size = head.int
                val body = ByteBuffer.allocate(size)
                if (channel.read(body) < size) break
                if (sequence > throughSequence) survivors += sequence to body.array()
            }
            val temp = File(dir, "journal.compact")
            FileChannel.open(
                temp.toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            ).use { out ->
                for ((sequence, record) in survivors) {
                    val buffer = ByteBuffer.allocate(12 + record.size)
                    buffer.putLong(sequence).putInt(record.size).put(record).flip()
                    while (buffer.hasRemaining()) out.write(buffer)
                }
                out.force(true)
            }
            channel.close()
            Files.move(
                temp.toPath(),
                File(dir, "journal.log").toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
            channel = FileChannel.open(
                File(dir, "journal.log").toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.READ,
            )
            channel.position(channel.size())
        }

        override fun footprintBytes(): Long = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        override fun close() = channel.close()
    }

    /** C2. Same log, `force(false)`: data only, metadata left to the filesystem. */
    private class SegmentedLogForceData : Candidate {
        override val id = "C2_APPEND_LOG_FORCE_DATA_ONLY"
        override val description = "append-only log, FileChannel.force(false) per commit"
        private lateinit var dir: File
        private lateinit var channel: FileChannel
        private val delegate = SegmentedLogForce()

        override fun open(dir: File) {
            this.dir = dir
            channel = FileChannel.open(
                File(dir, "journal.log").toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.READ,
            )
        }

        override fun append(sequence: Long, record: ByteArray) {
            val buffer = ByteBuffer.allocate(12 + record.size)
            buffer.putLong(sequence).putInt(record.size).put(record).flip()
            while (buffer.hasRemaining()) channel.write(buffer)
            channel.force(false)
        }

        override fun readAll(): Int {
            channel.position(0)
            var count = 0
            val head = ByteBuffer.allocate(12)
            while (true) {
                head.clear()
                if (channel.read(head) < 12) break
                head.flip(); head.long
                val size = head.int
                val body = ByteBuffer.allocate(size)
                if (channel.read(body) < size) break
                count++
            }
            return count
        }

        override fun prune(throughSequence: Long) {
            delegate.also { }
            // Same copy-forward compaction; measured separately only for append.
        }

        override fun footprintBytes(): Long = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        override fun close() = channel.close()
    }

    /** C3. `RandomAccessFile("rwd")`: the JVM's own synchronous-write mode. */
    private class RandomAccessSynchronous : Candidate {
        override val id = "C3_RANDOM_ACCESS_RWD"
        override val description = "RandomAccessFile in rwd mode, one write per commit"
        private lateinit var dir: File
        private lateinit var file: RandomAccessFile

        override fun open(dir: File) {
            this.dir = dir
            file = RandomAccessFile(File(dir, "journal.log"), "rwd")
        }

        override fun append(sequence: Long, record: ByteArray) {
            val buffer = ByteBuffer.allocate(12 + record.size)
            buffer.putLong(sequence).putInt(record.size).put(record)
            file.write(buffer.array())
        }

        override fun readAll(): Int {
            file.seek(0)
            var count = 0
            while (file.filePointer < file.length()) {
                file.readLong()
                val size = file.readInt()
                file.skipBytes(size)
                count++
            }
            return count
        }

        override fun prune(throughSequence: Long) = Unit
        override fun footprintBytes(): Long = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        override fun close() = file.close()
    }

    /**
     * C4. Whole-state rewrite with atomic rename, one fsync per commit.
     *
     * The snapshot-only design: no journal at all, the entire canonical state is
     * rewritten every commit. Included because it is the simplest possible
     * durable design and it is worth knowing exactly how badly it scales.
     */
    private class WholeFileRewrite : Candidate {
        override val id = "C4_WHOLE_FILE_REWRITE"
        override val description = "rewrite whole state + fsync + atomic rename per commit"
        private lateinit var dir: File
        private val accumulated = java.io.ByteArrayOutputStream()

        override fun open(dir: File) {
            this.dir = dir
        }

        override fun append(sequence: Long, record: ByteArray) {
            val buffer = ByteBuffer.allocate(12 + record.size)
            buffer.putLong(sequence).putInt(record.size).put(record)
            accumulated.write(buffer.array())
            val temp = File(dir, "state.tmp")
            FileChannel.open(
                temp.toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            ).use { out ->
                val all = ByteBuffer.wrap(accumulated.toByteArray())
                while (all.hasRemaining()) out.write(all)
                out.force(true)
            }
            Files.move(
                temp.toPath(),
                File(dir, "state.bin").toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        }

        override fun readAll(): Int {
            val bytes = File(dir, "state.bin").readBytes()
            val buffer = ByteBuffer.wrap(bytes)
            var count = 0
            while (buffer.remaining() >= 12) {
                buffer.long
                val size = buffer.int
                if (buffer.remaining() < size) break
                buffer.position(buffer.position() + size)
                count++
            }
            return count
        }

        override fun prune(throughSequence: Long) = Unit
        override fun footprintBytes(): Long = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        override fun close() = Unit
    }

    /** C5/C6. Direct SQLite, the conventional Android answer, in two durability modes. */
    private class Sqlite(
        private val journalMode: String,
        override val id: String,
    ) : Candidate {
        override val description = "direct SQLite, journal_mode=$journalMode, synchronous=FULL"
        private lateinit var dir: File
        private lateinit var connection: Connection

        override fun open(dir: File) {
            this.dir = dir
            connection = DriverManager.getConnection("jdbc:sqlite:${File(dir, "canonical.db").absolutePath}")
            connection.createStatement().use { s ->
                s.execute("PRAGMA journal_mode=$journalMode")
                s.execute("PRAGMA synchronous=FULL")
                s.execute("CREATE TABLE IF NOT EXISTS journal (seq INTEGER PRIMARY KEY, record BLOB NOT NULL)")
            }
            connection.autoCommit = true
        }

        override fun append(sequence: Long, record: ByteArray) {
            connection.prepareStatement("INSERT INTO journal(seq, record) VALUES(?, ?)").use { s ->
                s.setLong(1, sequence)
                s.setBytes(2, record)
                s.executeUpdate()
            }
        }

        override fun readAll(): Int {
            connection.createStatement().use { s ->
                s.executeQuery("SELECT seq, record FROM journal ORDER BY seq").use { rs ->
                    var count = 0
                    while (rs.next()) {
                        rs.getBytes(2)
                        count++
                    }
                    return count
                }
            }
        }

        override fun prune(throughSequence: Long) {
            connection.prepareStatement("DELETE FROM journal WHERE seq <= ?").use { s ->
                s.setLong(1, throughSequence)
                s.executeUpdate()
            }
            connection.createStatement().use { it.execute("VACUUM") }
        }

        override fun footprintBytes(): Long = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        override fun close() = connection.close()
    }

    // ------------------------------------------------------------- harness

    private class Result(
        val id: String,
        val description: String,
        val appendMicros: LongArray,
        val readAllMillis: Double,
        val pruneMillis: Double,
        val footprintBytes: Long,
        val recordsRead: Int,
    ) {
        fun percentile(p: Double): Long {
            val sorted = appendMicros.clone()
            sorted.sort()
            val index = StrictMath.ceil(p / 100.0 * sorted.size).toInt().coerceIn(1, sorted.size)
            return sorted[index - 1]
        }

        val meanMicros: Double get() = appendMicros.sum().toDouble() / appendMicros.size
    }

    private fun run(candidate: Candidate, root: File): Result {
        val dir = File(root, candidate.id).apply { mkdirs() }
        candidate.open(dir)
        val record = ByteArray(RECORD_BYTES) { (it * 31 % 251).toByte() }

        repeat(WARMUP) { candidate.append(it.toLong(), record) }

        val samples = LongArray(COMMITS)
        for (i in 0 until COMMITS) {
            val start = System.nanoTime()
            candidate.append((WARMUP + i).toLong(), record)
            samples[i] = (System.nanoTime() - start) / 1_000L
        }

        val footprint = candidate.footprintBytes()

        val readStart = System.nanoTime()
        val read = candidate.readAll()
        val readMillis = (System.nanoTime() - readStart) / 1_000_000.0

        val pruneStart = System.nanoTime()
        candidate.prune((WARMUP + COMMITS / 2).toLong())
        val pruneMillis = (System.nanoTime() - pruneStart) / 1_000_000.0

        candidate.close()
        return Result(
            candidate.id, candidate.description, samples, readMillis, pruneMillis, footprint, read,
        )
    }

    @JvmStatic
    public fun main(args: Array<String>) {
        // Deliberately not `createTempDirectory`. On this machine the system
        // temp directory is tmpfs, where fsync is a no-op and every candidate
        // would have looked two microseconds fast. A durability benchmark that
        // never reaches a disk measures nothing, so the root is taken from the
        // caller and the filesystem type is both printed and checked.
        val root = File(args.firstOrNull() ?: "build/persistence-bench").apply {
            deleteRecursively()
            mkdirs()
        }
        val store = Files.getFileStore(root.toPath())
        if (store.type() == "tmpfs" || store.type() == "ramfs") {
            System.err.println(
                "REFUSED: ${root.absolutePath} is on ${store.type()}, where fsync does not " +
                    "reach a device. Pass a path on persistent storage.",
            )
            kotlin.system.exitProcess(2)
        }
        val candidates = listOf(
            SegmentedLogForce(),
            SegmentedLogForceData(),
            RandomAccessSynchronous(),
            WholeFileRewrite(),
            Sqlite("WAL", "C5_SQLITE_WAL_SYNC_FULL"),
            Sqlite("DELETE", "C6_SQLITE_DELETE_SYNC_FULL"),
        )

        println("PERSISTENCE_BACKEND_BENCHMARK=D011")
        println("workload: single writer, append-only, fsync-acknowledged commit, whole-history replay")
        println("commits=$COMMITS recordBytes=$RECORD_BYTES warmup=$WARMUP")
        println("os=${System.getProperty("os.name")} arch=${System.getProperty("os.arch")}")
        println("jvm=${System.getProperty("java.vm.name")} ${System.getProperty("java.version")}")
        println("root=${root.absolutePath}")
        println("filesystem=${store.type()} device=${store.name()} (fsync reaches a real device)")
        println()
        println(
            "candidate".padEnd(32) + "mean_us".padStart(10) + "p50_us".padStart(10) +
                "p90_us".padStart(10) + "p99_us".padStart(10) + "max_us".padStart(10) +
                "replay_ms".padStart(12) + "compact_ms".padStart(12) + "bytes".padStart(12),
        )

        val results = candidates.map { candidate ->
            val result = run(candidate, root)
            println(
                result.id.padEnd(32) +
                    fmt(result.meanMicros).padStart(10) +
                    result.percentile(50.0).toString().padStart(10) +
                    result.percentile(90.0).toString().padStart(10) +
                    result.percentile(99.0).toString().padStart(10) +
                    result.appendMicros.max().toString().padStart(10) +
                    fmt(result.readAllMillis).padStart(12) +
                    fmt(result.pruneMillis).padStart(12) +
                    result.footprintBytes.toString().padStart(12),
            )
            result
        }

        println()
        println("== candidate descriptions")
        for (result in results) println("  ${result.id.padEnd(32)} ${result.description}")
        println()
        println("== notes")
        println("  C2 forces data only; its compaction column is not measured and reads 0.000.")
        println("  C3 and C4 do not implement pruning; their compaction column reads 0.000.")
        println("  Replay reads every surviving record, which is what recovery actually does.")
        println("  Storage footprint is the whole directory, so SQLite's WAL and journal count.")
        root.deleteRecursively()
    }

    private fun fmt(value: Double): String = String.format(java.util.Locale.ROOT, "%.3f", value)
}
