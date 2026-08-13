package com.animusmachinae.dll17.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HostShellStateTest {
    @Test
    fun shellReportsTheFrozenProductDisplayName() {
        assertEquals("Digital Living Lifeform", HostShellState.r000().displayName)
    }

    @Test
    fun shellReportsEveryCoreModuleItHosts() {
        assertEquals(
            listOf("core-math", "core-crypto", "core-state"),
            HostShellState.r000().modules,
        )
    }

    @Test
    fun shellDeclaresThatNoOrganismExistsYet() {
        assertTrue(HostShellState.r000().organismState.startsWith("No organism exists yet"))
    }
}
