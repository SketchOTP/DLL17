package com.animusmachinae.dll17.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopRunnerTest {
    @Test
    fun reportsEveryCoreModule() {
        assertEquals(
            listOf("core-math", "core-crypto", "core-state"),
            DesktopRunner.moduleInventory().map { it.id },
        )
    }

    @Test
    fun everyReportedModuleIsPureJvmAndCarriesCanonicalLogic() {
        DesktopRunner.moduleInventory().forEach { module ->
            assertEquals("pure-kotlin-jvm", module.runtimeBoundary, module.id)
            assertEquals(true, module.canonicalLogicImplemented, module.id)
        }
    }

    @Test
    fun emitsAStableEvidenceDigestAcrossRepeatedRuns() {
        // The reference runner is a determinism matrix target. If it is not
        // stable against itself, comparing it to a device is meaningless.
        val first = DesktopRunner.report()
        val second = DesktopRunner.report()
        assertEquals(first, second, "the desktop reference runner must be byte-stable")
        assertTrue(first.contains("R001_EVIDENCE_DIGEST="))
    }

    @Test
    fun headlessReportRunsWithoutTheAndroidFramework() {
        val absent = try {
            Class.forName("android.app.Activity")
            false
        } catch (expected: ClassNotFoundException) {
            true
        }
        assertTrue(absent, "desktop-runner must execute with no Android framework present")
        assertTrue(DesktopRunner.report().startsWith("Digital Living Lifeform - desktop runner"))
    }
}
