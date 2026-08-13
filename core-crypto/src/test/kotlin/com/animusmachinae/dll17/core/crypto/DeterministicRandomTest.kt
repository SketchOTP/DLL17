package com.animusmachinae.dll17.core.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Randomness contract tests, per `DeterminismContractV1` sections 7 and 8 and
 * Implementation Plan E2E work package R001.4.
 *
 * The two properties that matter for the R001 gate are counter-based recovery
 * and domain isolation. Statistical quality is deliberately not tested here: the
 * contract claims none, and a test asserting distribution properties of a
 * non-cryptographic generator would imply a guarantee the project does not make.
 */
class DeterministicRandomTest {

    @Test
    fun theFrozenConstantsAreTranscribedCorrectly() {
        // Transcribing a 64-bit constant by hand is a real failure mode, and a
        // wrong GAMMA would produce a plausible-looking generator that no other
        // implementation agrees with. Checking the unsigned hex catches it.
        assertEquals("9e3779b97f4a7c15", SplitMix64.GAMMA.toULong().toString(16))
    }

    @Test
    fun mix64IsAPureFunction() {
        for (input in listOf(0L, 1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE, 0x5DEECE66DL)) {
            val first = SplitMix64.mix64(input)
            repeat(10) { assertEquals(first, SplitMix64.mix64(input)) }
        }
    }

    @Test
    fun drawsAreCounterBasedAndPositionIndependent() {
        val stream = RandomSubstream(1, seed = 0x0123456789ABCDEFL)
        val sequential = (0 until 32).map { stream.nextLong() }

        // The same values must be reachable directly, without replaying.
        val direct = (0 until 32).map { SplitMix64.draw(0x0123456789ABCDEFL, it.toLong()) }
        assertEquals(sequential, direct, "draw(seed, n) must not depend on having drawn 0..n-1")

        // And a stream restored at position 20 must continue identically.
        val restored = RandomSubstream(1, seed = 0x0123456789ABCDEFL, counter = 20L)
        assertEquals(sequential[20], restored.nextLong())
    }

    @Test
    fun substreamStateSerializesToAFixedTwentyFourByteLayoutAndRoundTrips() {
        val stream = RandomSubstream(7, seed = -42L, counter = 0L)
        repeat(5) { stream.nextLong() }

        val bytes = stream.serialize()
        assertEquals(RandomSubstream.SERIALIZED_SIZE, bytes.size)

        val restored = RandomSubstream.deserialize(bytes)
        assertEquals(stream.domainId, restored.domainId)
        assertEquals(stream.seed, restored.seed)
        assertEquals(stream.counter, restored.counter)
        assertEquals(stream.nextLong(), restored.nextLong(), "restored stream must continue exactly")
    }

    @Test
    fun anUnknownAlgorithmOrdinalIsRefused() {
        val bytes = CanonicalWriter(24).putI32(99).putI32(1).putI64(0L).putI64(0L).toByteArray()
        assertFailsWith<CanonicalCodecException> { RandomSubstream.deserialize(bytes) }
    }

    @Test
    fun addingADomainCannotPerturbAnyExistingSubstream() {
        val masterSeed = 0x5EEDL

        val before = RandomDomainRegistry.registeredDomainIds().map { domainId ->
            domainId to RandomSubstream.derive(masterSeed, domainId).let { stream ->
                (0 until 16).map { stream.nextLong() }
            }
        }.toMap()

        // Derive a domain ID that was never registered, exactly as a future phase
        // adding a new domain would.
        val newDomainId = 4242
        val newcomer = RandomSubstream(
            newDomainId,
            SubstreamDerivation.substreamSeed(
                masterSeed,
                CanonicalEnvelope.CONTRACT_VERSION,
                newDomainId,
            ),
        )
        repeat(64) { newcomer.nextLong() }

        val after = RandomDomainRegistry.registeredDomainIds().map { domainId ->
            domainId to RandomSubstream.derive(masterSeed, domainId).let { stream ->
                (0 until 16).map { stream.nextLong() }
            }
        }.toMap()

        assertEquals(before, after, "existing substreams must be untouched by domain insertion")
    }

    @Test
    fun distinctDomainsProduceDistinctStreamsFromTheSameMasterSeed() {
        val masterSeed = 12345L
        val streams = RandomDomainRegistry.registeredDomainIds().map { domainId ->
            RandomSubstream.derive(masterSeed, domainId).let { stream ->
                (0 until 8).map { stream.nextLong() }
            }
        }
        for (i in streams.indices) {
            for (j in i + 1 until streams.size) {
                assertTrue(streams[i] != streams[j], "domains $i and $j collided")
            }
        }
    }

    @Test
    fun substreamSeedDependsOnlyOnSeedVersionAndDomain() {
        val a = SubstreamDerivation.substreamSeed(99L, 1, 7)
        val b = SubstreamDerivation.substreamSeed(99L, 1, 7)
        assertEquals(a, b)
        assertTrue(a != SubstreamDerivation.substreamSeed(99L, 1, 8))
        assertTrue(a != SubstreamDerivation.substreamSeed(99L, 2, 7))
        assertTrue(a != SubstreamDerivation.substreamSeed(100L, 1, 7))
    }

    @Test
    fun boundedDrawsStayInRangeAndAreReproducible() {
        val first = RandomSubstream.derive(7L, RandomDomainRegistry.DOMAIN_QUALIFICATION_PRIMARY)
        val second = RandomSubstream.derive(7L, RandomDomainRegistry.DOMAIN_QUALIFICATION_PRIMARY)
        repeat(2_000) {
            val value = first.nextLong().let { _ -> first.nextIntBelow(1_000_001) }
            assertTrue(value in 0..1_000_000, "bounded draw escaped its range: $value")
            second.nextLong()
            assertEquals(value, second.nextIntBelow(1_000_001))
        }
        assertFailsWith<CanonicalCodecException> { first.nextIntBelow(0) }
    }

    @Test
    fun everyDrawBelongsToARegisteredDomain() {
        for (domainId in RandomDomainRegistry.registeredDomainIds()) {
            assertEquals(domainId, RandomDomainRegistry.require(domainId))
        }
        assertFailsWith<CanonicalCodecException> { RandomDomainRegistry.require(9999) }
    }

    @Test
    fun theRegistryMatchesItsDocumentedDomains() {
        assertEquals(
            listOf(1, 2, 3),
            RandomDomainRegistry.registeredDomainIds().toList(),
            "domain IDs are immutable and never reused; changing this list is a contract change",
        )
    }
}
