package com.animusmachinae.dll17.core.continuity

import com.animusmachinae.dll17.core.state.DurabilityClass

/**
 * Identity binding, backup exclusion and quarantine, contract section 14.
 *
 * The half of R002.12 that can be implemented and qualified without
 * `RecoveryCryptographyContractV1` — which D007 did not authorize this phase to
 * freeze — and without a selected storage provider. What remains is declared
 * blocked below by the canonical status names, not silently omitted.
 */

/** Why a copied organism was refused. */
public enum class QuarantineReason(public val ordinal32: Int) {
    NONE(1),

    /** The device key container exists but cannot unwrap the canonical data key. */
    KEY_CONTAINER_REFUSED(2),

    /** The recorded device fingerprint does not match this device. */
    DEVICE_FINGERPRINT_MISMATCH(3),
}

/** Result of the activation check performed before any canonical work. */
public class ActivationCheck(
    public val admitted: Boolean,
    public val reason: QuarantineReason,
    public val detail: String,
)

/**
 * The device-binding gate.
 *
 * A copied active database does not boot as a second organism. The check runs
 * **before** any canonical event is folded, because a quarantined copy that
 * advanced even one clock would already have forked the identity.
 */
public object DeviceBinding {

    public fun check(state: ContinuityState, keys: KeyContainer): ActivationCheck {
        if (state.identity.deviceFingerprint != keys.deviceFingerprint) {
            return ActivationCheck(
                admitted = false,
                reason = QuarantineReason.DEVICE_FINGERPRINT_MISMATCH,
                detail = "canonical state is bound to device ${state.identity.deviceFingerprint}",
            )
        }
        return try {
            keys.dataKey()
            ActivationCheck(true, QuarantineReason.NONE, "device binding verified")
        } catch (refused: RuntimeException) {
            ActivationCheck(
                admitted = false,
                reason = QuarantineReason.KEY_CONTAINER_REFUSED,
                detail = "key container refused: ${refused.message}",
            )
        }
    }

    /** The canonical event that records a quarantine. */
    public fun quarantineEvent(state: ContinuityState): ContinuityEvent = ContinuityEvent.of(
        state.lastCommitSequence + 1L,
        ContinuityEventType.QUARANTINE_ENTERED,
        durability = DurabilityClass.WITNESSED,
    )
}

/** Recovery freshness classification, contract section 14.4. */
public enum class RecoveryFreshness(public val ordinal32: Int) {
    FRESH(1),
    STALE_WARNING(2),
    CRITICAL_WARNING(3),
}

/** Provider confirmation of one uploaded cold package. */
public class ProviderReceipt(
    public val receiptIdentifier: String,
    public val objectSizeBytes: Long,
    public val checksumHex: String,
    public val packageSequence: Long,
)

/**
 * Provider-neutral cold-package storage, contract section 14.4.
 *
 * With no production provider selected, every implementation of this interface
 * is `BLOCKED_SPEC_RECOVERY_PROVIDER`. The interface exists anyway so that the
 * rule it enforces — a provider can confirm bytes and nothing else — is visible
 * rather than implied.
 */
public interface RecoveryPackageStore {
    public val providerStatus: String

    public fun upload(packageBytes: ByteArray, packageSequence: Long): ProviderReceipt
}

/** The declared blocked provider. Its calls fail loudly rather than pretend. */
public object BlockedRecoveryPackageStore : RecoveryPackageStore {
    public const val STATUS: String = "BLOCKED_SPEC_RECOVERY_PROVIDER"

    override val providerStatus: String get() = STATUS

    override fun upload(packageBytes: ByteArray, packageSequence: Long): ProviderReceipt =
        throw UnsupportedOperationException(
            "no production recovery storage provider has been selected; provider-specific " +
                "implementation is $STATUS and current-device correctness does not depend on it",
        )
}

/**
 * Recovery-point accounting.
 *
 * A scheduled upload advances nothing. Freshness advances only when the provider
 * confirms the package **and** the application verifies receipt identifier,
 * size, checksum and sequence — all four, because any one of them alone can be
 * satisfied by a provider that stored something else.
 */
public object RecoveryPoint {

    public fun freshness(
        state: ContinuityState,
        nowVerifiedMillis: Long,
    ): RecoveryFreshness {
        val age = nowVerifiedMillis - state.identity.lastProtectedVerifiedMillis
        return when {
            age >= ContinuityContract.RECOVERY_CRITICAL_WARNING_MILLIS ->
                RecoveryFreshness.CRITICAL_WARNING
            age >= ContinuityContract.RECOVERY_STALE_WARNING_MILLIS ->
                RecoveryFreshness.STALE_WARNING
            else -> RecoveryFreshness.FRESH
        }
    }

    /**
     * Whether a receipt is sufficient to advance the protected recovery point.
     *
     * Returns false rather than throwing: a mismatched receipt is an ordinary
     * outcome of an opportunistic upload, not an error condition.
     */
    public fun receiptConfirms(
        receipt: ProviderReceipt,
        expectedIdentifier: String,
        expectedSize: Long,
        expectedChecksumHex: String,
        expectedSequence: Long,
    ): Boolean =
        receipt.receiptIdentifier == expectedIdentifier &&
            receipt.objectSizeBytes == expectedSize &&
            receipt.checksumHex == expectedChecksumHex &&
            receipt.packageSequence == expectedSequence

    /**
     * Declares a recovery gap.
     *
     * The organism never invents memories for the gap. This event records what
     * is known — the recovered point, the unavailable interval, the new epoch —
     * and nothing else.
     */
    public fun gapEvent(
        state: ContinuityState,
        knownUnavailableIntervalMillis: Long,
        newIdentityEpoch: Int,
    ): ContinuityEvent = ContinuityEvent.of(
        state.lastCommitSequence + 1L,
        ContinuityEventType.RECOVERY_GAP_DECLARED,
        a = knownUnavailableIntervalMillis,
        b = newIdentityEpoch.toLong(),
        durability = DurabilityClass.WITNESSED,
    )
}

/**
 * Status of the R002.12 work that this phase deliberately did not implement.
 *
 * Declared as data so that a later phase, a CI check or a governance reader can
 * see the blocked set without reading a report. Inventing these choices inside
 * persistence or UI code is prohibited by Implementation Plan E2E R002.12, and
 * this is what not inventing them looks like.
 */
public object RecoveryScopeStatus {
    public const val PROVIDER: String = "BLOCKED_SPEC_RECOVERY_PROVIDER"
    public const val CRYPTOGRAPHY: String = "BLOCKED_SPEC_RECOVERY_CRYPTOGRAPHY"

    public val blockedCapabilities: List<String> = listOf(
        "recovery-secret entropy, encoding and checksum",
        "mnemonic wordlist and user-held recovery root",
        "KDF and recovery-key wrapping",
        "cold-package manifest signature format",
        "identity-epoch challenge-response and activation lease",
        "epoch revocation and replay protection",
        "live device transfer ceremony",
    )

    public val reason: String =
        "RecoveryCryptographyContractV1 is not frozen and directive D007 did not authorize " +
            "this phase to freeze it; no production storage provider has been selected"
}
