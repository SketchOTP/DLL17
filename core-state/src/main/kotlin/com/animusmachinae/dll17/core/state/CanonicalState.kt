package com.animusmachinae.dll17.core.state

import com.animusmachinae.dll17.core.crypto.CanonicalEnvelope
import com.animusmachinae.dll17.core.crypto.CanonicalHash
import com.animusmachinae.dll17.core.crypto.CanonicalMapEntry
import com.animusmachinae.dll17.core.crypto.CanonicalReader
import com.animusmachinae.dll17.core.crypto.CanonicalWriter
import com.animusmachinae.dll17.core.crypto.RandomDomainRegistry
import com.animusmachinae.dll17.core.crypto.RandomSubstream

/**
 * The R001 canonical state and event vocabulary.
 *
 * This is a **determinism kernel**, not an organism. Every field here exists to
 * exercise one frozen determinism property — fixed-point arithmetic, substream
 * isolation, ordered collections, serialization stability — and none of them
 * carries physiological, affective or behavioural meaning. Field names are
 * deliberately neutral for that reason: naming a field `hunger` would be an
 * organism decision, and organism decisions belong to R003 and later behind the
 * A001 gate.
 */

/** Normalized event types the R001 reducer accepts. Ordinals are immutable. */
public enum class CanonicalEventType(public val ordinal32: Int) {
    /** Advances the canonical logical clock by `operandA` ticks. */
    ADVANCE_TIME(1),

    /** Saturating-adds fixed-point `operandA` into `numericA`. */
    APPLY_DELTA(2),

    /** Decays `numericB` by retention factor `operandA`. */
    APPLY_DECAY(3),

    /** Interpolates `numericA` toward `operandA` by factor `operandB`. */
    APPLY_INTERPOLATION(4),

    /** Consumes one draw from the substream for domain `operandA` into `numericB`. */
    DRAW_RANDOM(5),

    /** A witnessed material transition. Class W; adds `operandA` to `materialUnits`. */
    MATERIAL_INTERACTION(6),
    ;

    public companion object {
        private val BY_ORDINAL: Map<Int, CanonicalEventType> =
            entries.associateBy { it.ordinal32 }

        public fun fromOrdinal(value: Int): CanonicalEventType = BY_ORDINAL[value]
            ?: throw IllegalArgumentException("unknown canonical event ordinal $value")
    }
}

/**
 * A normalized canonical event.
 *
 * Events carry only integers. Anything continuous is quantized before it reaches
 * this type, which is what makes cross-architecture byte identity achievable at
 * all: a device-dependent sensor float that reached the reducer would defeat
 * every other precaution in the contract.
 */
public class CanonicalEvent(
    public val logicalEventId: Long,
    public val type: CanonicalEventType,
    public val operandA: Long,
    public val operandB: Long,
    public val durabilityClass: DurabilityClass,
) {
    public fun canonicalPayloadBytes(): ByteArray = CanonicalWriter(40)
        .putI64(logicalEventId)
        .putEnum(type.ordinal32)
        .putI64(operandA)
        .putI64(operandB)
        .putEnum(durabilityClass.ordinal32)
        .toByteArray()

    override fun equals(other: Any?): Boolean =
        other is CanonicalEvent &&
            other.logicalEventId == logicalEventId &&
            other.type == type &&
            other.operandA == operandA &&
            other.operandB == operandB &&
            other.durabilityClass == durabilityClass

    override fun hashCode(): Int {
        var result = logicalEventId.hashCode()
        result = 31 * result + type.ordinal32
        result = 31 * result + operandA.hashCode()
        result = 31 * result + operandB.hashCode()
        result = 31 * result + durabilityClass.ordinal32
        return result
    }

    override fun toString(): String =
        "CanonicalEvent(#$logicalEventId $type a=$operandA b=$operandB ${durabilityClass.name})"

    public companion object {
        public const val EVENT_CONTRACT_VERSION: Int = 1

        public fun decodePayload(bytes: ByteArray): CanonicalEvent {
            val reader = CanonicalReader(bytes)
            val event = readFrom(reader)
            reader.requireExhausted()
            return event
        }

        public fun readFrom(reader: CanonicalReader): CanonicalEvent = CanonicalEvent(
            logicalEventId = reader.readI64(),
            type = CanonicalEventType.fromOrdinal(reader.readEnum()),
            operandA = reader.readI64(),
            operandB = reader.readI64(),
            durabilityClass = DurabilityClass.fromOrdinal(reader.readEnum()),
        )
    }
}

/**
 * Immutable canonical snapshot.
 *
 * Immutability is not a style preference here. `reduce` returning a new snapshot
 * rather than mutating one is what makes `reduce(s0, events) == replay(s0,
 * journal)` a comparison of values rather than a comparison of histories, and it
 * removes any possibility that a partially-applied event is observable.
 */
