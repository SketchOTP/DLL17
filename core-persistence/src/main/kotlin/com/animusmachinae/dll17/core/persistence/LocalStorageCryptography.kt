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
 * `LocalStorageCryptographyContractV2`, frozen values.
 *
 * The structural rule comes from the canonical plan (R012.17) and is not
 * negotiable here: canonical payloads outside the key container are
 * authenticated ciphertext, never a plaintext second source of truth; the
 * daily-use data key is random and wrapped by container material; and only the
 * minimum metadata needed to locate and order records stays outside the
 * ciphertext, authenticated against it.
 *
 * V2 supersedes V1 on exactly one point: the *wrapping* epoch and the *data
 * key's identity* are separate quantities, and only the second one reaches a
 * record. Every algorithm, every derivation, every byte layout of a record and
 * every failure rule is carried forward unchanged.
 */
public object LocalStorageCryptographyContract {

    public const val CONTRACT_ID: String = "LocalStorageCryptographyContractV2"
    public const val CONTRACT_VERSION: Int = 2
    public const val SUPERSEDES: String = "LocalStorageCryptographyContractV1"

    public const val AEAD_ALGORITHM: String = ChaCha20Poly1305.ALGORITHM_ID
    public const val WRAP_ALGORITHM: String = ChaCha20Poly1305.ALGORITHM_ID
    public const val KDF_ALGORITHM: String = Hkdf.ALGORITHM_ID

    /** Domain separation for every key this contract derives. */
    public const val INFO_WRAPPING_KEY: String = "DLL17-LOCAL-WRAP-V1"

    /**
     * Canonical schema of the persisted key state.
     *
     * Version 2 under `LocalStorageCryptographyContractV2` adds `dataKeyId`.
     * Version 1 remains readable and is migrated in place; see
     * [LocalKeyStore.load].
     */
    public const val KEY_STATE_SCHEMA_ID: Int = 231
    public const val KEY_STATE_SCHEMA_VERSION: Int = 2
    public const val KEY_STATE_SCHEMA_VERSION_V1: Int = 1

    /** The first **wrapping** epoch a newly created organism writes under. */
    public const val INITIAL_KEY_EPOCH: Int = 1

    /**
     * The data-key identity a newly created organism is born with, and the one
     * every V1 organism migrates to.
     *
     * V1 conflated this with [INITIAL_KEY_EPOCH]. They share a value, and that
     * coincidence is why no journal has to be rewritten — but they are different
     * quantities and V2 keeps them apart.
     */
    public const val INITIAL_DATA_KEY_ID: Int = KeyContainer.INITIAL_DATA_KEY_ID
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
 * Persisted key state: the wrapped data-encryption key, the wrapping epoch that
 * protects it, and the identity of the key itself.
 *
 * The DEK is random and never derived from the device secret, so a rotation of
 * the container material rewraps rather than re-encrypts. Under V2 that promise
 * is finally true in the read path as well: [keyEpoch] moves, [dataKeyId] does
 * not, and only [dataKeyId] reaches a record.
 */
public class WrappedKeyState(
    /** The **wrapping** epoch. Advances on every container-material rotation. */
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
    /**
     * The identity of the wrapped data key. Unchanged by a wrapping rotation,
     * which is the entire point of separating it from [keyEpoch].
     */
    public val dataKeyId: Int = LocalStorageCryptographyContract.INITIAL_DATA_KEY_ID,
    /**
     * The schema version this state was read from. `1` means it predates V2 and
     * has not yet been migrated; freshly written state is always
     * [LocalStorageCryptographyContract.KEY_STATE_SCHEMA_VERSION].
     */
    public val schemaVersion: Int = LocalStorageCryptographyContract.KEY_STATE_SCHEMA_VERSION,
) {
    public val rewrapInFlight: Boolean get() = pendingEpoch != 0

    /** True while this state is still in the superseded V1 representation. */
    public val requiresMigration: Boolean
        get() = schemaVersion < LocalStorageCryptographyContract.KEY_STATE_SCHEMA_VERSION

    /**
     * The V2 representation of this state.
     *
     * Deliberately a pure function of the state, with no clock, no randomness and
     * no device read in it: that is what makes the migration idempotent, and
     * migrating twice produces the same bytes as migrating once.
     */
    public fun migrated(): WrappedKeyState = if (!requiresMigration) {
        this
    } else {
        WrappedKeyState(
            keyEpoch = keyEpoch,
            deviceFingerprint = deviceFingerprint,
            wrappedKey = wrappedKey,
            wrapNonce = wrapNonce,
            pendingEpoch = pendingEpoch,
            pendingWrappedKey = pendingWrappedKey,
            pendingWrapNonce = pendingWrapNonce,
            // Every V1 organism has exactly one data key and has never rotated
            // it, because V1 had no way to. So the migrated identity is the
            // initial one, always, for every installation in existence.
            dataKeyId = LocalStorageCryptographyContract.INITIAL_DATA_KEY_ID,
            schemaVersion = LocalStorageCryptographyContract.KEY_STATE_SCHEMA_VERSION,
        )
    }

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
            .putI32(dataKeyId)
            .toByteArray(),
    )

