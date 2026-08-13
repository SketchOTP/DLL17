package com.animusmachinae.dll17.core.state

import com.animusmachinae.dll17.core.crypto.CanonicalHash
import com.animusmachinae.dll17.core.crypto.CanonicalReader
import com.animusmachinae.dll17.core.crypto.CanonicalWriter

/**
 * Durability classes and the durable-input receipt, per Implementation Plan E2E
 * work package R001.9.
 *
 * The three classes exist to answer one question precisely: what is allowed to be
 * visible to the user before the durable write lands?
 */
public enum class DurabilityClass(public val ordinal32: Int) {
    /**
     * Class E — ephemeral presentation acknowledgement. A ripple, a highlight, a
     * haptic. It asserts only that the interface received input. Noncanonical,
     * immediate, and never evidence of anything.
     */
    EPHEMERAL(1),

    /**
     * Class O — ordinary canonical progress, eligible for bounded batching. The
     * batching window is measured and frozen by qualification; R001 fixes no
     * cadence because a cadence is a parameter and no device evidence exists yet.
     */
    ORDINARY(2),

    /**
     * Class W — witnessed material transition. Requires durable input evidence to
     * be acknowledged before the final semantic presentation may run.
     */
    WITNESSED(3),
    ;

    public companion object {
        public fun fromOrdinal(value: Int): DurabilityClass = when (value) {
            1 -> EPHEMERAL
            2 -> ORDINARY
            3 -> WITNESSED
            else -> throw IllegalArgumentException("unknown durability class ordinal $value")
        }
    }
}

/**
 * `MaterialInteractionReceipt` — the durable input evidence for a Class W
 * transition.
 *
 * The canonical architecture requires it to carry, at minimum, the normalized
 * input, the engine and event contract versions, the logical event ID, the
 * relevant pre-input state hash, and replay metadata sufficient to reapply the
 * interaction exactly. Everything here is one of those; nothing here is
 * presentation state.
 */
public class MaterialInteractionReceipt(
    public val logicalEventId: Long,
    public val engineContractVersion: Int,
    public val eventContractVersion: Int,
    public val normalizedInput: CanonicalEvent,
    public val preInputStateHash: ByteArray,
    public val replaySequence: Long,
) {
    /**
     * At-most-once presentation token.
     *
     * The token is consumed when the durable append starts, not when the
     * animation finishes. That ordering is the whole point: a recovery may
     * legitimately replay the canonical interaction, but it must never emit a
     * delayed final reaction merely because the process died while an animation
     * was in flight.
     */
    public var presentationToken: PresentationToken = PresentationToken.AVAILABLE
        private set

    public fun consumePresentationToken(): Boolean {
        if (presentationToken != PresentationToken.AVAILABLE) return false
        presentationToken = PresentationToken.CONSUMED_AT_START
        return true
    }

    public fun canonicalBytes(): ByteArray {
        val payload = CanonicalWriter(96)
            .putI64(logicalEventId)
            .putI32(engineContractVersion)
            .putI32(eventContractVersion)
            .putBytes(normalizedInput.canonicalPayloadBytes())
            .putBytes(preInputStateHash)
            .putI64(replaySequence)
            // The presentation token is deliberately part of the durable receipt:
            // at-most-once is a durability property, not a UI property.
            .putEnum(presentationToken.ordinal32)
            .toByteArray()
        return com.animusmachinae.dll17.core.crypto.CanonicalEnvelope.wrap(
            SCHEMA_ID,
            SCHEMA_VERSION,
            payload,
        )
    }

    public fun digestHex(): String =
        CanonicalHash.hex(CanonicalHash.ofEnvelope(canonicalBytes()))

    public companion object {
        public const val SCHEMA_ID: Int = 202
        public const val SCHEMA_VERSION: Int = 1

        public fun decode(bytes: ByteArray): MaterialInteractionReceipt {
            val contents = com.animusmachinae.dll17.core.crypto.CanonicalEnvelope.unwrap(bytes)
            if (contents.payloadSchemaId != SCHEMA_ID) {
                throw IllegalArgumentException(
                    "expected receipt schema $SCHEMA_ID, got ${contents.payloadSchemaId}",
                )
            }
            val reader = CanonicalReader(contents.payload)
            val logicalEventId = reader.readI64()
            val engineVersion = reader.readI32()
            val eventVersion = reader.readI32()
            val event = CanonicalEvent.decodePayload(reader.readBytes())
            val preHash = reader.readBytes()
            val replaySequence = reader.readI64()
            val token = PresentationToken.fromOrdinal(reader.readEnum())
            reader.requireExhausted()
            val receipt = MaterialInteractionReceipt(
                logicalEventId,
                engineVersion,
                eventVersion,
                event,
                preHash,
                replaySequence,
            )
            receipt.presentationToken = token
            return receipt
        }
    }
}

/** At-most-once presentation accounting for a Class W interaction. */
public enum class PresentationToken(public val ordinal32: Int) {
    AVAILABLE(1),
    CONSUMED_AT_START(2),
    ;

    public companion object {
        public fun fromOrdinal(value: Int): PresentationToken = when (value) {
            1 -> AVAILABLE
            2 -> CONSUMED_AT_START
            else -> throw IllegalArgumentException("unknown presentation token ordinal $value")
        }
    }
}
