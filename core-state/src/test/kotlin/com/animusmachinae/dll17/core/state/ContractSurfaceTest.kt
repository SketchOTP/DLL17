package com.animusmachinae.dll17.core.state

import com.animusmachinae.dll17.core.crypto.CanonicalCodecException
import com.animusmachinae.dll17.core.crypto.RandomDomainRegistry
import com.animusmachinae.dll17.core.crypto.RandomSubstream
import com.animusmachinae.dll17.core.math.ArithmeticContext
import com.animusmachinae.dll17.core.math.FixedPoint
import com.animusmachinae.dll17.core.math.GeneratedLookupTables
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Migration, panic witness, assisted-payload physics, lookup tables and the
 * qualification kernel — the remaining R001 work packages.
 */
class ContractSurfaceTest {

    private fun ctx() = ArithmeticContext.unattributed()

    // ------------------------------------------------------------- R001 migration

    @Test
    fun aLegacyVersionZeroSnapshotMigratesDeterministically() {
        val legacy = SnapshotMigration.encodeLegacyV0(
            logicalTime = 4_000L,
            masterSeed = 0xABCDEFL,
            numericA = FixedPoint.of(12, 500_000),
        )

        val migrated = SnapshotMigration.migrateToCurrent(legacy)

        assertEquals(CanonicalSnapshot.SCHEMA_VERSION, migrated.schemaVersion)
        assertEquals(4_000L, migrated.logicalTime)
        assertEquals(FixedPoint.of(12, 500_000), migrated.numericA)
        assertEquals(0L, migrated.numericB, "a field the old schema lacked gets its documented initial value")
        assertEquals(0L, migrated.materialUnits)

        // Running the migration twice must produce identical bytes.
        val again = SnapshotMigration.migrateToCurrent(legacy)
        assertTrue(migrated.canonicalBytes().contentEquals(again.canonicalBytes()))
    }

    @Test
    fun migrationDerivesNewSubstreamsWithoutAdvancingAnything() {
        val masterSeed = 0xABCDEFL
        val migrated = SnapshotMigration.migrateToCurrent(
            SnapshotMigration.encodeLegacyV0(0L, masterSeed, 0L),
        )
        for (stream in migrated.substreams) {
            assertEquals(
                0L,
                stream.counter,
                "a migration must not consume randomness; domain ${stream.domainId} was advanced",
            )
            assertEquals(
                RandomSubstream.derive(masterSeed, stream.domainId).seed,
                stream.seed,
                "substreams must be derived from the existing master seed, not reseeded",
            )
        }
    }

    @Test
    fun aFutureSchemaVersionIsRefusedRatherThanGuessedAt() {
        val future = SnapshotMigration.encodeFutureVersion(CanonicalSnapshot.SCHEMA_VERSION + 1)
        val failure = assertFailsWith<IllegalArgumentException> {
            SnapshotMigration.migrateToCurrent(future)
        }
        assertTrue(failure.message!!.contains("newer than this build"))
    }

    @Test
    fun decodingAcrossVersionsWithoutMigratingIsRefused() {
        val legacy = SnapshotMigration.encodeLegacyV0(0L, 1L, 0L)
        assertFailsWith<IllegalArgumentException> { CanonicalSnapshot.decode(legacy) }
    }

    @Test
    fun anArtifactFromAnotherContractVersionIsRefused() {
        val snapshot = CanonicalSnapshot.genesis(1L).canonicalBytes()
        // Bump the determinism contract version inside the envelope header.
        snapshot[9] = (snapshot[9] + 1).toByte()
        assertFailsWith<CanonicalCodecException> { CanonicalSnapshot.decode(snapshot) }
    }

    // ---------------------------------------------------------- R001.7 panic witness

