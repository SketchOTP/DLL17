package com.animusmachinae.dll17.core.persistence

import com.animusmachinae.dll17.core.continuity.Checkpoint
import com.animusmachinae.dll17.core.continuity.EncryptedRecordStore
import com.animusmachinae.dll17.core.crypto.CanonicalEnvelope
import com.animusmachinae.dll17.core.crypto.CanonicalHash
import com.animusmachinae.dll17.core.crypto.CanonicalWriter
import com.animusmachinae.dll17.core.crypto.ChaCha20Poly1305
import com.animusmachinae.dll17.core.recovery.RecoveryRoot
import java.io.File
import java.nio.file.Files
import java.util.Locale

/**
 * Measured performance of the selected backend.
 *
 * Deliberately separate from the qualification kernel and deliberately outside
 * its digest: a latency is a property of the machine that measured it, and
 * putting one in a reproducible digest would make every build fail on a busier
 * afternoon.
 *
 * Three repetitions, and every repetition reported. A single best run is the
 * easiest number to publish and the least informative one, and the p99 is the
 * figure that decides whether a commit blocks a frame.
 */
public object R012PerformanceHarness {

    private const val COMMITS = 1_500
    private const val WARMUP = 150
    private const val RECORD_BYTES = 256
    private const val REPETITIONS = 3

    private class Samples(val label: String, val micros: LongArray) {
        fun percentile(p: Double): Long {
            val sorted = micros.clone()
            sorted.sort()
            val index = StrictMath.ceil(p / 100.0 * sorted.size).toInt().coerceIn(1, sorted.size)
            return sorted[index - 1]
        }

        val mean: Double get() = micros.sum().toDouble() / micros.size
    }

    private fun payload(index: Int) = ByteArray(RECORD_BYTES) { ((index * 17 + it * 3) % 251).toByte() }

    private fun dataKey() = ByteArray(ChaCha20Poly1305.KEY_SIZE) { (it * 7 + 3).toByte() }

    private fun container() = InProcessDeviceKeyContainer(0xA1L, ByteArray(32) { (it * 13 + 5).toByte() })

    private fun checkpointOf(through: Long): Checkpoint {
        val stateBytes = CanonicalEnvelope.wrap(
            900, 1, CanonicalWriter(64).putI64(through).putRawBytes(ByteArray(4096)).toByteArray(),
        )
        return Checkpoint(1L, through, stateBytes, CanonicalHash.ofEnvelope(stateBytes), 1, 1, 1)
    }

    public fun run(root: File): String = buildString {
        val store = Files.getFileStore(root.toPath())
        append("R012_PERFORMANCE=measured\n")
        append("backend=").append(PersistenceBackendContract.BACKEND_ID).append('\n')
        append("os=").append(System.getProperty("os.name"))
        append(" arch=").append(System.getProperty("os.arch")).append('\n')
        append("jvm=").append(System.getProperty("java.vm.name"))
        append(' ').append(System.getProperty("java.version")).append('\n')
        append("filesystem=").append(store.type()).append(" device=").append(store.name()).append('\n')
        append("commits=").append(COMMITS).append(" recordBytes=").append(RECORD_BYTES)
        append(" warmup=").append(WARMUP).append(" repetitions=").append(REPETITIONS).append('\n')
        append("\nNOTE: these are reference-machine numbers on a desktop NVMe device. They are\n")
        append("      not Android device figures and are not production thresholds. No\n")
        append("      threshold is derived from them; see the contract for why.\n\n")

        append("== CLASS W — witnessed commit, one fsync per record\n")
        val classW = (1..REPETITIONS).map { rep -> classWRun(File(root, "classW-$rep"), rep) }
        append(renderTable(classW))

        append("\n== CLASS O — ordinary progress, batched with one fsync per batch\n")
        append("  batch  ").append("perRecord_us_mean".padStart(20))
        append("batch_us_mean".padStart(16)).append('\n')
        for (batch in listOf(1, 4, 16, 64)) {
            val perRecord = classORun(File(root, "classO-$batch"), batch)
            append("  ").append(batch.toString().padEnd(7))
            append(fmt(perRecord.first).padStart(20))
            append(fmt(perRecord.second).padStart(16)).append('\n')
        }
        append("  Batching is a durability *class* decision, not a storage trick: Class O may\n")
        append("  be batched because losing the tail of it is recoverable, and Class W may not.\n")

        append("\n== ENCRYPTED-RECORD OVERHEAD\n")
        val plainBytes = measureFootprint(File(root, "footprint-plain"), encrypted = false)
        val sealedBytes = measureFootprint(File(root, "footprint-sealed"), encrypted = true)
        append("  rawPayloadBytes=").append(COMMITS * RECORD_BYTES).append('\n')
        append("  unencryptedJournalBytes=").append(plainBytes).append('\n')
        append("  encryptedJournalBytes=").append(sealedBytes).append('\n')
        append("  overheadPerRecordBytes=")
        append((sealedBytes - plainBytes) / COMMITS).append('\n')
        append("  overheadRatio=")
        append(fmt(sealedBytes.toDouble() / plainBytes.toDouble())).append('\n')

        append("\n== SNAPSHOT, COMPACTION AND REPLAY\n")
        val directory = File(root, "lifecycle").apply { mkdirs() }
        val keys = LocalKeyStore(directory, container(), 1L)
        val state = keys.create(dataKey())
        SegmentedJournalMedium(directory).use { medium ->
            val records = EncryptedRecordStore(medium, keys.keyContainer(state), 1L)
            for (i in 1..COMMITS) records.append(i.toLong(), 1L, 700, 1, payload(i))

            val snapshotStore = SnapshotStore(directory)
            val snapshotMicros = LongArray(20) {
                val start = System.nanoTime()
                snapshotStore.write(checkpointOf(it.toLong() + 1L))
                (System.nanoTime() - start) / 1_000L
            }
            append("  snapshotWrite_us mean=").append(fmt(snapshotMicros.average()))
            append(" p50=").append(Samples("s", snapshotMicros).percentile(50.0))
            append(" max=").append(snapshotMicros.max()).append('\n')

            val replayStart = System.nanoTime()
            val replayed = records.readAll().size
            append("  replay_ms=").append(fmt((System.nanoTime() - replayStart) / 1_000_000.0))
            append(" records=").append(replayed).append('\n')

            val compactStart = System.nanoTime()
            records.prune(COMMITS / 2L)
            append("  compaction_ms=").append(fmt((System.nanoTime() - compactStart) / 1_000_000.0))
            append(" survivors=").append(records.readAll().size).append('\n')
            append("  journalBytesAfterCompaction=").append(medium.usedBytes).append('\n')
        }

        append("\n== COLD RECOVERY PACKAGE\n")
        val recoveryRoot = RecoveryRoot(ByteArray(32) { (it * 5 + 1).toByte() })
        val packageDir = File(root, "package").apply { mkdirs() }
        val packageKeys = LocalKeyStore(packageDir, container(), 1L)
        val packageState = packageKeys.create(dataKey())
        SegmentedJournalMedium(packageDir).use { medium ->
            val records = EncryptedRecordStore(medium, packageKeys.keyContainer(packageState), 1L)
            for (i in 1..COMMITS) records.append(i.toLong(), 1L, 700, 1, payload(i))
            SnapshotStore(packageDir).write(checkpointOf(COMMITS / 2L))
            val lineage = CanonicalHash.ofEnvelope(CanonicalWriter(8).putI64(1L).toByteArray())
            val createStart = System.nanoTime()
            val sealed = ColdRecoveryActivation.createPackage(
                packageDir, recoveryRoot, 1L, 1, 1L, lineage, records, 1_000L,
            )
            val createMillis = (System.nanoTime() - createStart) / 1_000_000.0
            val openStart = System.nanoTime()
            val opened = com.animusmachinae.dll17.core.recovery.ColdRecoveryPackage
                .open(recoveryRoot, sealed)
            val openMillis = (System.nanoTime() - openStart) / 1_000_000.0
            append("  tailRecords=").append(opened.journalTail.size).append('\n')
            append("  packageSizeBytes=").append(sealed.sizeBytes).append('\n')
            append("  createPackage_ms=").append(fmt(createMillis)).append('\n')
            append("  openPackage_ms=").append(fmt(openMillis)).append('\n')
        }
    }

