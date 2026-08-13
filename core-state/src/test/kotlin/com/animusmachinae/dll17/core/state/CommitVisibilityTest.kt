package com.animusmachinae.dll17.core.state

import com.animusmachinae.dll17.core.math.ArithmeticContext
import com.animusmachinae.dll17.core.math.FixedPoint
import com.animusmachinae.dll17.core.math.RecordingSaturationObserver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The commit visibility invariant and crash survivability, per Implementation
 * Plan E2E work packages R001.6 and R001.9.
 *
 * The sweep below crashes at every boundary the plan names and asserts three
 * things after each: canonical state recovers exactly to the last durably
 * acknowledged frame, no uncommitted material mutation was ever visible, and no
 * final semantic presentation was emitted more than once — including across
 * recovery.
 */
class CommitVisibilityTest {

    private fun ctx() = ArithmeticContext.unattributed(RecordingSaturationObserver())

    private fun witnessedEvent(id: Long, units: Long) = CanonicalEvent(
        id,
        CanonicalEventType.MATERIAL_INTERACTION,
        units,
        0L,
        DurabilityClass.WITNESSED,
    )

    @Test
    fun everyCrashBoundaryRecoversToTheLastDurablyAcknowledgedFrame() {
        val boundaries = CrashBoundary.injectable()
        assertEquals(8, boundaries.size, "the plan names eight boundaries; all must be covered")

        for (boundary in boundaries) {
            val genesis = CanonicalSnapshot.genesis(0xC0FFEEL)
            val journal = DurableJournal()
            val pipeline = WitnessedInteractionPipeline(genesis, journal)
            val liveCtx = ctx()

            // One clean interaction first, so there is real history to recover.
            pipeline.submit(witnessedEvent(1L, FixedPoint.of(1)), liveCtx)
            val afterFirst = pipeline.published.stateHashHex()

            // Then die at the boundary under test.
            val died = runCatching {
                pipeline.submit(witnessedEvent(2L, FixedPoint.of(5)), liveCtx, boundary)
            }
            assertTrue(died.isFailure, "$boundary should have produced a simulated death")
            assertTrue(died.exceptionOrNull() is SimulatedProcessDeath, "$boundary: $died")

            // Recovery sees the durable medium and nothing else.
            val recovered = InteractionRecovery.recover(genesis, journal.rawBytes(), ctx())

            val durablyAcknowledgedSecond = journal.isAcknowledged(2L)
            if (durablyAcknowledgedSecond) {
                // The frame was acknowledged before the crash, so the material
                // transition is legitimately part of history.
                assertEquals(
                    FixedPoint.of(6),
                    recovered.snapshot.materialUnits,
                    "$boundary: an acknowledged frame must survive",
                )
            } else {
                assertEquals(
                    afterFirst,
                    recovered.snapshot.stateHashHex(),
                    "$boundary: an unacknowledged frame must leave no trace",
                )
            }

            // Never more than one final reaction for the interrupted event, and
            // recovery contributes none at all.
            assertTrue(
                pipeline.presentation.finalSemanticCountFor(2L) <= 1,
                "$boundary: final semantic presentation must be at most once",
            )
            assertEquals(
                0,
                recovered.finalPresentationsEmitted,
                "$boundary: recovery must never emit a delayed reaction",
            )
        }
    }

    @Test
    fun materialStateIsNeverPublishedBeforeDurableAcknowledgement() {
        val preAckBoundaries = listOf(
            CrashBoundary.BEFORE_CLASSIFICATION,
            CrashBoundary.AFTER_CANDIDATE_REDUCTION,
            CrashBoundary.BEFORE_RECEIPT_WRITE,
            CrashBoundary.DURING_RECEIPT_WRITE,
        )
        for (boundary in preAckBoundaries) {
            val genesis = CanonicalSnapshot.genesis(1L)
            val journal = DurableJournal()
            val pipeline = WitnessedInteractionPipeline(genesis, journal)

            runCatching { pipeline.submit(witnessedEvent(1L, FixedPoint.of(9)), ctx(), boundary) }

            assertEquals(
                0L,
                pipeline.published.materialUnits,
                "$boundary: the candidate must not have been published",
            )
            assertEquals(
                0,
                pipeline.presentation.countOf(PresentationKind.FINAL_SEMANTIC),
                "$boundary: no final semantic presentation may occur before acknowledgement",
            )
        }
    }

    @Test
    fun thePresentationSinkRefusesAnUngatedFinalReaction() {
        val journal = DurableJournal()
        val sink = GatedPresentationSink(journal)
        // Class E and reversible anticipation are always permitted.
        sink.ephemeralAcknowledgement(1L)
        sink.reversibleAnticipation(1L)
        // The final reaction is not, while nothing is acknowledged.
        assertFailsWith<IllegalStateException> { sink.finalSemantic(1L, commitSequence = 1L) }
    }

