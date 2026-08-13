package com.animusmachinae.dll17.core.continuity

import com.animusmachinae.dll17.core.crypto.ChaCha20Poly1305
import com.animusmachinae.dll17.core.math.ArithmeticContext
import com.animusmachinae.dll17.core.math.FixedPoint
import com.animusmachinae.dll17.core.state.PlatformPanicWitness
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Platform protection, version boundaries, identity binding, and the R002 scope
 * boundary.
 *
 * The scope tests at the end are as load-bearing as the behavioural ones. R002
 * is allowed to make continuity correct; it is not allowed to decide what the
 * organism is, and "we only added a small energy variable for the durability
 * test" is exactly how an unqualified assumption becomes permanent.
 */
class PlatformAndScopeTest {

    private val hour = 3_600_000L

    private fun ctx() = ArithmeticContext.unattributed()

    private class Fixture {
        val medium = InMemoryDurableMedium()
        val keys = InMemoryKeyContainer(1, 5L, ByteArray(ChaCha20Poly1305.KEY_SIZE) { it.toByte() })
        val store = EncryptedRecordStore(medium, keys, 5L)
        val gate = InteractionGate()
        val subsystems = SubsystemRegistry()
        var state = ContinuityState.genesis(5L, 5L)
        val journal = ContinuityJournal(store, SingleWriterActor(), 1L, state)
        val controller = PlatformProtectionController(journal, subsystems, gate)
    }

    // ------------------------------------------------------------ resource shed

    @Test
    fun `shedding follows the frozen order`() {
        val shed = ResourceShedController()
        assertTrue(shed.admitsOptionalLoad())
        shed.shedTo(3)
        assertEquals(listOf("particles", "post-processing", "shadows"), shed.shedSteps)
        assertFalse(shed.admitsOptionalLoad())
        shed.restoreFully()
        assertTrue(shed.admitsOptionalLoad())
        assertFailsWith<IllegalArgumentException> { shed.shedTo(99) }
    }

    // -------------------------------------------------------------- deep suspend

    @Test
    fun `a successful anchor makes the witness unnecessary`() {
        val fixture = Fixture()
        val outcome = fixture.controller.enterDeepSuspend(fixture.state, 3, ctx())
        assertTrue(outcome.anchorCommitted)
        assertFalse(outcome.witnessAttempted)
        assertFalse(fixture.controller.panicWitness.hasWritten)
        assertTrue(fixture.subsystems.runningSubsystems.isEmpty())
        assertEquals(Subsystem.entries.toList(), fixture.subsystems.releasedInOrder)
    }

    @Test
    fun `a failed anchor produces exactly one witness attempt and no retry`() {
        val fixture = Fixture()
        fixture.medium.failNextAppend = true
        val outcome = fixture.controller.enterDeepSuspend(fixture.state, 7, ctx())

        assertFalse(outcome.anchorCommitted)
        assertTrue(outcome.witnessAttempted)
        assertTrue(outcome.witnessWritten)
        assertEquals(1, outcome.steps.count { it == DeepSuspendSteps.ATTEMPT_ANCHOR })
        assertEquals(1, outcome.steps.count { it == DeepSuspendSteps.ATTEMPT_WITNESS })

        val record = fixture.controller.panicWitness.readDiagnostic()
        assertNotNull(record)
        assertEquals(7, record.reasonOrdinal)
    }

    @Test
    fun `both writes failing still leaves the last durable anchor authoritative`() {
        val fixture = Fixture()
        fixture.state = ContinuityReducer.reduce(
            fixture.state,
            ContinuityEvent.of(1L, ContinuityEventType.ACTIVE_EXPERIENCE_ADVANCED, a = 4L),
            ctx(),
        ).also { fixture.journal.append(ContinuityEvent.of(1L, ContinuityEventType.ACTIVE_EXPERIENCE_ADVANCED, a = 4L)) }
        val durable = ContinuityJournal.recover(
            fixture.store,
            ContinuityState.genesis(5L, 5L),
            ctx(),
        ).stateHashHex

        val spentWitness = PlatformPanicWitness(attemptDeadlineNanos = 1L)
        spentWitness.write(1, 0L, 0L)
        val controller =
            PlatformProtectionController(fixture.journal, fixture.subsystems, fixture.gate, spentWitness)

        fixture.medium.failNextAppend = true
        val outcome = controller.enterDeepSuspend(fixture.state, 9, ctx())
        assertFalse(outcome.anchorCommitted)
        assertFalse(outcome.witnessWritten)
        assertEquals(
            durable,
            ContinuityJournal.recover(fixture.store, ContinuityState.genesis(5L, 5L), ctx()).stateHashHex,
        )
    }

