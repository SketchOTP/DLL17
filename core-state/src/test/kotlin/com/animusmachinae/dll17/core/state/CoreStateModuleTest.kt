package com.animusmachinae.dll17.core.state

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoreStateModuleTest {
    @Test
    fun declaresItsR000Identity() {
        assertEquals("core-state", CoreStateModule.ID)
        assertEquals("pure-kotlin-jvm", CoreStateModule.RUNTIME_BOUNDARY)
    }

    @Test
    fun declaresCanonicalLogicSinceR001() {
        assertTrue(
            CoreStateModule.CANONICAL_LOGIC_IMPLEMENTED,
            "R001 implemented this module's canonical logic under a frozen DeterminismContractV1.",
        )
    }

    @Test
    fun hasNoAndroidFrameworkOnTheClasspath() {
        for (type in ANDROID_FRAMEWORK_TYPES) {
            val absent = try {
                Class.forName(type)
                false
            } catch (expected: ClassNotFoundException) {
                true
            }
            assertTrue(absent, "$type must not be resolvable from core-state")
        }
    }

    private companion object {
        val ANDROID_FRAMEWORK_TYPES = listOf(
            "android.app.Activity",
            "android.content.Context",
            "android.os.Build",
            "android.os.SystemClock",
            "androidx.compose.runtime.Composer",
        )
    }
}