    @Test
    fun thePanicWitnessWritesAtMostOncePerAttemptAndIsFixedSize() {
        val witness = PlatformPanicWitness(attemptDeadlineNanos = 2_000_000L)
        assertTrue(!witness.hasWritten)

        assertTrue(witness.write(reasonOrdinal = 3, lastDurableSequence = 17L, monotonicNanos = 99L))
        assertTrue(!witness.write(reasonOrdinal = 4, lastDurableSequence = 18L, monotonicNanos = 100L))

        assertEquals(PlatformPanicWitness.RECORD_SIZE, witness.rawBytes().size)
        val record = checkNotNull(witness.readDiagnostic())
        assertEquals(3, record.reasonOrdinal)
        assertEquals(17L, record.lastDurableSequence, "the second write must not have overwritten the first")
    }

    @Test
    fun thePanicWitnessCannotReachCanonicalStateOrTheStateHash() {
        val genesis = CanonicalSnapshot.genesis(1L)
        val before = genesis.stateHashHex()

        val witness = PlatformPanicWitness(attemptDeadlineNanos = 1L)
        witness.write(reasonOrdinal = 1, lastDurableSequence = 5L, monotonicNanos = 5L)

        assertEquals(before, CanonicalSnapshot.genesis(1L).stateHashHex())
        // Structural: the witness bytes must not appear anywhere in canonical bytes.
        val canonical = genesis.canonicalBytes()
        val witnessBytes = witness.rawBytes()
        assertTrue(
            !containsSubsequence(canonical, witnessBytes),
            "panic witness content must be excluded from canonical serialization",
        )
    }

    @Test
    fun aLostPanicWitnessChangesNothing() {
        // "Safe to lose entirely" means replay must not consult it. Two replays,
        // one with a witness written and one without, must agree exactly.
        val genesis = CanonicalSnapshot.genesis(8L)
        val events = listOf(
            CanonicalEvent(1L, CanonicalEventType.APPLY_DELTA, FixedPoint.of(2), 0L, DurabilityClass.ORDINARY),
        )

        val withoutWitness = CanonicalReducer.reduceAll(genesis, events, ctx()).stateHashHex()

        PlatformPanicWitness(1L).write(1, 1L, 1L)
        val withWitness = CanonicalReducer.reduceAll(genesis, events, ctx()).stateHashHex()

        assertEquals(withoutWitness, withWitness)
    }

    private fun containsSubsequence(haystack: ByteArray, needle: ByteArray): Boolean {
        if (needle.isEmpty() || needle.size > haystack.size) return false
        outer@ for (start in 0..haystack.size - needle.size) {
            for (offset in needle.indices) {
                if (haystack[start + offset] != needle[offset]) continue@outer
            }
            return true
        }
        return false
    }

    // ------------------------------------------------- R001.8 assisted payload physics

    @Test
    fun assistedPayloadsCannotParticipateInPhysics() {
        val payload = QualificationAssistedPayload(canonicalDoseEffect = FixedPoint.of(1))
        assertTrue(!payload.dynamicRigidBody)
        assertTrue(!payload.collisionParticipation)
        assertTrue(payload.kinematicPresentationProxy, "a noncolliding kinematic proxy is permitted")
    }

    @Test
    fun theZeroPhysicsInvariantIsEnforcedByTheTypeSystem() {
        // The properties are declared final in AssistedPayload, so a subclass
        // cannot override them. This asserts the structural fact rather than
        // trusting the declaration, because the declaration is the control.
        for (name in listOf("getDynamicRigidBody", "getCollisionParticipation")) {
            val method = AssistedPayload::class.java.getDeclaredMethod(name)
            assertTrue(
                java.lang.reflect.Modifier.isFinal(method.modifiers),
                "$name must be final so no payload can opt into the physics solver",
            )
        }
    }

    @Test
    fun theCanonicalDoseEffectIsDataNotDerivedFromRenderState() {
        val payload = QualificationAssistedPayload(canonicalDoseEffect = FixedPoint.of(3, 250_000))
        repeat(10) {
            assertEquals(FixedPoint.of(3, 250_000), payload.canonicalDoseEffect)
        }
    }

    // ------------------------------------------------------------- lookup tables

    @Test
    fun everyGeneratedLookupTableVerifiesAgainstItsEmbeddedDigest() {
        assertTrue(GeneratedLookupTables.ALL.isNotEmpty())
        for (table in GeneratedLookupTables.ALL) {
            table.verify()
            assertEquals(table.expectedDigestHex, table.computedDigestHex())
        }
    }

