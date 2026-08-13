package com.animusmachinae.dll17.android

import android.os.Build
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.animusmachinae.dll17.core.state.R001QualificationKernel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Android half of the R001 cross-target determinism matrix.
 *
 * It runs the same [R001QualificationKernel] the desktop reference runner runs
 * and asserts the same frozen golden digest. Running it as an instrumented test
 * rather than through the shell UI is deliberate on two counts: determinism is a
 * property of ART executing the canonical core, not of anything Compose does,
 * and keeping the R000 shell untouched leaves R000's closed gate and its
 * evidence undisturbed.
 *
 * The `R001_EVIDENCE_DIGEST=` line is emitted to logcat so the harness can
 * capture the actual value from each target even when the assertion passes —
 * evidence of what was computed, not just that it matched.
 */
@RunWith(AndroidJUnit4::class)
class R001DeterminismInstrumentedTest {

    @Test
    fun theCanonicalCoreReproducesTheFrozenGoldenDigestOnThisTarget() {
        val report = R001QualificationKernel.run()

        Log.i(TAG, "target abi=${Build.SUPPORTED_ABIS.joinToString(",")}")
        Log.i(TAG, "target device=${Build.DEVICE} model=${Build.MODEL} sdk=${Build.VERSION.SDK_INT}")
        Log.i(TAG, "target fingerprint=${Build.FINGERPRINT}")
        for (line in R001QualificationKernel.renderReport(report).lines()) {
            Log.i(TAG, line)
        }

        assertTrue("every fixture must replay to identical bytes", report.allReplaysMatch)
        assertTrue("domain insertion must not perturb existing streams", report.domainIsolationHolds)
        assertEquals(
            "this Android target does not reproduce the frozen R001 evidence digest",
            R001QualificationKernel.GOLDEN_EVIDENCE_DIGEST,
            report.evidenceDigestHex,
        )
    }

    @Test
    fun theKernelIsStableAcrossRepeatedRunsOnDevice() {
        val first = R001QualificationKernel.run().evidenceDigestHex
        val second = R001QualificationKernel.run().evidenceDigestHex
        assertEquals("the kernel must be byte-stable on this target", first, second)
    }

    @Test
    fun everyPerSectionDigestMatchesTheDesktopReference() {
        // Per-section digests exist so that a cross-target failure names the
        // frozen decision that leaked, rather than only reporting that the
        // overall digest differed.
        val report = R001QualificationKernel.run()
        assertEquals(
            "fixed-point arithmetic diverged on this target",
            "1cb3b944aa46e05f322c91d3f724ab4f0094bb59c03e333577b9cb9a9f00d967",
            report.fixedPointDigestHex,
        )
        assertEquals(
            "deterministic randomness diverged on this target",
            "b3ba5f87e4f0fd3a9f932f83ed6752a1e051dcfac2ffaeef041e9bacabc34017",
            report.randomDigestHex,
        )
        assertEquals(
            "lookup table verification diverged on this target",
            "951d66b7863bbf706dcedec396b02032bbf5d34dc2b17f2438bf5f4ef6eb19ab",
            report.lookupDigestHex,
        )
        assertEquals(
            "canonical serialization diverged on this target",
            "552473e9afb48fbe61921bceedf1096aa1213dd98eb714c380bfc8bae79a43f5",
            report.serializationDigestHex,
        )
    }

    private companion object {
        const val TAG = "DLL17-R001"
    }
}
