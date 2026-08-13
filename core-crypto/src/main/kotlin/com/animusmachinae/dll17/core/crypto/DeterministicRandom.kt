package com.animusmachinae.dll17.core.crypto

/**
 * Deterministic randomness, frozen by `DeterminismContractV1` sections 7 and 8.
 *
 * Two properties matter more than statistical quality here:
 *
 * 1. **Counter-based.** Draw `n` is computable directly from `(seed, n)`. Nothing
 *    replays draws to reach a position, so recovery after process death cannot
 *    drift and a substream's whole state is two `Long` values.
 * 2. **Domain isolation.** A substream's seed is a pure function of
 *    `(masterSeed, contractVersion, domainId)` and of nothing else — not of the
 *    registry's size, contents or iteration order. Registering a new domain
 *    therefore cannot perturb an existing stream. That is a structural
 *    guarantee, not a test result.
 */
public object SplitMix64 {

    /** Odd 64-bit approximation of the golden ratio: unsigned `0x9E3779B97F4A7C15`. */
    public const val GAMMA: Long = -0x61C8864680B583EBL

    private const val MIX_A: Long = -0x40A7B892E31B1A47L // unsigned 0xBF58476D1CE4E5B9
    private const val MIX_B: Long = -0x6B2FB644ECCEEE15L // unsigned 0x94D049BB133111EB

    /** Stafford variant 13 finalizer. Wrapping multiplication, logical shifts. */
    public fun mix64(input: Long): Long {
        var z = input
        z = (z xor (z ushr 30)) * MIX_A
        z = (z xor (z ushr 27)) * MIX_B
        return z xor (z ushr 31)
    }

    /** The draw at zero-based index [index] of the substream seeded with [seed]. */
    public fun draw(seed: Long, index: Long): Long = mix64(seed + (index + 1L) * GAMMA)
}

/** Substream seed derivation, contract section 8. Mixer ID `SUBSTREAM_DERIVE_V1`. */
public object SubstreamDerivation {
    public const val MIXER_ID: String = "SUBSTREAM_DERIVE_V1"

    public fun substreamSeed(masterSeed: Long, contractVersion: Int, domainId: Int): Long {
        var z = SplitMix64.mix64(masterSeed + 1L * SplitMix64.GAMMA + contractVersion.toLong())
        z = SplitMix64.mix64(z + 2L * SplitMix64.GAMMA + domainId.toLong())
        return z
    }
}

/**
 * One isolated random substream.
 *
 * The instance is mutable only in [counter]; `seed` is immutable for the life of
 * the organism. `counter` is durable canonical state, so a substream that has
 * been restored from a journal produces exactly the draws it would have produced
 * had the process never died.
 */
