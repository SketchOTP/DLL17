package com.animusmachinae.dll17.core.continuity

import com.animusmachinae.dll17.core.crypto.CanonicalEnvelope
import com.animusmachinae.dll17.core.crypto.CanonicalReader
import com.animusmachinae.dll17.core.crypto.CanonicalWriter
import com.animusmachinae.dll17.core.state.DurabilityClass

/**
 * The normalized R002 continuity events, contract section 16.
 *
 * Reconciliation does not mutate state directly. It **plans a deterministic
 * event sequence**, and those events are folded by the reducer exactly like any
 * other canonical input. That indirection is the reason `reduce == replay` holds
 * for a reconciliation at all: a reconciliation that wrote state directly would
 * leave nothing in the journal to replay, and a restart in the middle of one
 * would be unrecoverable by construction.
 */
public enum class ContinuityEventType(public val ordinal32: Int) {
    /** A durable time anchor was written. Carries the anchor payload. */
    ANCHOR_WRITTEN(1),

    /** Same-boot wall/monotonic disagreement exceeded tolerance. `a` = skew. */
    CLOCK_ANOMALY_DETECTED(2),

    /** `a` milliseconds of qualified verified elapsed time. */
    VERIFIED_TIME_ADVANCED(3),

    /** `a` baseline-equivalent milliseconds of ordinary metabolism. */
    BASELINE_METABOLISM_APPLIED(4),

    /** `a` milliseconds granted, `b` = new carried remainder. */
    BLIND_CREDIT_REPLENISHED(5),

    /** `a` milliseconds consumed, `b` = boot identity it was consumed for. */
    BLIND_CREDIT_CONSUMED(6),

    /** `a` accrued baseline-equivalent millis, `b` = excess forgiven at accrual. */
    UNRESOLVED_TIME_DEBT_ACCRUED(7),

    /** `a` baseline-equivalent millis of bounded debt collection. */
    METABOLIC_ADJUSTMENT_APPLIED(8),

    /** Collection paused at the safety floor. */
    DEBT_PAUSED_LOW_RESERVE(9),

    /** Rearm armed; `a` = verified time at which it becomes effective. */
    DEBT_REARM_ARMED(10),

    /** `a` baseline-equivalent millis expired past the retention horizon. */
    DEBT_FORGIVEN(11),

    /** Mode C completion. `a` = total elapsed millis reconciled. */
    EXTENDED_ABSENCE_RECONCILED(12),

    DURABILITY_SAFE_HOLD_ENTERED(13),
    DURABILITY_SAFE_HOLD_EXITED(14),

    /** `a` = suspend reason class ordinal. */
    PLATFORM_DEEP_SUSPEND_ENTERED(15),

    PLATFORM_RECOVERY_COMPLETED(16),

    /** `a` = target [PlatformProtectionState] ordinal. */
    PLATFORM_STATE_CHANGED(17),

    /** `a` = last durable commit sequence recovery resumed from. */
    RECOVERY_PERFORMED(18),

    /** `a` = known unavailable interval millis, `b` = new identity epoch. */
    RECOVERY_GAP_DECLARED(19),

    /** `a` = checkpoint sequence. */
    SNAPSHOT_CREATED(20),

    /** `a` = from schema version, `b` = to schema version. */
    MIGRATION_PERFORMED(21),

    /** `a` = new generation ID. */
    GENERATION_FLIPPED(22),

    /** `a` = target [DurabilityAdmissionState] ordinal. */
    ADMISSION_STATE_CHANGED(23),

    /** `a` = ticks. Foreground canonical activity only. */
    ACTIVE_EXPERIENCE_ADVANCED(24),

    /** Neutral R002 care fixture: `a` and `b` restore `reserveA` and `reserveB`. */
    RESERVE_RESTORED(25),

    /** `a` = boot identity, `b` = 1 when the boot is inside a velocity anomaly. */
    BOOT_OBSERVED(26),

    /** Copied canonical state without a matching device key. */
    QUARANTINE_ENTERED(27),

    /** `a` = [GapProvenance] ordinal. A label; it changes nothing else. */
    GAP_PROVENANCE_LABELLED(28),

    /** `a` = milliseconds of bounded passive development, verified time only. */
    PASSIVE_DEVELOPMENT_APPLIED(29),

    /** `a` = target [DurabilityPresentationState] ordinal. */
    PRESENTATION_STATE_CHANGED(30),

    /**
     * Abundance-stability tracking for debt rearm. `a` = verified time the
     * reserves first reached abundance, `b` = 1 when present, 0 when reset.
     */
    DEBT_ABUNDANCE_STABILITY_UPDATED(31),
    ;

    public companion object {
        private val BY_ORDINAL: Map<Int, ContinuityEventType> = entries.associateBy { it.ordinal32 }

        public fun fromOrdinal(value: Int): ContinuityEventType = BY_ORDINAL[value]
            ?: throw IllegalArgumentException("unknown continuity event ordinal $value")
    }
}

/** One normalized continuity event. Canonical schema `212`. */
public class ContinuityEvent(
    public val logicalEventId: Long,
    public val type: ContinuityEventType,
    public val operandA: Long,
    public val operandB: Long,
    public val durabilityClass: DurabilityClass,
    public val anchor: DurableTimeAnchor?,
) {
    public fun canonicalPayloadBytes(): ByteArray {
        val writer = CanonicalWriter(96)
            .putI64(logicalEventId)
            .putEnum(type.ordinal32)
            .putI64(operandA)
            .putI64(operandB)
            .putEnum(durabilityClass.ordinal32)
            .putBool(anchor != null)
        anchor?.writeTo(writer)
        return writer.toByteArray()
    }

    public fun canonicalBytes(): ByteArray =
        CanonicalEnvelope.wrap(SCHEMA_ID, SCHEMA_VERSION, canonicalPayloadBytes())

    override fun equals(other: Any?): Boolean =
        other is ContinuityEvent && canonicalPayloadBytes().contentEquals(other.canonicalPayloadBytes())

    override fun hashCode(): Int = canonicalPayloadBytes().contentHashCode()

    override fun toString(): String =
        "ContinuityEvent(#$logicalEventId $type a=$operandA b=$operandB ${durabilityClass.name})"

    public companion object {
        public const val SCHEMA_ID: Int = 212
        public const val SCHEMA_VERSION: Int = 1
        public const val EVENT_CONTRACT_VERSION: Int = 1

        public fun of(
            id: Long,
            type: ContinuityEventType,
            a: Long = 0L,
            b: Long = 0L,
            durability: DurabilityClass = DurabilityClass.ORDINARY,
            anchor: DurableTimeAnchor? = null,
        ): ContinuityEvent = ContinuityEvent(id, type, a, b, durability, anchor)

        public fun readFrom(reader: CanonicalReader): ContinuityEvent {
            val id = reader.readI64()
            val type = ContinuityEventType.fromOrdinal(reader.readEnum())
            val a = reader.readI64()
            val b = reader.readI64()
            val durability = DurabilityClass.fromOrdinal(reader.readEnum())
            val hasAnchor = reader.readBool()
            val anchor = if (hasAnchor) DurableTimeAnchor.readFrom(reader) else null
            return ContinuityEvent(id, type, a, b, durability, anchor)
        }

        public fun decodePayload(bytes: ByteArray): ContinuityEvent {
            val reader = CanonicalReader(bytes)
            val event = readFrom(reader)
            reader.requireExhausted()
            return event
        }
    }
}
