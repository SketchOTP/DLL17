package com.animusmachinae.dll17.core.state

import com.animusmachinae.dll17.core.crypto.RandomDomainRegistry
import com.animusmachinae.dll17.core.math.ArithmeticContext
import com.animusmachinae.dll17.core.math.FixedPoint
import com.animusmachinae.dll17.core.math.RecordingSaturationObserver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `reduce(snapshot0, orderedCommittedEvents) == replay(snapshot0, durableJournal)`,
 * per Implementation Plan E2E work package R001.5.
 *
 * Equality means identical canonical bytes and identical canonical state hash,
 * not equivalent object values. The tests compare hex digests for that reason: an
 * `equals` that compared fields would pass even if the serializer had a bug, and
 * the serializer is exactly what the cross-target claim depends on.
 */
class ReplayEquivalenceTest {

    private fun ctx() = ArithmeticContext.unattributed(RecordingSaturationObserver())

    @Test
    fun everyQualificationFixtureReplaysToTheSameCanonicalBytes() {
        for (fixture in R001QualificationKernel.fixtures()) {
            val genesis = CanonicalSnapshot.genesis(fixture.masterSeed)

            val direct = CanonicalReducer.reduceAll(genesis, fixture.events, ctx())

            val journal = DurableJournal()
            val pipeline = WitnessedInteractionPipeline(genesis, journal)
            val journalCtx = ctx()
            for (event in fixture.events) pipeline.submit(event, journalCtx)

            val replayed = ReplayKernel.replay(genesis, journal, ctx())

            assertTrue(replayed.isConsistent, "${fixture.id} diverged at ${replayed.divergedAtSequence}")
            assertEquals(
                direct.stateHashHex(),
                replayed.snapshot.stateHashHex(),
                "${fixture.id}: direct reduction and journal replay must agree on the state hash",
            )
            assertTrue(
                direct.canonicalBytes().contentEquals(replayed.snapshot.canonicalBytes()),
                "${fixture.id}: byte identity, not merely equal field values",
            )
            assertEquals(fixture.events.size, replayed.framesApplied)
        }
    }

    @Test
    fun replayFromTheDurableMediumAloneReproducesTheOrganism() {
        val fixture = R001QualificationKernel.fixtures().first { it.id == "FX-RANDOM-01" }
        val genesis = CanonicalSnapshot.genesis(fixture.masterSeed)

        val journal = DurableJournal()
        val pipeline = WitnessedInteractionPipeline(genesis, journal)
        val liveCtx = ctx()
        for (event in fixture.events) pipeline.submit(event, liveCtx)
        val live = pipeline.published

        // Recovery sees only bytes. No in-memory object survives.
        val recovered = InteractionRecovery.recover(genesis, journal.rawBytes(), ctx())

        assertEquals(live.stateHashHex(), recovered.snapshot.stateHashHex())
        assertEquals(
            live.substreamFor(RandomDomainRegistry.DOMAIN_QUALIFICATION_PRIMARY).counter,
            recovered.snapshot.substreamFor(RandomDomainRegistry.DOMAIN_QUALIFICATION_PRIMARY).counter,
            "PRNG substream counters are canonical state and must survive recovery",
        )
    }

    @Test
    fun theReducerIsPure() {
        val genesis = CanonicalSnapshot.genesis(99L)
        val event = CanonicalEvent(
            1L,
            CanonicalEventType.APPLY_DELTA,
            FixedPoint.of(1),
            0L,
            DurabilityClass.ORDINARY,
        )
        val results = (0 until 50).map { CanonicalReducer.reduce(genesis, event, ctx()).stateHashHex() }
        assertEquals(1, results.distinct().size, "the reducer must be a pure function")
        assertEquals(
            CanonicalSnapshot.genesis(99L).stateHashHex(),
            genesis.stateHashHex(),
            "the input snapshot must not be mutated",
        )
    }