    @Test
    fun onlyReversibleAnticipationIsShownWhileDurabilityIsPending() {
        val genesis = CanonicalSnapshot.genesis(2L)
        val journal = DurableJournal()
        val pipeline = WitnessedInteractionPipeline(genesis, journal)

        runCatching {
            pipeline.submit(witnessedEvent(1L, FixedPoint.of(3)), ctx(), CrashBoundary.BEFORE_RECEIPT_WRITE)
        }

        val kinds = pipeline.presentation.log().map { it.kind }
        assertTrue(kinds.contains(PresentationKind.EPHEMERAL_ACKNOWLEDGEMENT))
        assertTrue(kinds.contains(PresentationKind.REVERSIBLE_ANTICIPATION))
        assertTrue(
            !kinds.contains(PresentationKind.FINAL_SEMANTIC),
            "no semantic claim may be made while the durable write is outstanding",
        )
    }

    @Test
    fun persistenceFailureResolvesThroughANeutralCancellation() {
        val genesis = CanonicalSnapshot.genesis(3L)
        val journal = DurableJournal()
        val pipeline = WitnessedInteractionPipeline(genesis, journal)

        runCatching {
            pipeline.submit(witnessedEvent(1L, FixedPoint.of(4)), ctx(), CrashBoundary.DURING_RECEIPT_WRITE)
        }

        assertEquals(
            1,
            pipeline.presentation.countOf(PresentationKind.NEUTRAL_CANCELLATION),
            "a failed durable write must resolve neutrally, not with a semantic claim",
        )
        assertEquals(0, journal.size, "an interrupted write must acknowledge nothing")
        assertEquals(1, journal.interruptedWriteCount)
    }

    @Test
    fun thePresentationTokenIsConsumedWhenTheWriteStartsNotWhenTheAnimationEnds() {
        val genesis = CanonicalSnapshot.genesis(4L)
        val journal = DurableJournal()
        val pipeline = WitnessedInteractionPipeline(genesis, journal)

        // Crash after acknowledgement but before the final presentation runs.
        runCatching {
            pipeline.submit(
                witnessedEvent(1L, FixedPoint.of(2)),
                ctx(),
                CrashBoundary.BEFORE_FINAL_SEMANTIC_PRESENTATION,
            )
        }

        val receipt = pipeline.spentReceipts().single()
        assertEquals(
            PresentationToken.CONSUMED_AT_START,
            receipt.presentationToken,
            "the token must already be spent, so recovery cannot re-fire the reaction",
        )
        assertTrue(!receipt.consumePresentationToken(), "a spent token must not be reusable")
        assertEquals(0, pipeline.presentation.countOf(PresentationKind.FINAL_SEMANTIC))
    }

    @Test
    fun theReceiptCarriesEverythingNeededToReapplyTheInteraction() {
        val genesis = CanonicalSnapshot.genesis(5L)
        val journal = DurableJournal()
        val pipeline = WitnessedInteractionPipeline(genesis, journal)
        val event = witnessedEvent(1L, FixedPoint.of(7))
        val preHash = genesis.stateHash()

        val outcome = pipeline.submit(event, ctx())
        val receipt = checkNotNull(outcome.receipt)

        assertEquals(event.logicalEventId, receipt.logicalEventId)
        assertEquals(CanonicalSnapshot.ENGINE_CONTRACT_VERSION, receipt.engineContractVersion)
        assertEquals(CanonicalEvent.EVENT_CONTRACT_VERSION, receipt.eventContractVersion)
        assertEquals(event, receipt.normalizedInput)
        assertTrue(preHash.contentEquals(receipt.preInputStateHash))
        assertEquals(outcome.committedSequence, receipt.replaySequence)

        // And it round-trips through canonical bytes.
        val decoded = MaterialInteractionReceipt.decode(receipt.canonicalBytes())
        assertEquals(receipt.digestHex(), decoded.digestHex())
        assertEquals(receipt.normalizedInput, decoded.normalizedInput)
    }

    @Test
    fun aCleanWitnessedInteractionEmitsExactlyOneFinalReaction() {
        val genesis = CanonicalSnapshot.genesis(6L)
        val journal = DurableJournal()
        val pipeline = WitnessedInteractionPipeline(genesis, journal)

        pipeline.submit(witnessedEvent(1L, FixedPoint.of(1)), ctx())

        assertEquals(1, pipeline.presentation.finalSemanticCountFor(1L))
        assertEquals(FixedPoint.of(1), pipeline.published.materialUnits)
        assertTrue(journal.isAcknowledged(1L))
    }
}
