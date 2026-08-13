package com.animusmachinae.dll17.android

import android.os.Build
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.animusmachinae.dll17.core.state.PlatformPanicWitness
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Empirical measurement of the `PlatformPanicWitness` write cost.
 *
 * The architect's 2026-08-07 correction is explicit that the panic witness has
 * no universal 2.0 ms requirement and that its attempt deadline "must be
 * established empirically on the supported-device matrix". This test is that
 * measurement. It deliberately asserts almost nothing about the timing: it
 * records what the device did, because the point is to replace an assumed
 * constant with evidence, not to replace it with a different assumed constant.
 *
 * What it does assert is the structural contract, which is not a timing
 * question: the write is bounded, non-retrying, and happens at most once per
 * instance.
 *
 * The measurement is noncanonical. Nothing here feeds a reducer or a state hash.
 */
@RunWith(AndroidJUnit4::class)
class PanicWitnessBenchmarkTest {

    @Test
    fun measuresTheWriteCostOnThisTarget() {
        val samples = LongArray(SAMPLE_COUNT)

        for (index in 0 until SAMPLE_COUNT) {
            // A fresh instance per sample: the witness writes at most once, and
            // reusing one would measure the early-return path instead.
            val witness = PlatformPanicWitness(attemptDeadlineNanos = 0L)
            val start = System.nanoTime()
            witness.write(
                reasonOrdinal = 1,
                lastDurableSequence = index.toLong(),
                monotonicNanos = start,
            )
            samples[index] = System.nanoTime() - start
        }

        samples.sort()
        val minimum = samples.first()
        val median = samples[samples.size / 2]
        val p95 = samples[(samples.size * 95) / 100]
        val p99 = samples[(samples.size * 99) / 100]
        val maximum = samples.last()

        Log.i(TAG, "panic witness benchmark")
        Log.i(TAG, "  target device     = ${Build.DEVICE}")
        Log.i(TAG, "  target abi        = ${Build.SUPPORTED_ABIS.firstOrNull()}")
        // SOC_MANUFACTURER and SOC_MODEL arrived at API 31; minSdk is 29, so
        // this is guarded rather than assumed.
        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            "${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}"
        } else {
            "unreported below API 31"
        }
        Log.i(TAG, "  soc               = $soc")
        Log.i(TAG, "  samples           = $SAMPLE_COUNT")
        Log.i(TAG, "  record size bytes = ${PlatformPanicWitness.RECORD_SIZE}")
        Log.i(TAG, "  min ns            = $minimum")
        Log.i(TAG, "  median ns         = $median")
        Log.i(TAG, "  p95 ns            = $p95")
        Log.i(TAG, "  p99 ns            = $p99")
        Log.i(TAG, "  max ns            = $maximum")
        Log.i(TAG, "PANIC_WITNESS_MEASURED_P99_NS=$p99")
        Log.i(TAG, "PANIC_WITNESS_MEASURED_MAX_NS=$maximum")

        // The only timing assertion is a sanity bound, several orders of
        // magnitude above anything a 24-byte in-memory write should cost. It
        // exists to catch a pathological regression, not to assert a budget.
        val recordSize = PlatformPanicWitness.RECORD_SIZE
        assertTrue(
            "panic witness write took $p99 ns at p99, implausible for a " +
                "$recordSize-byte preallocated write",
            p99 < 100_000_000L,
        )
    }

    @Test
    fun theWriteIsBoundedAndNonRetrying() {
        val witness = PlatformPanicWitness(attemptDeadlineNanos = 1_000_000L)
        assertTrue("first attempt must be accepted", witness.write(1, 1L, 1L))
        assertTrue("a second attempt must be refused, not retried", !witness.write(2, 2L, 2L))
        assertTrue(witness.hasWritten)
        // Fixed size, preallocated: the record never grows with the organism.
        assertTrue(witness.rawBytes().size == PlatformPanicWitness.RECORD_SIZE)
    }

    private companion object {
        const val TAG = "DLL17-R001"
        const val SAMPLE_COUNT = 2_000
    }
}