    @Test
    fun `the panic witness cannot alter canonical state`() {
        val fixture = Fixture()
        val before = fixture.state.stateHashHex()
        fixture.controller.panicWitness.write(4, 99L, 12345L)
        // The witness lives outside canonical state entirely; there is no field
        // for it to reach, which is the point.
        assertEquals(before, fixture.state.stateHashHex())
        assertEquals(PlatformPanicWitness.RECORD_SIZE, fixture.controller.panicWitness.rawBytes().size)
    }

    @Test
    fun `hysteresis refuses an early restart`() {
        val fixture = Fixture()
        val suspend = fixture.controller.enterDeepSuspend(fixture.state, 3, ctx())
        val observation = ClockObservation(hour, hour, true, 0L)
        assertNull(
            fixture.controller.recoverFromDeepSuspend(suspend.state, observation, 0L, ctx()),
        )
        assertNull(
            fixture.controller.recoverFromDeepSuspend(
                suspend.state,
                observation,
                ContinuityContract.THERMAL_REENTRY_HYSTERESIS_MILLIS - 1L,
                ctx(),
            ),
        )
        val recovered = fixture.controller.recoverFromDeepSuspend(
            suspend.state,
            observation,
            ContinuityContract.THERMAL_REENTRY_HYSTERESIS_MILLIS,
            ctx(),
        )
        assertNotNull(recovered)
        assertEquals(PlatformProtectionState.NORMAL, recovered.state.platformState)
        assertEquals(Subsystem.entries.size, fixture.subsystems.runningSubsystems.size)
    }

    @Test
    fun `deep suspend grants no biological credit`() {
        val fixture = Fixture()
        val before = fixture.state
        val outcome = fixture.controller.enterDeepSuspend(before, 3, ctx())
        assertEquals(before.developmentalProgress, outcome.state.developmentalProgress)
        assertEquals(before.activeExperienceTicks, outcome.state.activeExperienceTicks)
        assertEquals(before.reserveA, outcome.state.reserveA)
    }

    // -------------------------------------------------------- version boundary

    @Test
    fun `a legacy artifact migrates deterministically and idempotently`() {
        val legacy = ContinuityMigration.encodeLegacyV0(9L, 4L, 12_345L, FixedPoint.of(0, 600_000), 0L)
        val first = ContinuityMigration.migrateToCurrent(legacy)
        val second = ContinuityMigration.migrateToCurrent(legacy)
        assertTrue(first.canonicalBytes().contentEquals(second.canonicalBytes()))
        assertEquals(12_345L, first.wallClockAgeMillis)
        assertEquals(FixedPoint.of(0, 600_000), first.reserveA)
    }

    @Test
    fun `a future version is refused rather than guessed`() {
        val future = com.animusmachinae.dll17.core.crypto.CanonicalEnvelope.wrap(
            ContinuityState.SCHEMA_ID,
            ContinuityState.SCHEMA_VERSION + 1,
            ByteArray(0),
        )
        assertFailsWith<IllegalArgumentException> { ContinuityMigration.migrateToCurrent(future) }
    }

    @Test
    fun `decode never guesses across versions`() {
        val legacy = ContinuityMigration.encodeLegacyV0(9L, 4L, 0L, 0L, 0L)
        assertFailsWith<IllegalArgumentException> { ContinuityState.decode(legacy) }
    }

    // ------------------------------------------------------- identity binding

    @Test
    fun `a copied database does not boot as a second organism`() {
        val state = ContinuityState.genesis(5L, 5L)
        val foreign = InMemoryKeyContainer(1, 6L, ByteArray(ChaCha20Poly1305.KEY_SIZE))
        val check = DeviceBinding.check(state, foreign)
        assertFalse(check.admitted)
        assertEquals(QuarantineReason.DEVICE_FINGERPRINT_MISMATCH, check.reason)
    }

    @Test
    fun `a quarantined organism advances no clock at all`() {
        val quarantined = ContinuityReducer.reduce(
            ContinuityState.genesis(5L, 5L),
            DeviceBinding.quarantineEvent(ContinuityState.genesis(5L, 5L)),
            ctx(),
        )
        assertTrue(quarantined.identity.quarantined)
        assertFailsWith<IllegalStateException> {
            ContinuityReducer.reduce(
                quarantined,
                ContinuityEvent.of(9L, ContinuityEventType.VERIFIED_TIME_ADVANCED, a = hour),
                ctx(),
            )
        }
    }