    public companion object {

        /**
         * Decodes either schema version.
         *
         * V1 state is returned as it was written, marked [requiresMigration], and
         * is **not** silently upgraded here: decoding is a read, and a read must
         * not have a durable side effect. [LocalKeyStore.load] performs the
         * migration where a crash boundary can be reasoned about.
         */
        public fun decode(bytes: ByteArray): WrappedKeyState {
            val contents = CanonicalEnvelope.unwrap(bytes)
            if (contents.payloadSchemaId != LocalStorageCryptographyContract.KEY_STATE_SCHEMA_ID) {
                throw StorageFault("key state carries schema ${contents.payloadSchemaId}")
            }
            val version = contents.payloadSchemaVersion
            if (version != LocalStorageCryptographyContract.KEY_STATE_SCHEMA_VERSION &&
                version != LocalStorageCryptographyContract.KEY_STATE_SCHEMA_VERSION_V1
            ) {
                // Newer than this build understands. Refusing is the only safe
                // answer: guessing at a layout would either orphan the organism
                // or, worse, appear to work.
                throw StorageFault("key state carries unsupported schema version $version")
            }
            val reader = CanonicalReader(contents.payload)
            val keyEpoch = reader.readI32()
            val deviceFingerprint = reader.readI64()
            val wrappedKey = reader.readBytes()
            val wrapNonce = reader.readBytes()
            val pendingEpoch = reader.readI32()
            val pendingWrappedKey = reader.readBytes()
            val pendingWrapNonce = reader.readBytes()
            val dataKeyId = if (version == LocalStorageCryptographyContract.KEY_STATE_SCHEMA_VERSION) {
                reader.readI32()
            } else {
                LocalStorageCryptographyContract.INITIAL_DATA_KEY_ID
            }
            reader.requireExhausted()
            return WrappedKeyState(
                keyEpoch = keyEpoch,
                deviceFingerprint = deviceFingerprint,
                wrappedKey = wrappedKey,
                wrapNonce = wrapNonce,
                pendingEpoch = pendingEpoch,
                pendingWrappedKey = pendingWrappedKey,
                pendingWrapNonce = pendingWrapNonce,
                dataKeyId = dataKeyId,
                schemaVersion = version,
            )
        }

        /**
         * Encodes in the superseded V1 layout. Exists so migration can be
         * qualified against genuine V1 bytes rather than against a hand-written
         * approximation of them.
         */
        public fun encodeV1(state: WrappedKeyState): ByteArray = CanonicalEnvelope.wrap(
            LocalStorageCryptographyContract.KEY_STATE_SCHEMA_ID,
            LocalStorageCryptographyContract.KEY_STATE_SCHEMA_VERSION_V1,
            CanonicalWriter(160)
                .putI32(state.keyEpoch)
                .putI64(state.deviceFingerprint)
                .putBytes(state.wrappedKey)
                .putBytes(state.wrapNonce)
                .putI32(state.pendingEpoch)
                .putBytes(state.pendingWrappedKey)
                .putBytes(state.pendingWrapNonce)
                .toByteArray(),
        )
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
            dataKeyId = LocalStorageCryptographyContract.INITIAL_DATA_KEY_ID,
        )
        writeAtomically(state)
        return state
    }

    /**
     * Loads key state, migrating a V1 representation to V2 in the process.
     *
     * The migration is one atomic rename of one small file, and it rewrites **no
     * journal record** — the record layout is unchanged and V1 records are read
     * through their own stored context. That is the whole reason this is a safe
     * migration rather than a re-encryption of the organism's entire history.
     *
     * Crash boundaries, both of which recover to a readable state:
     *
     * - death before the rename leaves the V1 file, and the next open migrates
     *   again to the identical bytes;
     * - death after the rename leaves the V2 file, and the next open sees state
     *   that no longer requires migration and does nothing.
     *
     * There is no third state, because [writeAtomically] stages and renames.
     */
    public fun load(): WrappedKeyState {
        if (!statePath.exists()) throw StorageFault("no key state at ${statePath.absolutePath}")
        val stored = WrappedKeyState.decode(statePath.readBytes())
        if (!stored.requiresMigration) return stored
        val migrated = stored.migrated()
        writeAtomically(migrated)
        return migrated
    }

    /** Reads without migrating. For qualification of the migration itself. */
    public fun peek(): WrappedKeyState {
        if (!statePath.exists()) throw StorageFault("no key state at ${statePath.absolutePath}")
        return WrappedKeyState.decode(statePath.readBytes())
    }

    /** Writes state in the superseded V1 layout. Qualification use only. */
    public fun writeV1ForMigrationTest(state: WrappedKeyState) {
        val bytes = WrappedKeyState.encodeV1(state)
        stagingPath.writeBytes(bytes)
        java.io.RandomAccessFile(stagingPath, "rws").use { it.fd.sync() }
        Files.move(
            stagingPath.toPath(),
            statePath.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
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
            dataKeyId = state.dataKeyId,
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
            // The data key is untouched by a rewrap, so its identity is too.
            // This single line is the correction D013 exists for.
            dataKeyId = state.dataKeyId,
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
                dataKeyId = state.dataKeyId,
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
        override val dataKeyId: Int = state.dataKeyId
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
