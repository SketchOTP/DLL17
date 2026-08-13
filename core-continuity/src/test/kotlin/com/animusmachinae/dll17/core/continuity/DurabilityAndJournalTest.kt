package com.animusmachinae.dll17.core.continuity

import com.animusmachinae.dll17.core.crypto.ChaCha20Poly1305
import com.animusmachinae.dll17.core.math.ArithmeticContext
import com.animusmachinae.dll17.core.state.DurabilityClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Journal generations, the single writer, batching, compaction, admission,
 * safe-hold orderings and the encrypted-record boundary.
 *
 * The recurring theme: every rule is about *ordering*, and the tests assert the
 * order rather than only the outcome. An implementation free to reorder these
 * steps is free to reintroduce the exploit each one closes.
 */
class DurabilityAndJournalTest {

    private val organismId = 77L
    private val fingerprint = 0x1234L

    private fun ctx() = ArithmeticContext.unattributed()
    private fun key() = ByteArray(ChaCha20Poly1305.KEY_SIZE) { (it + 1).toByte() }

    private class Fixture(capacity: Long = ContinuityContract.JOURNAL_BYTE_BUDGET) {
        val medium = InMemoryDurableMedium(capacity)
        val keys = InMemoryKeyContainer(1, 0x1234L, ByteArray(ChaCha20Poly1305.KEY_SIZE) { (it + 1).toByte() })
        val store = EncryptedRecordStore(medium, keys, 77L)
        val gate = InteractionGate()
        var state = ContinuityState.genesis(77L, 0x1234L)
        val journal = ContinuityJournal(store, SingleWriterActor(), 1L, state)

        fun apply(event: ContinuityEvent, ctx: ArithmeticContext) {
            journal.append(event)
            state = ContinuityReducer.reduce(state, event, ctx)
        }

        fun tick(index: Long, ctx: ArithmeticContext) = apply(
            ContinuityEvent.of(
                state.lastCommitSequence + 1L,
                ContinuityEventType.ACTIVE_EXPERIENCE_ADVANCED,
                a = index,
            ),
            ctx,
        )
    }

    // ------------------------------------------------------------ writer actor

    @Test
    fun `the writer refuses to be reentered`() {
        val writer = SingleWriterActor()
        assertFailsWith<IllegalStateException> {
            writer.write { writer.write { 1 } }
        }
    }

    @Test
    fun `a batch is written under one writer entry`() {
        val fixture = Fixture()
        val before = fixture.journal.writeCount
        fixture.journal.appendBatch(
            (1..5).map {
                ContinuityEvent.of(it.toLong(), ContinuityEventType.ACTIVE_EXPERIENCE_ADVANCED, a = 1L)
            },
        )
        assertEquals(before + 1, fixture.journal.writeCount)
    }

    // ---------------------------------------------------------------- batching

    @Test
    fun `ephemeral transitions never reach the journal`() {
        val batcher = CommitBatcher()
        val ripple = ContinuityEvent.of(
            1L,
            ContinuityEventType.ACTIVE_EXPERIENCE_ADVANCED,
            durability = DurabilityClass.EPHEMERAL,
        )
        assertTrue(batcher.offer(ripple, 0L).isEmpty())
        assertEquals(0, batcher.pendingCount)
        assertTrue(batcher.flush().isEmpty())
    }

    @Test
    fun `ordinary transitions batch until the cadence and witnessed ones do not wait`() {
        val batcher = CommitBatcher()
        val ordinary = ContinuityEvent.of(1L, ContinuityEventType.ACTIVE_EXPERIENCE_ADVANCED)
        assertTrue(batcher.offer(ordinary, 0L).isEmpty())
        assertTrue(batcher.offer(ordinary, 100L).isEmpty())
        val flushed = batcher.offer(ordinary, ContinuityContract.CLASS_O_COMMIT_CADENCE_MILLIS)
        assertEquals(3, flushed.size)

        val witnessed = ContinuityEvent.of(
            9L,
            ContinuityEventType.DURABILITY_SAFE_HOLD_ENTERED,
            durability = DurabilityClass.WITNESSED,
        )
        batcher.offer(ordinary, 0L)
        val immediate = batcher.offer(witnessed, 1L)
        assertEquals(2, immediate.size)
        assertEquals(0, batcher.pendingCount)
    }

