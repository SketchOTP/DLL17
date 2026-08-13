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
    fun everyReportedModuleIsPureJvmAndCarriesNoCanonicalLogic() {
        DesktopRunner.moduleInventory().forEach { module ->
            assertEquals("pure-kotlin-jvm", module.runtimeBoundary, module.id)
            assertEquals(false, module.canonicalLogicImplemented, module.id)
        }
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
