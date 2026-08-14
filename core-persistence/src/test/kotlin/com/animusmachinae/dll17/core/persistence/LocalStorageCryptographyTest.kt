package com.animusmachinae.dll17.core.persistence

import com.animusmachinae.dll17.core.continuity.EncryptedRecordStore
import com.animusmachinae.dll17.core.continuity.StorageFault
import com.animusmachinae.dll17.core.crypto.ChaCha20Poly1305
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalStorageCryptographyTest {

    private val root: File = Files.createTempDirectory("dll17-crypto-test").toFile()
    private val organism = 0x0D11L

    @AfterTest
    fun cleanup() {
        root.deleteRecursively()
    }

    private fun dir(name: String) = File(root, name).apply { mkdirs() }

    private fun container(fingerprint: Long = 0xA1L, secretSeed: Int = 13) =
        InProcessDeviceKeyContainer(fingerprint, ByteArray(32) { (it * secretSeed + 5).toByte() })

    private fun dataKey(seed: Int = 7) =
        ByteArray(ChaCha20Poly1305.KEY_SIZE) { (it * seed + 3).toByte() }

    private fun payload(i: Int) = ByteArray(64) { ((i * 11 + it) % 251).toByte() }

    @Test
    fun `the data key round-trips through wrapping`() {
        val d = dir("wrap")
        val keys = LocalKeyStore(d, container(), organism)
        val state = keys.create(dataKey())
        assertTrue(keys.unwrap(state).contentEquals(dataKey()))
        assertEquals(LocalStorageCryptographyContract.INITIAL_KEY_EPOCH, state.keyEpoch)
    }

    @Test
    fun `creating key state twice is refused rather than orphaning existing records`() {
        val d = dir("no-overwrite")
        val keys = LocalKeyStore(d, container(), organism)
        keys.create(dataKey())
        assertFailsWith<StorageFault> { keys.create(dataKey(9)) }
    }

    @Test
    fun `an unavailable container refuses rather than producing a fresh key`() {
        val d = dir("unavailable")
        val device = container()
        val keys = LocalKeyStore(d, device, organism)
        val state = keys.create(dataKey())
        device.simulateUnavailable = true
        val failure = assertFailsWith<KeyStateFault> { keys.unwrap(state) }
        assertEquals(KeyFault.CONTAINER_UNAVAILABLE, failure.fault)
        // The key state must still be on disk. A cryptographic failure that
        // deleted it would look like a birth on the next launch.
        assertTrue(File(d, PersistenceBackendContract.KEYSTATE_FILE).exists())
    }

    @Test
    fun `a copied database on another device is refused as a device mismatch`() {
        val d = dir("copied")
        val state = LocalKeyStore(d, container(0xA1L), organism).create(dataKey())
        val foreign = LocalKeyStore(d, container(0xB2L), organism)
        val failure = assertFailsWith<KeyStateFault> { foreign.unwrap(state) }
        assertEquals(KeyFault.DEVICE_MISMATCH, failure.fault)
    }

    @Test
    fun `a different device secret fails authentication rather than returning noise`() {
        val d = dir("wrong-secret")
        val state = LocalKeyStore(d, container(0xA1L, secretSeed = 13), organism).create(dataKey())
        val other = LocalKeyStore(d, container(0xA1L, secretSeed = 99), organism)
        val failure = assertFailsWith<KeyStateFault> { other.unwrap(state) }
        assertEquals(KeyFault.WRAPPED_KEY_UNAUTHENTIC, failure.fault)
    }

    @Test
    fun `rotation preserves the data key so existing records stay readable`() {
        val d = dir("rotate")
        val keys = LocalKeyStore(d, container(), organism)
        var state = keys.create(dataKey())

        SegmentedJournalMedium(d).use { medium ->
            val store = EncryptedRecordStore(medium, keys.keyContainer(state), organism)
            for (i in 1..10) store.append(i.toLong(), 1L, 700, 1, payload(i))
        }

        state = keys.completeRewrap(keys.beginRewrap(state, 2))
        assertEquals(2, state.keyEpoch)
        assertFalse(state.rewrapInFlight)
        assertTrue(keys.unwrap(state).contentEquals(dataKey()))

        // Records written under epoch 1 carry epoch 1 in their header, so a store
        // opened at epoch 2 must refuse them rather than guess across epochs.
        SegmentedJournalMedium(d).use { medium ->
            assertFailsWith<StorageFault> {
                EncryptedRecordStore(medium, keys.keyContainer(state), organism).readAll()
            }
        }
    }

    @Test
    fun `an interrupted rewrap resumes on the next open`() {
        val d = dir("resume")
        val keys = LocalKeyStore(d, container(), organism)
        val created = keys.create(dataKey())
        keys.beginRewrap(created, 2)

        val reloaded = keys.load()
        assertTrue(reloaded.rewrapInFlight)
        assertEquals(1, reloaded.keyEpoch)

        val resumed = keys.resumeRewrap(reloaded)
        assertEquals(2, resumed.keyEpoch)
        assertFalse(resumed.rewrapInFlight)
        assertTrue(keys.unwrap(resumed).contentEquals(dataKey()))
    }

    @Test
    fun `resuming is idempotent and safe to call on clean state`() {
        val d = dir("idempotent")
        val keys = LocalKeyStore(d, container(), organism)
        val state = keys.create(dataKey())
        val once = keys.resumeRewrap(state)
        val twice = keys.resumeRewrap(once)
        assertEquals(state.keyEpoch, twice.keyEpoch)
        assertFalse(twice.rewrapInFlight)
    }

    @Test
    fun `an unusable pending wrap is abandoned rather than adopted`() {
        val d = dir("abandon")
        val keys = LocalKeyStore(d, container(), organism)
        val created = keys.create(dataKey())
        val staged = keys.beginRewrap(created, 2)
        // Corrupt the pending wrap, as an interrupted write to it would.
        val broken = WrappedKeyState(
            keyEpoch = staged.keyEpoch,
            deviceFingerprint = staged.deviceFingerprint,
            wrappedKey = staged.wrappedKey,
            wrapNonce = staged.wrapNonce,
            pendingEpoch = staged.pendingEpoch,
            pendingWrappedKey = staged.pendingWrappedKey.copyOf().also { it[0] = (it[0] + 1).toByte() },
            pendingWrapNonce = staged.pendingWrapNonce,
        )
        val resumed = keys.resumeRewrap(broken)
        // The rotation is abandoned and costs an epoch number. Nothing is lost,
        // because the data key never changed.
        assertEquals(1, resumed.keyEpoch)
        assertFalse(resumed.rewrapInFlight)
        assertTrue(keys.unwrap(resumed).contentEquals(dataKey()))
    }

    @Test
    fun `the medium holds no canonical plaintext`() {
        val d = dir("at-rest")
        val keys = LocalKeyStore(d, container(), organism)
        val state = keys.create(dataKey())
        SegmentedJournalMedium(d).use { medium ->
            val store = EncryptedRecordStore(medium, keys.keyContainer(state), organism)
            store.append(1L, 1L, 700, 1, payload(1))
            val raw = File(d, PersistenceBackendContract.JOURNAL_FILE).readBytes()
            assertFalse(indexOf(raw, payload(1)) >= 0, "plaintext is visible on the medium")
        }
    }

    @Test
    fun `quarantine survives a restart and is only cleared by recovery`() {
        val d = dir("quarantine")
        assertFalse(Quarantine.isQuarantined(d))
        Quarantine.mark(d, Quarantine.REASON_KEY_CONTAINER_REFUSED, "container unavailable")
        assertTrue(Quarantine.isQuarantined(d))
        assertEquals(Quarantine.REASON_KEY_CONTAINER_REFUSED, Quarantine.reason(d))
        // The marker is a file precisely so a restart cannot clear it.
        assertTrue(Quarantine.isQuarantined(d))
        Quarantine.clearAfterRecovery(d)
        assertFalse(Quarantine.isQuarantined(d))
    }

    @Test
    fun `deleting local material removes the wrapping key state`() {
        val d = dir("delete")
        val keys = LocalKeyStore(d, container(), organism)
        keys.create(dataKey())
        assertTrue(keys.exists)
        keys.deleteLocalMaterial()
        assertFalse(keys.exists)
    }

    private fun indexOf(haystack: ByteArray, needle: ByteArray): Int {
        outer@ for (i in 0..haystack.size - needle.size) {
            for (j in needle.indices) if (haystack[i + j] != needle[j]) continue@outer
            return i
        }
        return -1
    }
}