    @Test
    fun `nothing stays uncommitted beyond the maximum window`() {
        val batcher = CommitBatcher()
        val ordinary = ContinuityEvent.of(1L, ContinuityEventType.ACTIVE_EXPERIENCE_ADVANCED)
        batcher.offer(ordinary, 0L)
        val flushed = batcher.offer(ordinary, ContinuityContract.CLASS_O_MAX_UNCOMMITTED_WINDOW_MILLIS)
        assertTrue(flushed.isNotEmpty())
    }

    // -------------------------------------------------------------- generation

    @Test
    fun `generation IDs are immutable and increasing`() {
        val fixture = Fixture()
        assertEquals(1L, fixture.journal.generationId)
        val flipped = fixture.journal.flipGeneration()
        assertEquals(2L, flipped)
        assertEquals(1L, fixture.journal.sealedGenerationId)

        assertFailsWith<IllegalArgumentException> {
            ContinuityReducer.reduce(
                fixture.state.copy(generationId = 5L),
                ContinuityEvent.of(1L, ContinuityEventType.GENERATION_FLIPPED, a = 4L),
                ctx(),
            )
        }
    }

    // ------------------------------------------------------- recovery and tail

    @Test
    fun `an acknowledged event survives process death exactly`() {
        val fixture = Fixture()
        fixture.tick(1L, ctx())
        fixture.tick(2L, ctx())
        val expected = fixture.state.stateHashHex()

        val recovered = ContinuityJournal.recover(
            fixture.store,
            ContinuityState.genesis(organismId, fingerprint),
            ctx(),
        )
        assertEquals(expected, recovered.stateHashHex)
        assertEquals(2, recovered.eventsReplayed)
    }

    @Test
    fun `a torn tail is invisible to recovery`() {
        val fixture = Fixture()
        fixture.tick(1L, ctx())
        val expected = fixture.state.stateHashHex()

        fixture.medium.interruptNextAppend = true
        assertFailsWith<InterruptedWrite> {
            fixture.journal.append(
                ContinuityEvent.of(99L, ContinuityEventType.ACTIVE_EXPERIENCE_ADVANCED, a = 5L),
            )
        }
        val recovered = ContinuityJournal.recover(
            fixture.store,
            ContinuityState.genesis(organismId, fingerprint),
            ctx(),
        )
        assertEquals(expected, recovered.stateHashHex)
        assertEquals(1, recovered.eventsReplayed)
    }

    @Test
    fun `corruption before the tail is a storage fault rather than a skip`() {
        val fixture = Fixture()
        repeat(4) { fixture.tick(it.toLong() + 1L, ctx()) }
        // Skipping an unreadable record in the middle would let recovery invent
        // a history that never happened.
        fixture.medium.corrupt(2L, 40)
        assertFailsWith<StorageFault> { fixture.store.readAll() }
    }

    @Test
    fun `a failed write consumes its sequence so no nonce is ever reused`() {
        val fixture = Fixture()
        fixture.tick(1L, ctx())
        val before = fixture.journal.lastSequence
        fixture.medium.failNextAppend = true
        assertFailsWith<StorageFault> {
            fixture.journal.append(
                ContinuityEvent.of(5L, ContinuityEventType.ACTIVE_EXPERIENCE_ADVANCED, a = 1L),
            )
        }
        assertEquals(before + 1L, fixture.journal.lastSequence)
        assertFalse(fixture.journal.isAcknowledged(before + 1L))
    }

    // -------------------------------------------------------------- compaction

    @Test
    fun `compaction verifies before it prunes`() {
        val fixture = Fixture()
        repeat(6) { fixture.tick(it.toLong() + 1L, ctx()) }
        val expected = fixture.state.stateHashHex()
        fixture.journal.flipGeneration()

        val result = fixture.journal.compact(ctx())
        assertTrue(result.installed)
        assertNotNull(result.checkpoint)
        assertTrue(result.checkpoint!!.verifySelf())

        val recovered = ContinuityJournal.recover(
            fixture.store,
            ContinuityState.genesis(organismId, fingerprint),
            ctx(),
        )
        assertTrue(recovered.checkpointPresent)
        assertEquals(expected, recovered.stateHashHex)
    }

