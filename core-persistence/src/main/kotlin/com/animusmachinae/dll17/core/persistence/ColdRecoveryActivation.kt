package com.animusmachinae.dll17.core.persistence

import com.animusmachinae.dll17.core.continuity.Checkpoint
import com.animusmachinae.dll17.core.continuity.EncryptedRecordStore
import com.animusmachinae.dll17.core.continuity.StorageFault
import com.animusmachinae.dll17.core.crypto.CanonicalHash
import com.animusmachinae.dll17.core.recovery.AuthorityOutcome
import com.animusmachinae.dll17.core.recovery.ColdRecoveryPackage
import com.animusmachinae.dll17.core.recovery.IdentityAuthorityClient
import com.animusmachinae.dll17.core.recovery.IdentityAuthorityProtocol
import com.animusmachinae.dll17.core.recovery.ProviderException
import com.animusmachinae.dll17.core.recovery.RecoveryContents
import com.animusmachinae.dll17.core.recovery.RecoveryCryptographyContract
import com.animusmachinae.dll17.core.recovery.RecoveryFailure
import com.animusmachinae.dll17.core.recovery.RecoveryManifest
import com.animusmachinae.dll17.core.recovery.RecoveryPackageStoreV1
import com.animusmachinae.dll17.core.recovery.RecoveryRoot
import java.io.File

/**
 * The destination-device cold recovery flow.
 *
 * Canonical order (`Digital Living Lifeform`, identity-epoch activation
 * authority), implemented step for step:
 *
 * 1. the destination creates a device-bound key;
 * 2. the user supplies the recovery secret and the encrypted cold package;
 * 3. the destination verifies package integrity;
 * 4. the authority atomically advances the identity epoch and grants a lease;
 * 5. the destination rewraps local keys, durably commits the recovered state
 *    plus `RecoveryGapDeclared` where applicable, and begins under the new epoch;
 * 6. a prior device that later contacts the authority is rejected.
 *
 * The order is the security property. Verifying the package *before* asking the
 * authority means a corrupt or foreign package never consumes an epoch;
 * advancing the epoch *before* writing local state means a crash mid-restore
 * cannot leave two devices believing they hold the current epoch.
 */
public object ColdRecoveryActivation {

    /** What happened, in the vocabulary the product is allowed to display. */
    public enum class Outcome(public val ordinal32: Int) {
        RECOVERED(1),
        PACKAGE_UNAVAILABLE(2),
        PACKAGE_REJECTED(3),
        PACKAGE_STALE(4),
        AUTHORITY_REFUSED(5),
        AUTHORITY_UNAVAILABLE(6),
        LOCAL_WRITE_FAILED(7),
    }

    public class Result(
        public val outcome: Outcome,
        public val activatedEpoch: Int,
        public val recoveredCheckpointSequence: Long,
        public val restoredRecords: Int,
        public val recoveryGapDeclared: Boolean,
        public val knownUnavailableIntervalMillis: Long,
        public val detail: String,
    ) {
        public val succeeded: Boolean get() = outcome == Outcome.RECOVERED
    }

