package com.animusmachinae.dll17.core.persistence

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The architectural claims that are easiest to state and easiest to lose.
 *
 * Both are checked structurally rather than asserted in a document, because a
 * document cannot fail a build.
 */
class ModuleBoundaryTest {

    @Test
    fun `the identity authority service is not on the organism core classpath`() {
        // The canonical plan requires the authority to be separately deployable
        // and requires that ordinary local operation never calls it. A compile-
        // time dependency from a core module would quietly falsify both, so the
        // kernel takes the authority as a parameter instead.
        val present = try {
            Class.forName("com.animusmachinae.dll17.services.identity.IdentityAuthorityService")
            true
        } catch (absent: ClassNotFoundException) {
            false
        }
        assertFalse(
            present,
            "core-persistence can see the authority service; local operation must never depend on it",
        )
    }

    @Test
    fun `no production source in this module imports an Android type`() {
        val sources = File("src/main/kotlin").walkTopDown().filter { it.extension == "kt" }.toList()
        assertTrue(sources.isNotEmpty(), "no sources found; the test is looking in the wrong place")
        val offenders = sources.filter { source ->
            source.readLines().any { line ->
                line.startsWith("import android.") || line.startsWith("import androidx.")
            }
        }
        assertEquals(
            emptyList(),
            offenders.map { it.name },
            "persistence correctness must be provable off-device",
        )
    }

    @Test
    fun `no production source reaches for a platform key store directly`() {
        // `DeviceKeyContainer` is the seam. A module that could touch Keystore
        // directly could hide a platform dependency inside canonical logic, and
        // the Keystore is not canonical authority.
        val sources = File("src/main/kotlin").walkTopDown().filter { it.extension == "kt" }
        val offenders = sources.filter { source ->
            source.readText().contains("java.security.KeyStore") ||
                source.readText().contains("AndroidKeyStore")
        }
        assertEquals(emptyList(), offenders.map { it.name }.toList())
    }
}