public class CanonicalSnapshot(
    public val schemaVersion: Int,
    public val logicalTime: Long,
    public val masterSeed: Long,
    public val randomContractVersion: Int,
    public val numericA: Long,
    public val numericB: Long,
    public val materialUnits: Long,
    public val lastCommitSequence: Long,
    substreams: List<RandomSubstream>,
) {
    /**
     * Substreams, held sorted by domain ID.
     *
     * A `Map` would be the natural type and is exactly what the contract
     * forbids: `HashMap` iteration order is not guaranteed to agree between a
     * desktop JDK and ART, so an unordered collection anywhere near canonical
     * bytes is a latent cross-target failure.
     */
    public val substreams: List<RandomSubstream> = substreams.sortedBy { it.domainId }

    init {
        for (index in 1 until this.substreams.size) {
            if (this.substreams[index - 1].domainId == this.substreams[index].domainId) {
                throw IllegalArgumentException(
                    "duplicate random domain ${this.substreams[index].domainId} in snapshot",
                )
            }
        }
        for (stream in this.substreams) RandomDomainRegistry.require(stream.domainId)
    }

    public fun substreamFor(domainId: Int): RandomSubstream =
        substreams.firstOrNull { it.domainId == domainId }
            ?: throw IllegalArgumentException("snapshot holds no substream for domain $domainId")

    public fun hasSubstream(domainId: Int): Boolean = substreams.any { it.domainId == domainId }

    /**
     * Returns a copy with [stream] replacing or joining the substream set.
     *
     * Adding a domain never touches any other domain's seed or counter, which is
     * the property the domain-insertion qualification depends on.
     */
    public fun withSubstream(stream: RandomSubstream): CanonicalSnapshot = copy(
        substreams = substreams.filter { it.domainId != stream.domainId } + stream,
    )

    public fun copy(
        schemaVersion: Int = this.schemaVersion,
        logicalTime: Long = this.logicalTime,
        masterSeed: Long = this.masterSeed,
        randomContractVersion: Int = this.randomContractVersion,
        numericA: Long = this.numericA,
        numericB: Long = this.numericB,
        materialUnits: Long = this.materialUnits,
        lastCommitSequence: Long = this.lastCommitSequence,
        substreams: List<RandomSubstream> = this.substreams,
    ): CanonicalSnapshot = CanonicalSnapshot(
        schemaVersion,
        logicalTime,
        masterSeed,
        randomContractVersion,
        numericA,
        numericB,
        materialUnits,
        lastCommitSequence,
        substreams,
    )

    /** Canonical serialization, envelope included. */
    public fun canonicalBytes(): ByteArray {
        val body = CanonicalWriter(128)
            .putI64(logicalTime)
            .putI64(masterSeed)
            .putI32(randomContractVersion)
            .putI64(numericA)
            .putI64(numericB)
            .putI64(materialUnits)
            .putI64(lastCommitSequence)

        // Substreams as a canonical map keyed by domain ID, so the encoding is
        // order-independent by construction rather than by the caller's care.
        val entries = substreams.map { stream ->
            CanonicalMapEntry(
                key = CanonicalWriter(4).putI32(stream.domainId).toByteArray(),
                value = stream.serialize(),
            )
        }
        body.putCanonicalMap(entries)

        return CanonicalEnvelope.wrap(SCHEMA_ID, schemaVersion, body.toByteArray())
    }

    public fun stateHash(): ByteArray = CanonicalHash.ofEnvelope(canonicalBytes())

    public fun stateHashHex(): String = CanonicalHash.hex(stateHash())

    override fun equals(other: Any?): Boolean =
        other is CanonicalSnapshot && canonicalBytes().contentEquals(other.canonicalBytes())

    override fun hashCode(): Int = canonicalBytes().contentHashCode()

    override fun toString(): String =
        "CanonicalSnapshot(v$schemaVersion t=$logicalTime a=$numericA b=$numericB " +
            "material=$materialUnits seq=$lastCommitSequence hash=${stateHashHex().take(16)})"

    public companion object {
        public const val SCHEMA_ID: Int = 201
        public const val SCHEMA_VERSION: Int = 1

        /** Engine contract version recorded in Class W receipts. */
        public const val ENGINE_CONTRACT_VERSION: Int = 1

        /**
         * The R001 genesis snapshot for a given master seed.
         *
         * Two substreams are derived up front so that the qualification fixtures
         * have streams to consume; the third registered domain is deliberately
         * absent so that the domain-insertion test has something to insert.
         */
        public fun genesis(masterSeed: Long): CanonicalSnapshot = CanonicalSnapshot(
            schemaVersion = SCHEMA_VERSION,
            logicalTime = 0L,
            masterSeed = masterSeed,
            randomContractVersion = CanonicalEnvelope.CONTRACT_VERSION,
            numericA = 0L,
            numericB = 0L,
            materialUnits = 0L,
            lastCommitSequence = 0L,
            substreams = listOf(
                RandomSubstream.derive(masterSeed, RandomDomainRegistry.DOMAIN_QUALIFICATION_PRIMARY),
                RandomSubstream.derive(masterSeed, RandomDomainRegistry.DOMAIN_QUALIFICATION_SECONDARY),
            ),
        )

        public fun decode(bytes: ByteArray): CanonicalSnapshot {
            val contents = CanonicalEnvelope.unwrap(bytes)
            if (contents.payloadSchemaId != SCHEMA_ID) {
                throw IllegalArgumentException(
                    "expected snapshot schema $SCHEMA_ID, got ${contents.payloadSchemaId}",
                )
            }
            if (contents.payloadSchemaVersion != SCHEMA_VERSION) {
                throw IllegalArgumentException(
                    "snapshot schema version ${contents.payloadSchemaVersion} requires migration; " +
                        "decode never guesses across versions",
                )
            }
            val reader = CanonicalReader(contents.payload)
            val logicalTime = reader.readI64()
            val masterSeed = reader.readI64()
            val randomContractVersion = reader.readI32()
            val numericA = reader.readI64()
            val numericB = reader.readI64()
            val materialUnits = reader.readI64()
            val lastCommitSequence = reader.readI64()
            val entries = reader.readCanonicalMap()
            reader.requireExhausted()

            val substreams = entries.map { entry ->
                RandomSubstream.deserialize(entry.value)
            }
            return CanonicalSnapshot(
                contents.payloadSchemaVersion,
                logicalTime,
                masterSeed,
                randomContractVersion,
                numericA,
                numericB,
                materialUnits,
                lastCommitSequence,
                substreams,
            )
        }
    }
}
