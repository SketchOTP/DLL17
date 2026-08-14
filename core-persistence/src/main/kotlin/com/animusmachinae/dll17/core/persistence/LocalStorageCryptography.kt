package com.animusmachinae.dll17.core.persistence

import com.animusmachinae.dll17.core.continuity.KeyContainer
import com.animusmachinae.dll17.core.continuity.StorageFault
import com.animusmachinae.dll17.core.crypto.CanonicalEnvelope
import com.animusmachinae.dll17.core.crypto.CanonicalHash
import com.animusmachinae.dll17.core.crypto.CanonicalReader
import com.animusmachinae.dll17.core.crypto.CanonicalWriter
import com.animusmachinae.dll17.core.crypto.ChaCha20Poly1305
import com.animusmachinae.dll17.core.crypto.Hkdf
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * `LocalStorageCryptographyContractV1`, frozen values.
 *
 * The structural rule comes from the canonical plan (R012.17) and is not
 * negotiable here: canonical payloads outside the key container are
 * authenticated ciphertext, never a plaintext second source of truth; the
 * daily-use data key is random and wrapped by container material; and only the
 * minimum metadata needed to locate and order records stays outside the
 * ciphertext, authenticated against it.
 */
public object LocalStorageCryptographyContract {

    public const val CONTRACT_ID: String = "LocalStorageCryptographyContractV1"
    public const val CONTRACT_VERSION: Int = 1

    public const val AEAD_ALGORITHM: String = ChaCha20Poly1305.ALGORITHM_ID
    public const val WRAP_ALGORITHM: String = ChaCha20Poly1305.ALGORITHM_ID
    public const val KDF_ALGORITHM: String = Hkdf.ALGORITHM_ID

    /** Domain separation for every key this contract derives. */
    public const val INFO_WRAPPING_KEY: String = "DLL17-LOCAL-WRAP-V1"

    /** Canonical schema of the persisted key state. */
    public const val KEY_STATE_SCHEMA_ID: Int = 231
    public const val KEY_STATE_SCHEMA_VERSION: Int = 1

    /** The first epoch a newly created organism writes under. */
    public const val INITIAL_KEY_EPOCH: Int = 1
}

/** Why local key material could not be used. */
public enum class KeyFault(public val ordinal32: Int) {
    NONE(1),
    CONTAINER_UNAVAILABLE(2),
    WRAPPED_KEY_UNAUTHENTIC(3),
    EPOCH_MISMATCH(4),
    DEVICE_MISMATCH(5),
}

public class KeyStateFault(public val fault: KeyFault, message: String) : RuntimeException(message)

/**
 * The device-bound key container, the Android Keystore analogue.
 *
 * On Android this is backed by a non-exportable Keystore key. Here it is an
 * interface with a JVM implementation, because canonical correctness has to be
 * provable in a test with no Keystore anywhere near it — and because a class
 * that could only run on a device is a class nobody can qualify.
 */
public interface DeviceKeyContainer {
    public val deviceFingerprint: Long

    /** Whether the container can currently unwrap. False on a copied database. */
    public val available: Boolean

    /**
     * The container's root secret. Never leaves the device on Android; here it
     * is returned so the wrapping key can be derived in one place.
     */
    public fun rootSecret(): ByteArray
}

/** A container backed by an in-process secret. Desktop, test and reference use. */
public class InProcessDeviceKeyContainer(
    override val deviceFingerprint: Long,
    private val secret: ByteArray,
) : DeviceKeyContainer {

    /** Simulates a copied database: the state is present, the container is not. */
    public var simulateUnavailable: Boolean = false

    override val available: Boolean get() = !simulateUnavailable

    override fun rootSecret(): ByteArray {
        if (simulateUnavailable) {
            throw KeyStateFault(
                KeyFault.CONTAINER_UNAVAILABLE,
                "device key container refused to release its root secret",
            )
        }
        return secret.copyOf()
    }
}

/**
 * Persisted key state: the wrapped data-encryption key and its epoch.
 *
 * The DEK itself is random and never derived from the device secret, so a
 * rotation of the container material rewraps rather than re-encrypts, and the
 * millions of records already written stay readable.
 */