    /**
     * Runs the whole flow.
     *
     * [localEpochFloor] is the identity epoch this installation has already seen.
     * A package below it is stale and is refused: history may go backwards, but
     * identity may not, or a superseded device could reclaim the organism with an
     * old backup.
     */
    public fun recover(
        directory: File,
        root: RecoveryRoot,
        organismId: Long,
        packageSequence: Long,
        store: RecoveryPackageStoreV1,
        authority: IdentityAuthorityClient,
        container: DeviceKeyContainer,
        localEpochFloor: Int,
        lastLocalLogicalTime: Long,
        nowMillis: Long,
        newDataKey: ByteArray,
    ): Result {
        // Step 2: fetch. A provider failure is an ordinary outcome.
        val bytes = try {
            store.get(organismId, packageSequence)
        } catch (provider: ProviderException) {
            return Result(
                Outcome.PACKAGE_UNAVAILABLE, 0, 0L, 0, false, 0L,
                "provider ${store.providerId} could not supply the package: ${provider.outcome}",
            )
        }

        // Step 3: verify before anything irreversible happens.
        val sealed = try {
            ColdRecoveryPackage.parse(bytes)
        } catch (failure: RecoveryFailure) {
            return Result(Outcome.PACKAGE_REJECTED, 0, 0L, 0, false, 0L, failure.message ?: "")
        }
        if (sealed.manifest.organismId != organismId) {
            return Result(
                Outcome.PACKAGE_REJECTED, 0, 0L, 0, false, 0L,
                "package belongs to organism ${sealed.manifest.organismId}",
            )
        }
        if (sealed.manifest.identityEpoch < localEpochFloor) {
            return Result(
                Outcome.PACKAGE_STALE, 0, 0L, 0, false, 0L,
                "${RecoveryCryptographyContract.STALE_PACKAGE_STATE}: package epoch " +
                    "${sealed.manifest.identityEpoch} is below the activated epoch $localEpochFloor",
            )
        }
        val contents = try {
            ColdRecoveryPackage.open(root, sealed)
        } catch (failure: RecoveryFailure) {
            return Result(Outcome.PACKAGE_REJECTED, 0, 0L, 0, false, 0L, failure.message ?: "")
        }

        // Step 4: the authority decides, atomically, who holds the organism now.
        val requestedEpoch = sealed.manifest.identityEpoch + 1
        val challenge = authority.challenge(organismId, nowMillis)
        if (challenge.outcome != AuthorityOutcome.CHALLENGE_ISSUED) {
            return Result(
                Outcome.AUTHORITY_REFUSED, 0, 0L, 0, false, 0L,
                "authority refused a challenge: ${challenge.outcome}",
            )
        }
        val proof = IdentityAuthorityProtocol.activationProof(
            root, organismId, challenge.nonce, requestedEpoch, container.deviceFingerprint,
        )
        val activation = authority.activate(
            organismId, requestedEpoch, challenge.nonce, proof,
            container.deviceFingerprint, nowMillis,
        )
        if (!activation.granted) {
            return Result(
                Outcome.AUTHORITY_REFUSED, activation.currentEpoch, 0L, 0, false, 0L,
                "authority refused activation: ${activation.outcome} — ${activation.detail}",
            )
        }

        // Step 5: local write. Everything above this line is reversible; nothing
        // below it is, which is why the authority already knows the answer.
        return try {
            val restored = restoreLocally(
                directory, contents, container, organismId, newDataKey,
            )
            val gap = sealed.manifest.lastProtectedLogicalTime < lastLocalLogicalTime
            val unavailable = if (gap) {
                lastLocalLogicalTime - sealed.manifest.lastProtectedLogicalTime
            } else {
                0L
            }
            Quarantine.clearAfterRecovery(directory)
            Result(
                outcome = Outcome.RECOVERED,
                activatedEpoch = requestedEpoch,
                recoveredCheckpointSequence = sealed.manifest.checkpointSequence,
                restoredRecords = restored,
                recoveryGapDeclared = gap,
                knownUnavailableIntervalMillis = unavailable,
                detail = "recovered to checkpoint ${sealed.manifest.checkpointSequence} " +
                    "under epoch $requestedEpoch",
            )
        } catch (fault: StorageFault) {
            Result(
                Outcome.LOCAL_WRITE_FAILED, requestedEpoch, 0L, 0, false, 0L,
                "local storage refused the restore: ${fault.message}",
            )
        }
    }