    private fun renderTable(runs: List<Samples>): String = buildString {
        append("  run".padEnd(8)).append("mean_us".padStart(10)).append("p50_us".padStart(10))
        append("p90_us".padStart(10)).append("p99_us".padStart(10)).append("max_us".padStart(10))
        append('\n')
        for (run in runs) {
            append("  ").append(run.label.padEnd(6))
            append(fmt(run.mean).padStart(10))
            append(run.percentile(50.0).toString().padStart(10))
            append(run.percentile(90.0).toString().padStart(10))
            append(run.percentile(99.0).toString().padStart(10))
            append(run.micros.max().toString().padStart(10))
            append('\n')
        }
    }

    private fun classWRun(directory: File, repetition: Int): Samples {
        directory.mkdirs()
        val keys = LocalKeyStore(directory, container(), 1L)
        val state = keys.create(dataKey())
        return SegmentedJournalMedium(directory).use { medium ->
            val records = EncryptedRecordStore(medium, keys.keyContainer(state), 1L)
            repeat(WARMUP) { records.append(it.toLong() + 1L, 1L, 700, 1, payload(it)) }
            val samples = LongArray(COMMITS)
            for (i in 0 until COMMITS) {
                val start = System.nanoTime()
                records.append((WARMUP + i).toLong() + 1L, 1L, 700, 1, payload(i))
                samples[i] = (System.nanoTime() - start) / 1_000L
            }
            Samples(repetition.toString(), samples)
        }
    }

    /** Returns (per-record micros, whole-batch micros) for a given batch size. */
    private fun classORun(directory: File, batch: Int): Pair<Double, Double> {
        directory.mkdirs()
        val medium = SegmentedJournalMedium(directory)
        var sequence = 1L
        repeat(WARMUP) { medium.appendWithoutForce(sequence++, payload(it)) }
        medium.forceNow()
        val batches = COMMITS / batch
        val start = System.nanoTime()
        repeat(batches) {
            repeat(batch) { i -> medium.appendWithoutForce(sequence++, payload(i)) }
            medium.forceNow()
        }
        val totalMicros = (System.nanoTime() - start) / 1_000.0
        medium.close()
        return (totalMicros / (batches * batch)) to (totalMicros / batches)
    }

    private fun measureFootprint(directory: File, encrypted: Boolean): Long {
        directory.mkdirs()
        return SegmentedJournalMedium(directory).use { medium ->
            if (encrypted) {
                val keys = LocalKeyStore(directory, container(), 1L)
                val state = keys.create(dataKey())
                val records = EncryptedRecordStore(medium, keys.keyContainer(state), 1L)
                for (i in 1..COMMITS) records.append(i.toLong(), 1L, 700, 1, payload(i))
            } else {
                for (i in 1..COMMITS) medium.append(i.toLong(), payload(i))
            }
            medium.usedBytes
        }
    }

    private fun fmt(value: Double): String = String.format(Locale.ROOT, "%.3f", value)

}