    @Test
    fun theUnitRampHasTheGeneratedShape() {
        val ramp = GeneratedLookupTables.UNIT_RAMP
        assertEquals(257, ramp.size)
        assertEquals(0L, ramp.valueAt(0))
        assertEquals(FixedPoint.ONE, ramp.valueAt(256))
        assertEquals(500_000L, ramp.valueAt(128))
        // Monotonic non-decreasing.
        for (index in 1 until ramp.size) {
            assertTrue(ramp.valueAt(index) >= ramp.valueAt(index - 1), "ramp regressed at $index")
        }
        assertFailsWith<IndexOutOfBoundsException> { ramp.valueAt(257) }
    }

    @Test
    fun aCorruptedTableFailsLoudly() {
        val corrupted = com.animusmachinae.dll17.core.math.LookupTable(
            tableId = "LOOKUP_UNIT_RAMP_V1",
            tableVersion = 1,
            expectedDigestHex = GeneratedLookupTables.UNIT_RAMP.expectedDigestHex,
            values = longArrayOf(0L, 1L, 2L),
        )
        assertFailsWith<IllegalStateException> { corrupted.verify() }
    }

    // ------------------------------------------------------ qualification kernel

    @Test
    fun theQualificationKernelIsStableAcrossRepeatedRuns() {
        val first = R001QualificationKernel.run()
        val second = R001QualificationKernel.run()
        assertEquals(
            first.evidenceDigestHex,
            second.evidenceDigestHex,
            "the kernel must be byte-stable against itself before it can be compared across targets",
        )
        assertTrue(first.allReplaysMatch)
        assertTrue(first.domainIsolationHolds)
        assertEquals(64, first.evidenceDigestHex.length)
    }

    @Test
    fun theKernelReproducesTheFrozenGoldenDigest() {
        // The cross-target claim, asserted on whatever target is running this.
        assertEquals(
            R001QualificationKernel.GOLDEN_EVIDENCE_DIGEST,
            R001QualificationKernel.run().evidenceDigestHex,
            "this target does not reproduce the frozen R001 evidence digest",
        )
    }

    @Test
    fun theKernelExercisesEveryRegisteredRandomDomain() {
        // The late-insert domain is intentionally not consumed by fixtures; the
        // other two must be, or the isolation proof has nothing to isolate.
        val consumed = R001QualificationKernel.fixtures()
            .flatMap { it.events }
            .filter { it.type == CanonicalEventType.DRAW_RANDOM }
            .map { it.operandA.toInt() }
            .toSet()
        assertTrue(RandomDomainRegistry.DOMAIN_QUALIFICATION_PRIMARY in consumed)
        assertTrue(RandomDomainRegistry.DOMAIN_QUALIFICATION_SECONDARY in consumed)
    }

    @Test
    fun everyDurabilityClassIsRepresentedInTheFixtures() {
        val classes = R001QualificationKernel.fixtures()
            .flatMap { it.events }
            .map { it.durabilityClass }
            .toSet()
        assertTrue(DurabilityClass.WITNESSED in classes, "Class W must be exercised")
        assertTrue(DurabilityClass.ORDINARY in classes, "Class O must be exercised")
    }

    @Test
    fun noOrganismStateExistsInTheCanonicalSnapshot() {
        // R001 is a determinism kernel. If a physiological field ever appears
        // here without the A001 gate, this test is the tripwire.
        val forbidden = listOf(
            "hunger", "thirst", "fatigue", "affect", "mood", "drive", "bond",
            "trust", "memory", "episode", "relationship", "development", "torpor",
            "personality", "habit", "fear", "learning",
        )
        val fields = CanonicalSnapshot::class.java.declaredFields.map { it.name.lowercase() }
        for (name in forbidden) {
            assertTrue(
                fields.none { it.contains(name) },
                "canonical snapshot contains organism field matching '$name': $fields",
            )
        }
    }
}