public class WrappedKeyState(
    public val keyEpoch: Int,
    public val deviceFingerprint: Long,
    public val wrappedKey: ByteArray,
    public val wrapNonce: ByteArray,
    /**
     * Set while a rewrap is in flight. If a process dies mid-rewrap, the next
     * open sees both epochs and can finish or abandon deterministically.
     */
    public val pendingEpoch: Int,
    public val pendingWrappedKey: ByteArray,
    public val pendingWrapNonce: ByteArray,
) {
    public val rewrapInFlight: Boolean get() = pendingEpoch != 0

    public fun canonicalBytes(): ByteArray = CanonicalEnvelope.wrap(
        LocalStorageCryptographyContract.KEY_STATE_SCHEMA_ID,
        LocalStorageCryptographyContract.KEY_STATE_SCHEMA_VERSION,
        CanonicalWriter(160)
            .putI32(keyEpoch)
            .putI64(deviceFingerprint)
            .putBytes(wrappedKey)
            .putBytes(wrapNonce)
            .putI32(pendingEpoch)
            .putBytes(pendingWrappedKey)
            .putBytes(pendingWrapNonce)
            .toByteArray(),
    )

    public companion object {
        public fun decode(bytes: ByteArray): WrappedKeyState {
            val contents = CanonicalEnvelope.unwrap(bytes)
            if (contents.payloadSchemaId != LocalStorageCryptographyContract.KEY_STATE_SCHEMA_ID) {
                throw StorageFault("key state carries schema ${contents.payloadSchemaId}")
            }
            val reader = CanonicalReader(contents.payload)
            val state = WrappedKeyState(
                keyEpoch = reader.readI32(),
                deviceFingerprint = reader.readI64(),
                wrappedKey = reader.readBytes(),
                wrapNonce = reader.readBytes(),
                pendingEpoch = reader.readI32(),
                pendingWrappedKey = reader.readBytes(),
                pendingWrapNonce = reader.readBytes(),
            )
            reader.requireExhausted()
            return state
        }
    }
}

/**
 * The local key lifecycle: wrap, unwrap, rotate, resume an interrupted rewrap,
 * and refuse rather than reset.
 *
 * The one rule that matters more than the others: **a cryptographic failure
 * never creates a new organism.** Every failure path here throws or quarantines.
 * None of them returns a fresh key, because a fresh key would silently orphan
 * every existing record and the next startup would look like a birth.
 */