    @Test
    fun snapshotSerializationIsStableAcrossEncodeDecodeCycles() {
        for (fixture in R001QualificationKernel.fixtures()) {
            var snapshot = CanonicalSnapshot.genesis(fixture.masterSeed)
            snapshot = CanonicalReducer.reduceAll(snapshot, fixture.events, ctx())

            var bytes = snapshot.canonicalBytes()
            repeat(5) {
                val decoded = CanonicalSnapshot.decode(bytes)
                val reencoded = decoded.canonicalBytes()
                assertTrue(
                    bytes.contentEquals(reencoded),
                    "${fixture.id}: re-encoding a decoded snapshot must be byte-identical",
                )
                bytes = reencoded
            }
        }
    }

    @Test
    fun substreamOrderInTheSnapshotDoesNotAffectCanonicalBytes() {
        val genesis = CanonicalSnapshot.genesis(5L)
        val reordered = CanonicalSnapshot(
            genesis.schemaVersion,
            genesis.logicalTime,
            genesis.masterSeed,
            genesis.randomContractVersion,
            genesis.numericA,
            genesis.numericB,
            genesis.materialUnits,
            genesis.lastCommitSequence,
            genesis.substreams.reversed(),
        )
        assertTrue(
            genesis.canonicalBytes().contentEquals(reordered.canonicalBytes()),
            "substream ordering must not reach the canonical bytes",
        )
    }

    @Test
    fun aTamperedJournalIsDetectedRatherThanApplied() {
        val genesis = CanonicalSnapshot.genesis(11L)
        val journal = DurableJournal()
        val pipeline = WitnessedInteractionPipeline(genesis, journal)
        val liveCtx = ctx()
        repeat(3) { index ->
            pipeline.submit(
                CanonicalEvent(
                    index + 1L,
                    CanonicalEventType.APPLY_DELTA,
                    FixedPoint.of(1),
                    0L,
                    DurabilityClass.ORDINARY,
                ),
                liveCtx,
            )
        }

        // Rebuild the journal with one frame's event operand altered.
        val records = journal.rawBytes().toMutableList()
        val tampered = CommitFrame.decode(records[1])
        records[1] = CommitFrame(
            tampered.sequence,
            CanonicalEvent(
                tampered.event.logicalEventId,
                tampered.event.type,
                FixedPoint.of(999),
                tampered.event.operandB,
                tampered.event.durabilityClass,
            ),
            tampered.preStateHash,
            tampered.postStateHash,
            tampered.receiptDigest,
        ).canonicalBytes()

        val result = ReplayKernel.replay(genesis, DurableJournal.fromRawBytes(records), ctx())
        assertTrue(!result.isConsistent, "a tampered frame must not replay silently")
        assertEquals(2L, result.divergedAtSequence)
    }

    @Test
    fun intendedOperatingRangesDoNotSaturate() {
        // The R001 exit gate treats saturation inside a normal operating range as
        // a failure, so this asserts an absence rather than a behaviour.
        val normalFixtures = R001QualificationKernel.normalOperatingFixtureIds()
        for (fixture in R001QualificationKernel.fixtures()) {
            if (fixture.id !in normalFixtures) continue
            val observer = RecordingSaturationObserver()
            val ctx = ArithmeticContext.unattributed(observer)
            CanonicalReducer.reduceAll(CanonicalSnapshot.genesis(fixture.masterSeed), fixture.events, ctx)
            assertEquals(
                0,
                observer.count,
                "${fixture.id} saturated inside its intended operating range: ${observer.recorded()}",
            )
        }
    }

    @Test
    fun logicalTimeNeverRunsBackwards() {
        val genesis = CanonicalSnapshot.genesis(1L)
        val backwards = CanonicalEvent(
            1L,
            CanonicalEventType.ADVANCE_TIME,
            -1L,
            0L,
            DurabilityClass.ORDINARY,
        )
        val failure = runCatching { CanonicalReducer.reduce(genesis, backwards, ctx()) }
        assertTrue(failure.isFailure, "negative time advance must be refused")
    }
}