    /**
     * Writes the recovered checkpoint and journal tail into a fresh local store.
     *
     * The destination gets a **new** data key rather than one carried in the
     * package. The package's job is to move canonical history, not to move device
     * key material into a device that did not generate it.
     */
    private fun restoreLocally(
        directory: File,
        contents: RecoveryContents,
        container: DeviceKeyContainer,
        organismId: Long,
        newDataKey: ByteArray,
    ): Int {
        val keyStore = LocalKeyStore(directory, container, organismId)
        keyStore.deleteLocalMaterial()
        File(directory, PersistenceBackendContract.JOURNAL_FILE).delete()
        val keyState = keyStore.create(newDataKey)
        val medium = SegmentedJournalMedium(directory)
        val records = EncryptedRecordStore(medium, keyStore.keyContainer(keyState), organismId)

        // The checkpoint is verified against its own hash before it is trusted.
        val checkpoint = Checkpoint.decode(contents.checkpointBytes)
        if (!checkpoint.verifySelf()) {
            throw StorageFault("recovered checkpoint failed its own hash check")
        }
        SnapshotStore(directory).write(checkpoint)

        var written = 0
        for ((sequence, plaintext) in contents.journalTail) {
            records.append(
                sequence = sequence,
                generationId = checkpoint.generationId,
                schemaId = EncryptedRecordStore.RECORD_SCHEMA_ID,
                schemaVersion = EncryptedRecordStore.RECORD_SCHEMA_VERSION,
                plaintext = plaintext,
            )
            written++
        }
        medium.close()
        return written
    }

    /**
     * Builds a cold package from the current local store.
     *
     * The journal tail is decrypted and re-encrypted under the recovery key
     * rather than copied as local ciphertext, because local ciphertext is bound
     * to a device key the destination will never have.
     */
    public fun createPackage(
        directory: File,
        root: RecoveryRoot,
        organismId: Long,
        identityEpoch: Int,
        packageSequence: Long,
        lineageHash: ByteArray,
        records: EncryptedRecordStore,
        lastProtectedLogicalTime: Long,
    ): ColdRecoveryPackage.Sealed {
        val checkpoint = SnapshotStore(directory).read()
            ?: throw RecoveryFailure(
                "RECOVERY_PACKAGE_UNAVAILABLE",
                "no checkpoint exists yet; there is nothing to protect",
            )
        val tail = records.readAll()
            .filter { it.sequence > checkpoint.throughSequence }
            .map { it.sequence to it.payload }
        val contents = RecoveryContents(checkpoint.canonicalBytes(), tail)
        return ColdRecoveryPackage.create(
            root = root,
            manifestTemplate = { ciphertextBytes, ciphertextHash ->
                RecoveryManifest(
                    organismId = organismId,
                    identityEpoch = identityEpoch,
                    packageSequence = packageSequence,
                    checkpointSequence = checkpoint.throughSequence,
                    lastProtectedLogicalTime = lastProtectedLogicalTime,
                    engineContractVersion = checkpoint.engineContractVersion,
                    eventContractVersion = checkpoint.eventContractVersion,
                    randomContractVersion = checkpoint.randomContractVersion,
                    ciphertextBytes = ciphertextBytes,
                    ciphertextHash = ciphertextHash,
                    lineageHash = lineageHash,
                )
            },
            contents = contents,
            organismId = organismId,
        )
    }
}

/**
 * The checkpoint file.
 *
 * Written through a staging file and an atomic rename, and verified against its
 * own hash on read. A checkpoint is the one thing recovery cannot re-derive, so
 * a half-written one must be impossible rather than merely unlikely.
 */
public class SnapshotStore(private val directory: File) {

    private val path = File(directory, PersistenceBackendContract.CHECKPOINT_FILE)
    private val staging = File(directory, PersistenceBackendContract.CHECKPOINT_STAGING_FILE)

    public fun write(checkpoint: Checkpoint) {
        if (!checkpoint.verifySelf()) {
            throw StorageFault("refusing to write a checkpoint that fails its own hash check")
        }
        staging.writeBytes(checkpoint.canonicalBytes())
        java.io.RandomAccessFile(staging, "rws").use { it.fd.sync() }
        java.nio.file.Files.move(
            staging.toPath(),
            path.toPath(),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING,
            java.nio.file.StandardCopyOption.ATOMIC_MOVE,
        )
    }

    public fun read(): Checkpoint? {
        if (!path.exists()) return null
        val checkpoint = Checkpoint.decode(path.readBytes())
        if (!checkpoint.verifySelf()) {
            throw StorageFault("stored checkpoint failed its own hash check")
        }
        return checkpoint
    }

    public fun hashHex(): String? {
        if (!path.exists()) return null
        return CanonicalHash.hex(CanonicalHash.ofEnvelope(path.readBytes()))
    }
}
