package com.animusmachinae.dll17.android

import android.os.Build
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.animusmachinae.dll17.core.continuity.R002QualificationKernel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Android half of the R002 continuity and durability qualification.
 *
 * Two independent claims run here, and conflating them would weaken both. The
 * first is determinism: ART must compute the same canonical result the desktop
 * JVM computes. The second is honesty: every exploit fixture — reboot loops,
 * clock spoofing, storage exhaustion, thermal suspension, torn writes — must
 * still be defeated when the code is executing on the device it is meant to
 * defend.
 *
 * It runs as an instrumented test rather than through the shell UI for the same
 * reason the R001 test does: continuity is a property of the canonical core, not
 * of Compose, and leaving the R000 shell untouched keeps that gate's evidence
 * undisturbed.
 */
@RunWith(AndroidJUnit4::class)
class R002ContinuityInstrumentedTest {

    @Test
    fun theContinuityCoreReproducesTheFrozenGoldenDigestOnThisTarget() {
        val report = R002QualificationKernel.run()

        Log.i(TAG, "target abi=${Build.SUPPORTED_ABIS.joinToString(",")}")
        Log.i(TAG, "target device=${Build.DEVICE} model=${Build.MODEL} sdk=${Build.VERSION.SDK_INT}")
        Log.i(TAG, "target fingerprint=${Build.FINGERPRINT}")
        for (line in R002QualificationKernel.renderReport(report).lines()) {
            Log.i(TAG, line)
        }

        assertEquals(
            "this Android target does not reproduce the frozen R002 evidence digest",
            R002QualificationKernel.GOLDEN_EVIDENCE_DIGEST,
            report.evidenceDigestHex,
        )
    }

    @Test
    fun everyContinuityDefenceHoldsOnDevice() {
        val report = R002QualificationKernel.run()
        val failures = report.fixtureResults.filterNot { it.defenceHeld }
        assertTrue(
            "continuity defences failed on this target: " +
                failures.joinToString { "${it.id} (${it.area})" },
            failures.isEmpty(),
        )
    }

    @Test
    fun everyPerSectionDigestMatchesTheDesktopReference() {
        // Per-section digests exist so that a cross-target failure names the
        // subsystem that diverged rather than only reporting a different total.
        val report = R002QualificationKernel.run()
        assertEquals(
            "time-trust classification diverged on this target",
            "32c553602ec91f167d6d0eaa418dc1ac49dd291491cee0d969c2b920cb83ddaa",
            report.trustDigestHex,
        )
        assertEquals(
            "reconciliation mode and chunk schedule diverged on this target",
            "4d0237959e543f7c99192549f97989d8a9d7fa418fb3b63560133cd88148379d",
            report.reconciliationDigestHex,
        )
        assertEquals(
            "credit and debt arithmetic diverged on this target",
            "591ec340b36eb158dc1a861891d08227da4628cee407f27ba151bbbf44cc57db",
            report.debtDigestHex,
        )
        assertEquals(
            "durability thresholds or orderings diverged on this target",
            "c73563ca850c05fd629f75a7758bb3ba77e36ba212047edf92f292474bd1023b",
            report.durabilityDigestHex,
        )
        assertEquals(
            "the encrypted-record boundary diverged on this target",
            "6db8d74c470473b369c2009f8b4e36cad76f9f8b052aa030000ac4db8bb79e44",
            report.encryptionDigestHex,
        )
        assertEquals(
            "replay determinism diverged on this target",
            "712c72e954725d4d83415c385debe86350895244e08bac7c6b691f324b2c531c",
            report.replayDigestHex,
        )
    }

    @Test
    fun theKernelIsStableAcrossRepeatedRunsOnDevice() {
        val first = R002QualificationKernel.run().evidenceDigestHex
        val second = R002QualificationKernel.run().evidenceDigestHex
        assertEquals("the kernel must be byte-stable on this target", first, second)
    }

    private companion object {
        const val TAG = "DLL17-R002"
    }
}
