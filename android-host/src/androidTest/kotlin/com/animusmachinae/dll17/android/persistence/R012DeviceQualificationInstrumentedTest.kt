package com.animusmachinae.dll17.android.persistence

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the device qualification kernel and preserves what it produced.
 *
 * The report is written to the instrumentation's external files directory so it
 * can be pulled off the target verbatim. A qualification whose only record is a
 * green test result is a qualification nobody can check afterwards.
 */
@RunWith(AndroidJUnit4::class)
class R012DeviceQualificationInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun everyDeviceFixtureHolds() {
        val report = R012DeviceQualificationKernel.run(context)
        val rendered = report.render()
        for (line in rendered.lines()) Log.i(TAG, line)
        writeEvidence(rendered)

        // Every fixture must hold except those explicitly declared as blocked on
        // architect review of a frozen-contract conflict. An undeclared failure
        // fails the run; a declared one is evidence, and is reported as such.
        val undeclared = report.notHeld.filterNot {
            it.id in R012DeviceQualificationKernel.PENDING_ARCHITECT_REVIEW
        }
        assertTrue(
            "device fixtures did not hold: " + undeclared.joinToString { "${it.id} (${it.readout})" },
            undeclared.isEmpty(),
        )
        for (declared in report.notHeld) {
            Log.w(TAG, "NOT HELD, pending architect review: ${declared.id} — ${declared.readout}")
        }
    }

    @Test
    fun theTargetIsIdentifiedHonestlyInItsOwnEvidence() {
        // D012 requires physical-device evidence for Keystore behaviour, real
        // app-private storage, latency and restart. Whether this target is one
        // is not something the report may leave to the reader to guess.
        val report = R012DeviceQualificationKernel.run(context)
        assertTrue(
            "the report must state whether the target is emulated",
            report.deviceFacts.contains("emulated="),
        )
        Log.i(TAG, "emulated=${R012DeviceQualificationKernel.isEmulator()}")
    }

    private fun writeEvidence(rendered: String) {
        val directory = InstrumentationRegistry.getInstrumentation()
            .targetContext.getExternalFilesDir(null) ?: context.filesDir
        File(directory, EVIDENCE_FILE).writeText(rendered)
        Log.i(TAG, "evidence written to ${File(directory, EVIDENCE_FILE).absolutePath}")
    }

    private companion object {
        const val TAG = "DLL17-R012-device"
        const val EVIDENCE_FILE = "r012_device_qualification.txt"
    }
}