    @Test
    fun `recovery freshness follows the contract thresholds`() {
        val state = ContinuityState.genesis(5L, 5L)
        assertEquals(RecoveryFreshness.FRESH, RecoveryPoint.freshness(state, 0L))
        assertEquals(
            RecoveryFreshness.STALE_WARNING,
            RecoveryPoint.freshness(state, ContinuityContract.RECOVERY_STALE_WARNING_MILLIS),
        )
        assertEquals(
            RecoveryFreshness.CRITICAL_WARNING,
            RecoveryPoint.freshness(state, ContinuityContract.RECOVERY_CRITICAL_WARNING_MILLIS),
        )
    }

    @Test
    fun `a receipt must match on all four fields to advance the recovery point`() {
        val receipt = ProviderReceipt("obj-1", 1_000L, "abcd", 7L)
        assertTrue(RecoveryPoint.receiptConfirms(receipt, "obj-1", 1_000L, "abcd", 7L))
        assertFalse(RecoveryPoint.receiptConfirms(receipt, "obj-2", 1_000L, "abcd", 7L))
        assertFalse(RecoveryPoint.receiptConfirms(receipt, "obj-1", 999L, "abcd", 7L))
        assertFalse(RecoveryPoint.receiptConfirms(receipt, "obj-1", 1_000L, "abce", 7L))
        assertFalse(RecoveryPoint.receiptConfirms(receipt, "obj-1", 1_000L, "abcd", 8L))
    }

    @Test
    fun `the blocked provider fails loudly instead of pretending`() {
        assertEquals(RecoveryScopeStatus.PROVIDER, BlockedRecoveryPackageStore.providerStatus)
        assertFailsWith<UnsupportedOperationException> {
            BlockedRecoveryPackageStore.upload(ByteArray(4), 1L)
        }
    }

    // ---------------------------------------------------------- scope boundary

    private fun continuitySources(): List<Path> =
        Files.walk(Path.of("src/main/kotlin")).use { stream ->
            stream.filter { it.extension == "kt" }.toList()
        }

    @Test
    fun `no R003 or later organism vocabulary appears in the continuity core`() {
        // R002 owns continuity. Physiology, drives, action selection, learning,
        // memory, relationships and development are gated behind A001, and a
        // "small" one introduced for a durability fixture would be load-bearing
        // before it was ever qualified.
        val prohibited = listOf(
            "hunger", "thirst", "fatigue", "affection", "mood", "emotion",
            "drive", "affordance", "torpor", "physiology", "personality",
            "relationship", "memoryTier", "episodic", "habituation", "evolution",
            "routeEvidence", "dialogue",
        )
        val sources = continuitySources()
        // A structural test that silently scanned nothing would pass forever.
        assertTrue(sources.size >= 10, "expected the continuity sources, found ${sources.size}")

        val violations = mutableListOf<String>()
        for (path in sources) {
            val text = path.readText()
            for (line in text.lines().withIndex()) {
                // Prose in comments legitimately names the excluded subsystems,
                // so only declarations are checked.
                val trimmed = line.value.trim()
                if (trimmed.startsWith("*") || trimmed.startsWith("//")) continue
                for (word in prohibited) {
                    if (trimmed.contains(word, ignoreCase = true)) {
                        violations += "${path.fileName}:${line.index + 1} mentions '$word'"
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(), "R002 introduced organism vocabulary:\n${violations.joinToString("\n")}")
    }

    @Test
    fun `the neutral reserves carry no meaning beyond a bounded fraction`() {
        val state = ContinuityState.genesis(5L, 5L)
        assertEquals(FixedPoint.ONE, state.reserveA)
        assertEquals(FixedPoint.ONE, state.reserveB)
        assertFailsWith<IllegalArgumentException> { state.copy(reserveA = FixedPoint.ONE + 1L) }
        assertFailsWith<IllegalArgumentException> { state.copy(reserveB = -1L) }
    }

    @Test
    fun `continuity state round trips through canonical bytes`() {
        val state = ContinuityState.genesis(5L, 5L).copy(
            wallClockAgeMillis = 1_234_567L,
            reserveA = FixedPoint.of(0, 123_456),
            debt = DebtLedgerState.genesis().copy(
                state = DebtState.PAUSED_LOW_RESERVE,
                outstandingBaselineEquivMillis = 999L,
                abundanceStablePresent = true,
                abundanceStableSinceVerifiedMillis = 42L,
            ),
            admissionState = DurabilityAdmissionState.PRESSURE,
            presentationState = DurabilityPresentationState.TEMPORAL_DESYNC,
            platformState = PlatformProtectionState.RESOURCE_SHED,
            gapProvenance = GapProvenance.LIKELY_PLATFORM_FORCED_SUSPEND,
        )
        val decoded = ContinuityState.decode(state.canonicalBytes())
        assertTrue(state.canonicalBytes().contentEquals(decoded.canonicalBytes()))
        assertEquals(state.stateHashHex(), decoded.stateHashHex())
    }
}