public class RandomSubstream(
    public val domainId: Int,
    public val seed: Long,
    counter: Long = 0L,
) {
    public var counter: Long = counter
        private set

    init {
        if (counter < 0L) throw CanonicalCodecException("substream counter must not be negative")
    }

    /** Algorithm ordinal written into the serialized state. */
    public val algorithmOrdinal: Int get() = ALGORITHM_ORDINAL

    /** Consumes one draw. No allocation, no string, no digest. */
    public fun nextLong(): Long {
        val value = SplitMix64.draw(seed, counter)
        counter += 1L
        return value
    }

    /** Reads draw [index] without consuming; used by replay verification. */
    public fun peekAt(index: Long): Long = SplitMix64.draw(seed, index)

    /**
     * Uniform value in `[0, bound)` by rejection sampling on the unsigned draw,
     * so the result carries no modulo bias. Rejection is deterministic: the same
     * counter position always rejects the same number of times.
     */
    public fun nextIntBelow(bound: Int): Int {
        if (bound <= 0) throw CanonicalCodecException("bound must be positive, got $bound")
        val boundLong = bound.toLong()
        // Largest multiple of bound at or below 2^63-1, used as the rejection threshold.
        val threshold = (Long.MAX_VALUE / boundLong) * boundLong
        while (true) {
            val raw = nextLong() ushr 1 // 63 unsigned bits
            if (raw < threshold) return (raw % boundLong).toInt()
        }
    }

    public fun copy(): RandomSubstream = RandomSubstream(domainId, seed, counter)

    /** Fixed 24-byte layout, contract section 7.3. */
    public fun serialize(): ByteArray = CanonicalWriter(SERIALIZED_SIZE)
        .putI32(ALGORITHM_ORDINAL)
        .putI32(domainId)
        .putI64(seed)
        .putI64(counter)
        .toByteArray()

    public fun writeTo(writer: CanonicalWriter) {
        writer.putI32(ALGORITHM_ORDINAL).putI32(domainId).putI64(seed).putI64(counter)
    }

    override fun equals(other: Any?): Boolean =
        other is RandomSubstream &&
            other.domainId == domainId &&
            other.seed == seed &&
            other.counter == counter

    override fun hashCode(): Int {
        var result = domainId
        result = 31 * result + seed.hashCode()
        result = 31 * result + counter.hashCode()
        return result
    }

    override fun toString(): String = "RandomSubstream(domain=$domainId, counter=$counter)"

    public companion object {
        public const val ALGORITHM_ID: String = "PRNG_SPLITMIX64_V1"
        public const val ALGORITHM_ORDINAL: Int = 1
        public const val SERIALIZED_SIZE: Int = 24

        public fun readFrom(reader: CanonicalReader): RandomSubstream {
            val algorithm = reader.readI32()
            if (algorithm != ALGORITHM_ORDINAL) {
                throw CanonicalCodecException(
                    "unknown PRNG algorithm ordinal $algorithm; this build implements only $ALGORITHM_ID",
                )
            }
            val domainId = reader.readI32()
            val seed = reader.readI64()
            val counter = reader.readI64()
            if (counter < 0L) throw CanonicalCodecException("substream counter must not be negative")
            return RandomSubstream(domainId, seed, counter)
        }

        public fun deserialize(bytes: ByteArray): RandomSubstream {
            val reader = CanonicalReader(bytes)
            val stream = readFrom(reader)
            reader.requireExhausted()
            return stream
        }

        /** Derives a fresh substream for [domainId] from the organism master seed. */
        public fun derive(masterSeed: Long, domainId: Int): RandomSubstream = RandomSubstream(
            domainId = domainId,
            seed = SubstreamDerivation.substreamSeed(
                masterSeed,
                CanonicalEnvelope.CONTRACT_VERSION,
                domainId,
            ),
            counter = 0L,
        )
    }
}

/**
 * The executable half of `docs/architecture/registries/RandomDomainRegistry.md`.
 *
 * Domain IDs are immutable integers and are never reused. R001 registers only
 * the domains it actually draws from: the qualification harness needs two to
 * prove isolation, and a third exists solely to be inserted mid-test so the
 * insertion-safety property has something to demonstrate on. No organism domain
 * is registered here, because no organism behaviour exists.
 */
public object RandomDomainRegistry {

    public const val REGISTRY_VERSION: Int = 1

    /** Reference substream used by the deterministic qualification kernel. */
    public const val DOMAIN_QUALIFICATION_PRIMARY: Int = 1

    /** Second reference substream, used to show two domains do not interfere. */
    public const val DOMAIN_QUALIFICATION_SECONDARY: Int = 2

    /**
     * Registered late in the domain-insertion test. It exists to be added after
     * the first two have already been consumed, which is the only way to
     * demonstrate that insertion leaves prior streams untouched.
     */
    public const val DOMAIN_QUALIFICATION_LATE_INSERT: Int = 3

    private val REGISTERED: IntArray = intArrayOf(
        DOMAIN_QUALIFICATION_PRIMARY,
        DOMAIN_QUALIFICATION_SECONDARY,
        DOMAIN_QUALIFICATION_LATE_INSERT,
    )

    public fun registeredDomainIds(): IntArray = REGISTERED.copyOf()

    public fun isRegistered(domainId: Int): Boolean {
        for (id in REGISTERED) if (id == domainId) return true
        return false
    }

    public fun require(domainId: Int): Int {
        if (!isRegistered(domainId)) {
            throw CanonicalCodecException(
                "random domain $domainId is not in RandomDomainRegistry; every draw must " +
                    "belong to a registered domain",
            )
        }
        return domainId
    }
}