    @Test
    fun `a failed checkpoint install prunes nothing`() {
        val fixture = Fixture()
        repeat(6) { fixture.tick(it.toLong() + 1L, ctx()) }
        val expected = fixture.state.stateHashHex()
        val before = fixture.journal.events().size
        fixture.journal.flipGeneration()

        fixture.medium.failNextAppend = true
        val result = fixture.journal.compact(ctx())
        assertFalse(result.installed)
        assertEquals(before, fixture.journal.events().size)
        assertNull(fixture.journal.installedCheckpoint)
        assertEquals(
            expected,
            ContinuityJournal.recover(
                fixture.store,
                ContinuityState.genesis(organismId, fingerprint),
                ctx(),
            ).stateHashHex,
        )
    }

    @Test
    fun `compaction with no sealed generation does nothing`() {
        val fixture = Fixture()
        fixture.tick(1L, ctx())
        val result = fixture.journal.compact(ctx())
        assertFalse(result.installed)
    }

    // --------------------------------------------------------------- admission

    @Test
    fun `admission thresholds follow the contract`() {
        val fixture = Fixture()
        val controller = DurabilityAdmissionController(fixture.journal, fixture.store, fixture.gate)
        assertEquals(DurabilityAdmissionState.OPEN, controller.evaluate(fixture.state))

        fixture.medium.simulateAdditionalUsage(ContinuityContract.JOURNAL_SOFT_FLIP_BYTES)
        assertEquals(DurabilityAdmissionState.PRESSURE, controller.evaluate(fixture.state))

        fixture.medium.simulateAdditionalUsage(
            ContinuityContract.JOURNAL_BYTE_BUDGET -
                ContinuityContract.JOURNAL_SOFT_FLIP_BYTES -
                ContinuityContract.EMERGENCY_DURABILITY_RESERVE_BYTES,
        )
        assertEquals(
            DurabilityAdmissionState.READ_ONLY_SURVIVAL,
            controller.evaluate(fixture.state),
        )
    }

    @Test
    fun `a storage fault is sticky until a self-test succeeds`() {
        val fixture = Fixture()
        val controller = DurabilityAdmissionController(fixture.journal, fixture.store, fixture.gate)
        val faulted = fixture.state.copy(admissionState = DurabilityAdmissionState.STORAGE_FAULT)
        assertEquals(DurabilityAdmissionState.STORAGE_FAULT, controller.evaluate(faulted))
    }

    @Test
    fun `safe hold entry and exit follow the normative orderings`() {
        val fixture = Fixture()
        val controller = DurabilityAdmissionController(fixture.journal, fixture.store, fixture.gate)
        val entry = controller.enterSafeHold(fixture.state, ctx())
        assertEquals(SafeHoldSteps.ENTRY_ORDER, entry.steps)
        assertTrue(entry.state.safeHoldActive)
        assertEquals(DurabilityPresentationState.TEMPORAL_DESYNC, entry.state.presentationState)

        val exit = controller.exitSafeHold(
            entry.state,
            ClockObservation(2_000_000L, 2_000_000L, true, 1L),
            ctx(),
        )
        assertEquals(SafeHoldSteps.EXIT_ORDER, exit.steps)
        assertFalse(exit.state.safeHoldActive)
        assertEquals(DurabilityAdmissionState.OPEN, exit.state.admissionState)
        assertTrue(fixture.gate.isOpen)
    }

    @Test
    fun `a failed emergency commit falls back to the last durable anchor`() {
        val fixture = Fixture()
        fixture.tick(1L, ctx())
        val durable = fixture.state.stateHashHex()
        val controller = DurabilityAdmissionController(fixture.journal, fixture.store, fixture.gate)

        fixture.medium.failNextAppend = true
        val entry = controller.enterSafeHold(fixture.state, ctx())
        assertFalse(entry.succeeded)
        assertEquals(SafeHoldSteps.ENTER_STORAGE_FAULT, entry.steps.last())
        assertEquals(DurabilityAdmissionState.STORAGE_FAULT, entry.state.admissionState)
        assertEquals(
            durable,
            ContinuityJournal.recover(
                fixture.store,
                ContinuityState.genesis(organismId, fingerprint),
                ctx(),
            ).stateHashHex,
        )
    }

    @Test
    fun `a hold does not skip elapsed time`() {
        val fixture = Fixture()
        val controller = DurabilityAdmissionController(fixture.journal, fixture.store, fixture.gate)
        val entry = controller.enterSafeHold(fixture.state, ctx())
        val exit = controller.exitSafeHold(
            entry.state,
            ClockObservation(48L * 3_600_000L, 48L * 3_600_000L, true, 0L),
            ctx(),
        )
        // With no blind credit standing, an unverifiable held interval grants no
        // biology at all — but it is still *accounted for*, as debt, rather than
        // vanishing. A hold that simply skipped the interval would leave nothing
        // here.
        assertTrue(exit.state.debt.outstandingBaselineEquivMillis > 0L)
    }

