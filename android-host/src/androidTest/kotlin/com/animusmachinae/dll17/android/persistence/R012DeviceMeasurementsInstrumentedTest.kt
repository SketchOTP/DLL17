package com.animusmachinae.dll17.android.persistence

import android.os.Build
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.animusmachinae.dll17.core.persistence.R012PerformanceHarness
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measured persistence behaviour on this Android target.
 *
 * The harness is the same one D011 ran on the desktop, unchanged, so the two
 * sets of figures are directly comparable and any difference is the platform
 * rather than the method. It reports distributions over repeated runs rather
 * than a best case, because a storage stack's interesting behaviour is in its
 * tail and a mean hides exactly the stall a witnessed commit would suffer.
 *
 * **No production threshold is derived here, and none may be derived from an
 * emulator.** These are measurements. Turning one into a limit requires a
 * canonical requirement that authorises the limit and a derivation rule, and
 * `PersistenceBackendContractV1` currently authorises neither.
 */
@RunWith(AndroidJUnit4::class)
class R012DeviceMeasurementsInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun measuredPersistenceBehaviourIsRecordedForThisTarget() {
        val root = File(context.filesDir, "r012-device-performance").apply {
            deleteRecursively()
            mkdirs()
        }
        val filesystem = R012DeviceQualificationKernel.filesystemOf(context.filesDir)
        assertTrue(
            "app-private storage is on $filesystem; a durability measurement there means nothing",
            filesystem != "tmpfs" && filesystem != "ramfs",
        )

        val header = buildString {
            append("R012 DEVICE PERFORMANCE\n")
            append("=".repeat(72)).append('\n')
            append("model=${Build.MODEL} device=${Build.DEVICE} manufacturer=${Build.MANUFACTURER}\n")
            append("soc=${if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL else "unreported"} ")
            append("board=${Build.BOARD} hardware=${Build.HARDWARE}\n")
            append("abis=${Build.SUPPORTED_ABIS.joinToString(",")}\n")
            append("android=${Build.VERSION.RELEASE} api=${Build.VERSION.SDK_INT}\n")
            append("fingerprint=${Build.FINGERPRINT}\n")
            append("emulated=${R012DeviceQualificationKernel.isEmulator()}\n")
            append("storage=app-private filesDir, filesystem=$filesystem\n")
            append(
                "NOTE: measurements only. No production threshold is derived from these figures.\n",
            )
        }
        val measured = R012PerformanceHarness.run(root)
        val rendered = header + "\n" + measured
        for (line in rendered.lines()) Log.i(TAG, line)

        val directory = context.getExternalFilesDir(null) ?: context.filesDir
        File(directory, EVIDENCE_FILE).writeText(rendered)
        assertTrue("the harness produced no measurements", measured.isNotBlank())
    }

    private companion object {
        const val TAG = "DLL17-R012-perf"
        const val EVIDENCE_FILE = "r012_device_performance.txt"
    }
}