public class LocalKeyStore(
    private val directory: File,
    private val container: DeviceKeyContainer,
    private val organismId: Long,
) {

    private val statePath = File(directory, PersistenceBackendContract.KEYSTATE_FILE)
    private val stagingPath = File(directory, PersistenceBackendContract.KEYSTATE_STAGING_FILE)

    public val exists: Boolean get() = statePath.exists()

    private fun wrappingKey(epoch: Int): ByteArray = Hkdf.derive(
        inputKeyMaterial = container.rootSecret(),
        salt = CanonicalWriter(16).putI64(organismId).putI32(epoch).toByteArray(),
        info = LocalStorageCryptographyContract.INFO_WRAPPING_KEY,
        length = ChaCha20Poly1305.KEY_SIZE,
    )

    private fun wrapAad(epoch: Int): ByteArray = CanonicalWriter(24)
        .putI64(organismId)
        .putI64(container.deviceFingerprint)
        .putI32(epoch)
        .toByteArray()

    /** Nonce derived from `(organismId, epoch)`. One wrap per epoch, so unique. */
    private fun wrapNonce(epoch: Int): ByteArray =
        CanonicalWriter(12).putI32(epoch).putI64(organismId).toByteArray()

    /**
     * Creates key state for a new organism.
     *
     * The data key comes from the caller, because entropy generation is
     * noncanonical evidence supplied by the host — the same rule
     * `OrganismBirthContractV1` applies to the organism ID and master seed.
     */
    public fun create(dataKey: ByteArray): WrappedKeyState {
        if (dataKey.size != ChaCha20Poly1305.KEY_SIZE) {
            throw IllegalArgumentException("data key must be ${ChaCha20Poly1305.KEY_SIZE} bytes")
        }
        if (statePath.exists()) {
            throw StorageFault("key state already exists; creating would orphan existing records")
        }
        val epoch = LocalStorageCryptographyContract.INITIAL_KEY_EPOCH
        val nonce = wrapNonce(epoch)
        val state = WrappedKeyState(
            keyEpoch = epoch,
            deviceFingerprint = container.deviceFingerprint,
            wrappedKey = ChaCha20Poly1305.seal(wrappingKey(epoch), nonce, wrapAad(epoch), dataKey),
            wrapNonce = nonce,
            pendingEpoch = 0,
            pendingWrappedKey = ByteArray(0),
            pendingWrapNonce = ByteArray(0),
        )
        writeAtomically(state)
        return state
    }

    public fun load(): WrappedKeyState {
        if (!statePath.exists()) throw StorageFault("no key state at ${statePath.absolutePath}")
        return WrappedKeyState.decode(statePath.readBytes())
    }

    /**
     * Unwraps the data key.
     *
     * Fails loudly in every direction: an unavailable container, a foreign
     * device fingerprint, and an unauthentic wrapped key are three different
     * faults and each is reported as itself.
     */
    public fun unwrap(state: WrappedKeyState): ByteArray {
        if (!container.available) {
            throw KeyStateFault(
                KeyFault.CONTAINER_UNAVAILABLE,
                "device key container is unavailable; canonical records stay sealed",
            )
        }
        if (state.deviceFingerprint != container.deviceFingerprint) {
            throw KeyStateFault(
                KeyFault.DEVICE_MISMATCH,
                "key state is bound to device ${state.deviceFingerprint}",
            )
        }
        return try {
            ChaCha20Poly1305.open(
                key = wrappingKey(state.keyEpoch),
                nonce = state.wrapNonce,
                aad = wrapAad(state.keyEpoch),
                sealed = state.wrappedKey,
            )
        } catch (failure: ChaCha20Poly1305.AuthenticationFailure) {
            throw KeyStateFault(
                KeyFault.WRAPPED_KEY_UNAUTHENTIC,
                "wrapped data key failed authentication: ${failure.message}",
            )
        }
    }

    /**
     * Rotates the wrapping epoch. The data key is unchanged, so nothing already
     * written has to be re-encrypted.
     *
     * Two durable steps, in this order:
     *
     * 1. write the state with both the current wrap and a `pending` wrap under
     *    the new epoch, and force it;
     * 2. write the state with the new wrap promoted and the pending fields
     *    cleared, and force it.
     *
     * A process death between them leaves step one's file, which [resumeRewrap]
     * completes. A process death during either write leaves the previous file
     * intact, because both go through a staging file and an atomic rename.
     */
    public fun beginRewrap(state: WrappedKeyState, newEpoch: Int): WrappedKeyState {
        if (newEpoch <= state.keyEpoch) {
            throw IllegalArgumentException("rewrap epoch $newEpoch does not advance")
        }
        val dataKey = unwrap(state)
        val nonce = wrapNonce(newEpoch)
        val staged = WrappedKeyState(
            keyEpoch = state.keyEpoch,
            deviceFingerprint = container.deviceFingerprint,
            wrappedKey = state.wrappedKey,
            wrapNonce = state.wrapNonce,
            pendingEpoch = newEpoch,
            pendingWrappedKey = ChaCha20Poly1305.seal(
                wrappingKey(newEpoch), nonce, wrapAad(newEpoch), dataKey,
            ),
            pendingWrapNonce = nonce,
        )
        writeAtomically(staged)
        return staged
    }

    /** Promotes a pending wrap. Idempotent: safe to call on already-clean state. */
    public fun completeRewrap(state: WrappedKeyState): WrappedKeyState {
        if (!state.rewrapInFlight) return state
        val promoted = WrappedKeyState(
            keyEpoch = state.pendingEpoch,
            deviceFingerprint = state.deviceFingerprint,
            wrappedKey = state.pendingWrappedKey,
            wrapNonce = state.pendingWrapNonce,
            pendingEpoch = 0,
            pendingWrappedKey = ByteArray(0),
            pendingWrapNonce = ByteArray(0),
        )
        writeAtomically(promoted)
        return promoted
    }

    /**
     * Called at every open. Finishes an interrupted rewrap if the pending wrap
     * is usable, and abandons it if it is not.
     *
     * Abandoning is safe precisely because the data key never changed: the old
     * wrap still opens it, so a failed rotation costs an epoch number and
     * nothing else.
     */
    public fun resumeRewrap(state: WrappedKeyState): WrappedKeyState {
        if (!state.rewrapInFlight) return state
        val usable = try {
            ChaCha20Poly1305.open(
                key = wrappingKey(state.pendingEpoch),
                nonce = state.pendingWrapNonce,
                aad = wrapAad(state.pendingEpoch),
                sealed = state.pendingWrappedKey,
            )
            true
        } catch (failure: RuntimeException) {
            false
        }
        return if (usable) {
            completeRewrap(state)
        } else {
            val abandoned = WrappedKeyState(
                keyEpoch = state.keyEpoch,
                deviceFingerprint = state.deviceFingerprint,
                wrappedKey = state.wrappedKey,
                wrapNonce = state.wrapNonce,
                pendingEpoch = 0,
                pendingWrappedKey = ByteArray(0),
                pendingWrapNonce = ByteArray(0),
            )
            writeAtomically(abandoned)
            abandoned
        }
    }

    /**
     * Deletion order, from the canonical plan: the wrapping material goes first,
     * so any residual ciphertext on the device is already unreadable before the
     * files are removed. Removing files first would leave a window where the
     * data is recoverable and the key is not yet gone.
     */
    public fun deleteLocalMaterial() {
        statePath.delete()
        stagingPath.delete()
    }

    private fun writeAtomically(state: WrappedKeyState) {
        val bytes = state.canonicalBytes()
        stagingPath.writeBytes(bytes)
        java.io.RandomAccessFile(stagingPath, "rws").use { it.fd.sync() }
        Files.move(
            stagingPath.toPath(),
            statePath.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
    }

    /** Adapts the wrapped state to the R002 [KeyContainer] the record store expects. */
    public fun keyContainer(state: WrappedKeyState): KeyContainer = object : KeyContainer {
        override val keyEpoch: Int = state.keyEpoch
        override val deviceFingerprint: Long = container.deviceFingerprint
        override fun dataKey(): ByteArray = unwrap(state)
    }
}

/**
 * The quarantine marker.
 *
 * A quarantined installation is refused on every subsequent open, by a file that
 * exists rather than by an in-memory flag, because the point of quarantine is to
 * survive the restart that a copied database would otherwise use to try again.
 */
public object Quarantine {

    public fun mark(directory: File, reason: String, detail: String) {
        val marker = File(directory, PersistenceBackendContract.QUARANTINE_MARKER_FILE)
        val bytes = CanonicalEnvelope.wrap(
            SCHEMA_ID,
            SCHEMA_VERSION,
            // The reason is a canonical identifier; the detail is free text and
            // is written as bytes, because a human-readable explanation is not an
            // identifier and forcing it to be one would either truncate it or
            // reject it for containing a space.
            CanonicalWriter(256)
                .putIdentifier(reason)
                .putBytes(detail.toByteArray(Charsets.UTF_8))
                .toByteArray(),
        )
        marker.writeBytes(bytes)
        java.io.RandomAccessFile(marker, "rws").use { it.fd.sync() }
    }

    public fun isQuarantined(directory: File): Boolean =
        File(directory, PersistenceBackendContract.QUARANTINE_MARKER_FILE).exists()

    public fun reason(directory: File): String? {
        val marker = File(directory, PersistenceBackendContract.QUARANTINE_MARKER_FILE)
        if (!marker.exists()) return null
        val contents = CanonicalEnvelope.unwrap(marker.readBytes())
        return CanonicalReader(contents.payload).readIdentifier()
    }

    /**
     * Clearing a quarantine is an explicit recovery outcome, never an automatic
     * retry. It exists so a successful cold recovery can lift the marker it set.
     */
    public fun clearAfterRecovery(directory: File) {
        File(directory, PersistenceBackendContract.QUARANTINE_MARKER_FILE).delete()
    }

    public const val SCHEMA_ID: Int = 232
    public const val SCHEMA_VERSION: Int = 1

    public const val REASON_KEY_CONTAINER_REFUSED: String = "KEY_CONTAINER_REFUSED"
    public const val REASON_DEVICE_MISMATCH: String = "DEVICE_FINGERPRINT_MISMATCH"
    public const val REASON_SUPERSEDED_EPOCH: String = "SUPERSEDED_IDENTITY_EPOCH"

    /** Convenience for tests and callers that want the hash of a marker. */
    public fun markerHash(directory: File): String {
        val marker = File(directory, PersistenceBackendContract.QUARANTINE_MARKER_FILE)
        return CanonicalHash.hex(CanonicalHash.ofEnvelope(marker.readBytes()))
    }
}
