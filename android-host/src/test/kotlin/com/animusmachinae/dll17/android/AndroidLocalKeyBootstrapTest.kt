package com.animusmachinae.dll17.android

import com.animusmachinae.dll17.android.persistence.AndroidLocalKeyBootstrap
import com.animusmachinae.dll17.android.persistence.AndroidPersistenceLocations
import com.animusmachinae.dll17.core.crypto.ChaCha20Poly1305
import com.animusmachinae.dll17.core.persistence.DeviceKeyContainer
import com.animusmachinae.dll17.core.persistence.InProcessDeviceKeyContainer
import com.animusmachinae.dll17.core.persistence.KeyFault
import com.animusmachinae.dll17.core.persistence.LocalKeyStore
import com.animusmachinae.dll17.core.persistence.Quarantine
import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The startup decision table, qualified without a keystore anywhere near it.
 *
 * This is the JVM half of the Android key bootstrap. The container is injected,
 * so every row of the table — including the one that must never happen — can be
 * exercised in an ordinary unit test that runs in CI on every commit, rather
 * than only on a device somebody has to plug in.
 */
class AndroidLocalKeyBootstrapTest {

    private val root: File = Files.createTempDirectory("dll17-bootstrap").toFile()
    private val organism = 0x0D11_0011L
    private val alias = "test.alias"

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    private fun container(fingerprint: Long = 0xA1L): DeviceKeyContainer =
        InProcessDeviceKeyContainer(fingerprint, ByteArray(32) { (it * 13 + 5).toByte() })

    private fun dir(name: String) = File(root, name).apply { mkdirs() }

    private fun bootstrap(
        directory: File,
        present: Boolean,
        open: (String) -> DeviceKeyContainer? = { container() },
        create: (String) -> DeviceKeyContainer = { container() },
    ) = AndroidLocalKeyBootstrap(
        keyDirectory = directory,
        organismId = organism,
        alias = alias,
        containerPresent = { present },
        openContainer = open,
        createContainer = create,
    )

    private fun seedKeyState(directory: File, container: DeviceKeyContainer) {
        LocalKeyStore(directory, container, organism)
            .create(ByteArray(ChaCha20Poly1305.KEY_SIZE) { (it * 7 + 3).toByte() })
    }

    @Test
    fun `no state and no material permits a birth`() {
        val directory = dir("fresh")
        val resolution = bootstrap(directory, present = false).resolve()
        assertEquals(AndroidLocalKeyBootstrap.Outcome.BIRTH_PERMITTED, resolution.outcome)
        assertTrue(resolution.mayCreateOrganism)
    }

    @Test
    fun `material without state permits a birth reusing the material`() {
        // Nothing on disk that the missing state could have protected. Refusing
        // here would strand an installation whose app data was cleared while the
        // keystore entry survived.
        val directory = dir("material-only")
        val resolution = bootstrap(directory, present = true).resolve()
        assertEquals(AndroidLocalKeyBootstrap.Outcome.BIRTH_PERMITTED, resolution.outcome)
        assertTrue(resolution.detail.contains("reusing"))
    }

    @Test
    fun `state with usable material opens rather than creating`() {
        val directory = dir("openable")
        seedKeyState(directory, container())
        val resolution = bootstrap(directory, present = true).resolve()
        assertEquals(AndroidLocalKeyBootstrap.Outcome.OPENED, resolution.outcome)
        assertFalse(resolution.mayCreateOrganism)
        assertEquals(1, resolution.state?.keyEpoch)
    }

    @Test
    fun `state with destroyed material quarantines and never permits a birth`() {
        // The row that matters. A device whose keystore was lost looks exactly
        // like a device that never had an organism, and reading it that way is
        // how a product replaces someone's creature with a new one.
        val directory = dir("destroyed")
        seedKeyState(directory, container())
        val resolution = bootstrap(directory, present = false).resolve()

        assertEquals(AndroidLocalKeyBootstrap.Outcome.QUARANTINED, resolution.outcome)
        assertEquals(KeyFault.CONTAINER_UNAVAILABLE, resolution.fault)
        assertFalse(resolution.mayCreateOrganism)
        assertTrue(Quarantine.isQuarantined(directory))
    }

    @Test
    fun `state whose material refuses to release its secret quarantines`() {
        val directory = dir("unavailable")
        val live = InProcessDeviceKeyContainer(0xA1L, ByteArray(32) { (it * 13 + 5).toByte() })
        seedKeyState(directory, live)
        live.simulateUnavailable = true
        val resolution = bootstrap(directory, present = true, open = { live }).resolve()

        assertEquals(AndroidLocalKeyBootstrap.Outcome.QUARANTINED, resolution.outcome)
        assertEquals(KeyFault.CONTAINER_UNAVAILABLE, resolution.fault)
    }

    @Test
    fun `state bound to another device quarantines as a mismatch not as a birth`() {
        val directory = dir("foreign")
        seedKeyState(directory, container(0xA1L))
        val resolution = bootstrap(directory, present = true, open = { container(0xB2L) }).resolve()

        assertEquals(AndroidLocalKeyBootstrap.Outcome.QUARANTINED, resolution.outcome)
        assertEquals(KeyFault.DEVICE_MISMATCH, resolution.fault)
        assertFalse(resolution.mayCreateOrganism)
    }

    @Test
    fun `a quarantine already in force is reported and not re-decided`() {
        val directory = dir("already")
        seedKeyState(directory, container())
        Quarantine.mark(directory, "CONTAINER_UNAVAILABLE", "earlier refusal")
        val resolution = bootstrap(directory, present = true).resolve()
        assertEquals(AndroidLocalKeyBootstrap.Outcome.ALREADY_QUARANTINED, resolution.outcome)
        assertFalse(resolution.mayCreateOrganism)
    }

    @Test
    fun `key state is retained through a quarantine`() {
        // Deleting it would destroy the only thing a later cold recovery could
        // verify the recovered organism against.
        val directory = dir("retained")
        seedKeyState(directory, container())
        bootstrap(directory, present = false).resolve()
        assertTrue(LocalKeyStore(directory, container(), organism).exists)
    }

    @Test
    fun `an interrupted rewrap is resolved before anything is unwrapped`() {
        val directory = dir("rewrap")
        val live = container()
        val store = LocalKeyStore(directory, live, organism)
        val state = store.create(ByteArray(ChaCha20Poly1305.KEY_SIZE) { (it * 7 + 3).toByte() })
        store.beginRewrap(state, state.keyEpoch + 1)

        val resolution = bootstrap(directory, present = true, open = { live }).resolve()
        assertEquals(AndroidLocalKeyBootstrap.Outcome.OPENED, resolution.outcome)
        assertEquals(2, resolution.state?.keyEpoch)
        assertFalse(resolution.state!!.rewrapInFlight)
    }

    @Test
    fun `the canonical layout sits under a declared backup exclusion prefix`() {
        val filesDir = dir("files")
        val locations = AndroidPersistenceLocations(filesDir)
        for (path in locations.relativePaths()) {
            assertTrue(
                "layout path $path is not covered by any declared exclusion prefix",
                AndroidPersistenceLocations.REQUIRED_EXCLUSION_PREFIXES.any { path.startsWith(it.trimEnd('/')) },
            )
        }
    }

    @Test
    fun `the layout creates every canonical directory inside app private storage`() {
        val filesDir = dir("files-create")
        val locations = AndroidPersistenceLocations(filesDir).create()
        assertTrue(locations.all.all { it.isDirectory })
        assertTrue(locations.all.all { it.absolutePath.startsWith(filesDir.absolutePath) })
    }
}