    // ------------------------------------------------------------ presentation

    @Test
    fun `autonomous life is refused during a durability hold`() {
        val presentation = DurabilityPresentation(DurabilityPresentationState.TEMPORAL_DESYNC)
        presentation.showContactInterrupted()
        presentation.showLastDurableStatus(3L * 86_400_000L)
        assertFailsWith<IllegalStateException> { presentation.showAutonomousLife("exploring") }
    }

    @Test
    fun `prohibited durability vocabulary is rejected`() {
        for (word in DurabilityPresentation.PROHIBITED_VOCABULARY) {
            assertFalse(
                DurabilityPresentation.copyIsHonest("The organism is in $word until storage returns"),
                "'$word' must not appear in durability-hold copy",
            )
        }
        assertTrue(
            DurabilityPresentation.copyIsHonest(
                "Contact interrupted. The organism's biological time may still be passing. " +
                    "Restore storage to re-establish a safe connection.",
            ),
        )
    }

    @Test
    fun `the retired stasis ordinal is never reused`() {
        assertTrue(
            DurabilityPresentationState.entries.none {
                it.ordinal32 ==
                    DurabilityPresentationState.RETIRED_STASIS_PROJECTION_ORDINAL
            },
        )
        assertFailsWith<IllegalArgumentException> {
            DurabilityPresentationState.fromOrdinal(
                DurabilityPresentationState.RETIRED_STASIS_PROJECTION_ORDINAL,
            )
        }
    }

    // ------------------------------------------------------- encrypted records

    @Test
    fun `no canonical plaintext reaches the medium`() {
        val fixture = Fixture()
        repeat(3) { fixture.tick(it.toLong() + 1L, ctx()) }
        val marker = ContinuityEvent.of(
            fixture.state.lastCommitSequence + 1L,
            ContinuityEventType.ACTIVE_EXPERIENCE_ADVANCED,
            a = 0x0BADCAFEL,
        ).canonicalPayloadBytes()
        fixture.apply(ContinuityEvent.decodePayload(marker), ctx())

        for ((_, record) in fixture.medium.records()) {
            assertFalse(
                record.toList().windowed(marker.size).any { it.toByteArray().contentEquals(marker) },
                "canonical plaintext appeared on the durable medium",
            )
        }
    }

    @Test
    fun `a record moved to another sequence fails authentication`() {
        val medium = InMemoryDurableMedium()
        val keys = InMemoryKeyContainer(1, fingerprint, key())
        val store = EncryptedRecordStore(medium, keys, organismId)
        store.append(1L, 1L, ContinuityEvent.SCHEMA_ID, 1, "payload-one".toByteArray())

        val moved = medium.records().single().second
        val other = InMemoryDurableMedium()
        other.append(2L, moved)
        val otherStore = EncryptedRecordStore(other, keys, organismId)
        // The AAD binds the record to its position, so relocation is detected
        // rather than silently accepted.
        assertFailsWith<StorageFault> { otherStore.readAll() }
    }

    @Test
    fun `a record from another key epoch is refused rather than guessed`() {
        val medium = InMemoryDurableMedium()
        val store = EncryptedRecordStore(medium, InMemoryKeyContainer(1, fingerprint, key()), organismId)
        store.append(1L, 1L, ContinuityEvent.SCHEMA_ID, 1, "payload".toByteArray())

        val laterEpoch = EncryptedRecordStore(
            medium,
            InMemoryKeyContainer(2, fingerprint, key()),
            organismId,
        )
        assertFailsWith<StorageFault> { laterEpoch.readAll() }
    }

    @Test
    fun `a different organism cannot read the records`() {
        val medium = InMemoryDurableMedium()
        val keys = InMemoryKeyContainer(1, fingerprint, key())
        EncryptedRecordStore(medium, keys, organismId)
            .append(1L, 1L, ContinuityEvent.SCHEMA_ID, 1, "payload".toByteArray())
        assertFailsWith<StorageFault> {
            EncryptedRecordStore(medium, keys, organismId + 1L).readAll()
        }
    }
}
