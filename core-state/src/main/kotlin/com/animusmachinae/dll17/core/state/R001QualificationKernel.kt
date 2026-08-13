package com.animusmachinae.dll17.core.state

import com.animusmachinae.dll17.core.crypto.CanonicalEnvelope
import com.animusmachinae.dll17.core.crypto.CanonicalHash
import com.animusmachinae.dll17.core.crypto.CanonicalWriter
import com.animusmachinae.dll17.core.crypto.RandomDomainRegistry
import com.animusmachinae.dll17.core.crypto.RandomSubstream
import com.animusmachinae.dll17.core.math.ArithmeticContext
import com.animusmachinae.dll17.core.math.ExecutionMode
import com.animusmachinae.dll17.core.math.FixedPoint
import com.animusmachinae.dll17.core.math.GeneratedLookupTables
import com.animusmachinae.dll17.core.math.RecordingSaturationObserver

/**
 * The R001 cross-target qualification kernel.
 *
 * This is the artifact the determinism matrix is actually about. It runs
 * byte-identically on a desktop JVM, on Tensor hardware and on the x86 Android
 * emulator, and it reduces everything it did to one digest. If that digest
 * differs between two targets, R001 has failed, and the per-section digests
 * narrow down which frozen decision leaked a platform dependency.
 *
 * It reads no clock, no file, no environment variable and no device property.
 * Everything it consumes is a compiled-in constant, which is what makes the
 * digest comparable at all.
 */
public object R001QualificationKernel {

    public const val FIXTURE_SET_ID: String = "R001-FIXTURES-V1"

    /**
     * The frozen cross-target golden digest.
     *
     * This single constant is what the determinism matrix actually asserts.
     * Because it is compiled into the shared core, the same assertion runs on the
     * desktop JVM, on Tensor hardware and on the x86 emulator, and any target
     * that computes something else fails on that target rather than quietly
     * producing a different organism.
     *
     * Changing this value is a determinism change. It is legitimate only
     * alongside a contract version bump and regenerated golden vectors; editing
     * it to make a failing target pass would be falsifying the qualification.
     */
    public const val GOLDEN_EVIDENCE_DIGEST: String =
        "54bc044740a4c05b41b509a7160bff559e09421f2eaa55dc36c3d3ffadc1bd86"
    public const val FIXTURE_SET_VERSION: Int = 1
    public const val EVIDENCE_SCHEMA_ID: Int = 301

    /** One replay fixture: a master seed and an ordered event sequence. */
    public class Fixture(
        public val id: String,
        public val masterSeed: Long,
        public val events: List<CanonicalEvent>,
    )

    /** Per-fixture result. */
    public class FixtureResult(
        public val id: String,
        public val directStateHashHex: String,
        public val replayStateHashHex: String,
        public val framesApplied: Int,
        public val journalDigestHex: String,
        public val saturationCount: Int,
    ) {
        /** `reduce == replay`, byte for byte, is the R001 gate criterion. */
        public val replayMatches: Boolean get() = directStateHashHex == replayStateHashHex
    }

    /** Full evidence produced by one run of the kernel. */
    public class Report(
        public val fixtureResults: List<FixtureResult>,
        public val fixedPointDigestHex: String,
        public val randomDigestHex: String,
        public val lookupDigestHex: String,
        public val serializationDigestHex: String,
        public val domainIsolationHolds: Boolean,
        public val evidenceDigestHex: String,
        public val totalSaturations: Int,
    ) {
        public val allReplaysMatch: Boolean get() = fixtureResults.all { it.replayMatches }
    }

    // ------------------------------------------------------------------ fixtures

    private fun event(
        id: Long,
        type: CanonicalEventType,
        a: Long,
        b: Long = 0L,
        durability: DurabilityClass = DurabilityClass.ORDINARY,
    ): CanonicalEvent = CanonicalEvent(id, type, a, b, durability)

    public fun fixtures(): List<Fixture> = listOf(
        Fixture(
            id = "FX-ARITHMETIC-01",
            masterSeed = 0x0123456789ABCDEFL,
            events = listOf(
                event(1, CanonicalEventType.ADVANCE_TIME, 1_000L),
                event(2, CanonicalEventType.APPLY_DELTA, FixedPoint.of(3, 250_000)),
                event(3, CanonicalEventType.APPLY_DELTA, FixedPoint.of(-1, 125_000)),
                event(4, CanonicalEventType.APPLY_INTERPOLATION, FixedPoint.of(10), FixedPoint.of(0, 333_333)),
                event(5, CanonicalEventType.ADVANCE_TIME, 7L),
            ),
        ),
        Fixture(
            id = "FX-RANDOM-01",
            masterSeed = -0x5DEECE66DL,
            events = listOf(
                event(1, CanonicalEventType.DRAW_RANDOM, RandomDomainRegistry.DOMAIN_QUALIFICATION_PRIMARY.toLong()),
                event(2, CanonicalEventType.DRAW_RANDOM, RandomDomainRegistry.DOMAIN_QUALIFICATION_SECONDARY.toLong()),
                event(3, CanonicalEventType.DRAW_RANDOM, RandomDomainRegistry.DOMAIN_QUALIFICATION_PRIMARY.toLong()),
                event(4, CanonicalEventType.APPLY_DECAY, FixedPoint.of(0, 500_000)),
                event(5, CanonicalEventType.DRAW_RANDOM, RandomDomainRegistry.DOMAIN_QUALIFICATION_SECONDARY.toLong()),
            ),
        ),
        Fixture(
            id = "FX-DECAY-01",
            masterSeed = 1L,
            events = listOf(
                event(1, CanonicalEventType.DRAW_RANDOM, RandomDomainRegistry.DOMAIN_QUALIFICATION_PRIMARY.toLong()),
                event(2, CanonicalEventType.APPLY_DECAY, FixedPoint.of(0, 900_000)),
                event(3, CanonicalEventType.APPLY_DECAY, FixedPoint.of(0, 900_000)),
                event(4, CanonicalEventType.APPLY_DECAY, FixedPoint.of(0, 900_000)),
                event(5, CanonicalEventType.APPLY_DECAY, FixedPoint.of(0, 900_000)),
                event(6, CanonicalEventType.APPLY_DECAY, FixedPoint.of(0, 900_000)),
            ),
        ),
        Fixture(
            id = "FX-WITNESSED-01",
            masterSeed = 0x7FFFFFFFFFFFFFFFL,
            events = listOf(
                event(1, CanonicalEventType.ADVANCE_TIME, 5L),
                event(
                    2,
                    CanonicalEventType.MATERIAL_INTERACTION,
                    FixedPoint.of(2, 500_000),
                    durability = DurabilityClass.WITNESSED,
                ),
                event(3, CanonicalEventType.APPLY_DELTA, FixedPoint.of(0, 1)),
                event(
                    4,
                    CanonicalEventType.MATERIAL_INTERACTION,
                    FixedPoint.of(-1),
                    durability = DurabilityClass.WITNESSED,
                ),
            ),
        ),
        Fixture(
            id = "FX-BOUNDARY-01",
            masterSeed = Long.MIN_VALUE + 1L,
            events = listOf(
                event(1, CanonicalEventType.APPLY_DELTA, FixedPoint.MAX),
                // Deliberately saturating: this fixture exists to prove that
                // saturation is deterministic and diagnosed, not that it is absent.
                event(2, CanonicalEventType.APPLY_DELTA, FixedPoint.MAX),
                event(3, CanonicalEventType.APPLY_DELTA, FixedPoint.MIN),
                event(4, CanonicalEventType.APPLY_INTERPOLATION, FixedPoint.MIN, FixedPoint.ONE),
            ),
        ),
    )

    /**
     * Fixtures whose intended operating range must **not** saturate.
     *
     * `FX-BOUNDARY-01` is excluded by name: it is the fixture that deliberately
     * runs past the bounds. Every other fixture saturating would be an R001 exit
     * gate failure, because saturation inside a normal operating range is a
     * qualification failure rather than a warning.
     */
    public fun normalOperatingFixtureIds(): Set<String> =
        fixtures().map { it.id }.filterNot { it == "FX-BOUNDARY-01" }.toSet()

    // ----------------------------------------------------------------- execution

    private fun runFixture(fixture: Fixture): FixtureResult {
        val observer = RecordingSaturationObserver()
        val ctx = ArithmeticContext(0, 0L, ExecutionMode.FOREGROUND, observer)
        val genesis = CanonicalSnapshot.genesis(fixture.masterSeed)

        // Direct reduction.
        val direct = CanonicalReducer.reduceAll(genesis, fixture.events, ctx)

        // The same events again, through the durable pipeline, then replayed
        // from the durable medium alone.
        val journalObserver = RecordingSaturationObserver()
        val journalCtx = ArithmeticContext(0, 0L, ExecutionMode.FOREGROUND, journalObserver)
        val journal = DurableJournal()
        val pipeline = WitnessedInteractionPipeline(genesis, journal)
        for (event in fixture.events) {
            pipeline.submit(event, journalCtx)
        }

        val replayObserver = RecordingSaturationObserver()
        val replayCtx = ArithmeticContext(0, 0L, ExecutionMode.FOREGROUND, replayObserver)
        val recovery = InteractionRecovery.recover(genesis, journal.rawBytes(), replayCtx)

        val journalDigest = CanonicalHash.hex(
            CanonicalHash.ofEnvelope(
                CanonicalEnvelope.wrap(
                    CommitFrame.SCHEMA_ID,
                    CommitFrame.SCHEMA_VERSION,
                    CanonicalWriter(256).apply {
                        beginSequence(journal.size)
                        journal.rawBytes().forEach { putBytes(it) }
                    }.toByteArray(),
                ),
            ),
        )

        return FixtureResult(
            id = fixture.id,
            directStateHashHex = direct.stateHashHex(),
            replayStateHashHex = recovery.snapshot.stateHashHex(),
            framesApplied = recovery.framesRecovered,
            journalDigestHex = journalDigest,
            saturationCount = observer.count,
        )
    }

    /**
     * Deterministic fixed-point vectors.
     *
     * Chosen to exercise the places fixed-point implementations actually break:
     * the exact bounds, rounding ties in both directions, negative operands where
     * truncation and floor disagree, and multiplications that require the full
     * 128-bit intermediate.
     */
    private fun fixedPointDigest(): String {
        val observer = RecordingSaturationObserver()
        val ctx = ArithmeticContext.unattributed(observer)
        val writer = CanonicalWriter(512)

        val operands = longArrayOf(
            0L, 1L, -1L, 500_000L, -500_000L, FixedPoint.ONE, -FixedPoint.ONE,
            999_999L, 1_000_001L, 3_000_000L, -3_000_000L,
            FixedPoint.MAX, FixedPoint.MIN, FixedPoint.MAX / 2L, FixedPoint.MIN / 2L,
        )

        for (a in operands) {
            for (b in operands) {
                writer.putI64(FixedPoint.satAdd(a, b, ctx))
                writer.putI64(FixedPoint.satSubtract(a, b, ctx))
                writer.putI64(FixedPoint.satMultiplyScaled(a, b, ctx))
                writer.putI64(FixedPoint.satDivide(a, b, ctx))
                writer.putI64(FixedPoint.satDecay(a, b, ctx))
                writer.putI64(FixedPoint.satInterpolate(a, b, 333_333L, ctx))
            }
        }
        // Rounding ties, both signs.
        for (numerator in -10L..10L) {
            writer.putI64(FixedPoint.satDivide(numerator, 2_000_000L, ctx))
            writer.putI64(FixedPoint.satMultiplyScaled(numerator, 500_000L, ctx))
        }
        writer.putI32(observer.count)

        return digestOf(310, writer.toByteArray())
    }

    /** Deterministic PRNG vectors: derivation, draws, serialization round trip. */
    private fun randomDigest(): String {
        val writer = CanonicalWriter(512)
        val seeds = longArrayOf(0L, 1L, -1L, 0x0123456789ABCDEFL, Long.MAX_VALUE, Long.MIN_VALUE + 1L)
        for (masterSeed in seeds) {
            for (domainId in RandomDomainRegistry.registeredDomainIds()) {
                val stream = RandomSubstream.derive(masterSeed, domainId)
                writer.putI64(stream.seed)
                repeat(8) { writer.putI64(stream.nextLong()) }
                repeat(4) { writer.putI32(stream.nextIntBelow(1_000_001)) }
                writer.putRawBytes(stream.serialize())
                // Round trip must be exact, including the counter.
                val restored = RandomSubstream.deserialize(stream.serialize())
                writer.putI64(restored.counter)
                writer.putI64(restored.nextLong())
            }
        }
        return digestOf(311, writer.toByteArray())
    }

    /** Lookup table digests, verified rather than merely read. */
    private fun lookupDigest(): String {
        val writer = CanonicalWriter(256)
        for (table in GeneratedLookupTables.ALL) {
            table.verify()
            writer.putIdentifier(table.tableId)
            writer.putI32(table.tableVersion)
            writer.putI32(table.size)
            writer.putIdentifier(table.computedDigestHex().take(64))
        }
        return digestOf(312, writer.toByteArray())
    }

    /** Serialization stability: encode, decode, re-encode, compare. */
    private fun serializationDigest(): String {
        val writer = CanonicalWriter(512)
        for (fixture in fixtures()) {
            val genesis = CanonicalSnapshot.genesis(fixture.masterSeed)
            val encoded = genesis.canonicalBytes()
            val roundTripped = CanonicalSnapshot.decode(encoded).canonicalBytes()
            writer.putBool(encoded.contentEquals(roundTripped))
            writer.putBytes(encoded)
            // Migration path: a version-0 artifact must land on a stable version-1 result.
            val migrated = SnapshotMigration.migrateToCurrent(
                SnapshotMigration.encodeLegacyV0(fixture.masterSeed and 0xFFFF, fixture.masterSeed, 0L),
            )
            writer.putBytes(migrated.canonicalBytes())
        }
        return digestOf(313, writer.toByteArray())
    }

    /**
     * Domain-insertion isolation.
     *
     * Consumes two substreams, inserts a third domain that did not previously
     * exist, and checks that the first two produce exactly the draws they were
     * going to produce. This is the executable form of "adding a new random
     * domain must not change output of any existing stream".
     */
    private fun domainIsolationHolds(): Boolean {
        val masterSeed = 0x5EEDL
        val genesis = CanonicalSnapshot.genesis(masterSeed)
        val ctx = ArithmeticContext.unattributed()

        val draws = listOf(
            event(1, CanonicalEventType.DRAW_RANDOM, RandomDomainRegistry.DOMAIN_QUALIFICATION_PRIMARY.toLong()),
            event(2, CanonicalEventType.DRAW_RANDOM, RandomDomainRegistry.DOMAIN_QUALIFICATION_SECONDARY.toLong()),
            event(3, CanonicalEventType.DRAW_RANDOM, RandomDomainRegistry.DOMAIN_QUALIFICATION_PRIMARY.toLong()),
        )

        val without = CanonicalReducer.reduceAll(genesis, draws, ctx)

        val withLateDomain = CanonicalReducer.reduceAll(
            genesis.withSubstream(
                RandomSubstream.derive(masterSeed, RandomDomainRegistry.DOMAIN_QUALIFICATION_LATE_INSERT),
            ),
            draws,
            ctx,
        )

        // Every pre-existing substream must be identical, and the numeric value
        // they folded into must be identical too.
        val primaryUnchanged = without.substreamFor(RandomDomainRegistry.DOMAIN_QUALIFICATION_PRIMARY) ==
            withLateDomain.substreamFor(RandomDomainRegistry.DOMAIN_QUALIFICATION_PRIMARY)
        val secondaryUnchanged = without.substreamFor(RandomDomainRegistry.DOMAIN_QUALIFICATION_SECONDARY) ==
            withLateDomain.substreamFor(RandomDomainRegistry.DOMAIN_QUALIFICATION_SECONDARY)

        return primaryUnchanged && secondaryUnchanged && without.numericB == withLateDomain.numericB
    }

    private fun digestOf(schemaId: Int, payload: ByteArray): String =
        CanonicalHash.hex(CanonicalHash.ofEnvelope(CanonicalEnvelope.wrap(schemaId, 1, payload)))

    /** Runs everything and produces the single comparable evidence digest. */
    public fun run(): Report {
        val results = fixtures().map { runFixture(it) }
        val fixedPoint = fixedPointDigest()
        val random = randomDigest()
        val lookup = lookupDigest()
        val serialization = serializationDigest()
        val isolation = domainIsolationHolds()

        val writer = CanonicalWriter(1024)
            .putIdentifier(FIXTURE_SET_ID)
            .putI32(FIXTURE_SET_VERSION)
            .putI32(CanonicalEnvelope.CONTRACT_VERSION)
            .beginSequence(results.size)
        for (result in results) {
            writer.putIdentifier(result.id)
            writer.putIdentifier(result.directStateHashHex)
            writer.putIdentifier(result.replayStateHashHex)
            writer.putI32(result.framesApplied)
            writer.putIdentifier(result.journalDigestHex)
        }
        writer.putIdentifier(fixedPoint)
        writer.putIdentifier(random)
        writer.putIdentifier(lookup)
        writer.putIdentifier(serialization)
        writer.putBool(isolation)

        val evidenceDigest = digestOf(EVIDENCE_SCHEMA_ID, writer.toByteArray())

        return Report(
            fixtureResults = results,
            fixedPointDigestHex = fixedPoint,
            randomDigestHex = random,
            lookupDigestHex = lookup,
            serializationDigestHex = serialization,
            domainIsolationHolds = isolation,
            evidenceDigestHex = evidenceDigest,
            totalSaturations = results.sumOf { it.saturationCount },
        )
    }

    /**
     * Human-readable rendering, used by the desktop runner and the Android
     * instrumented test so both targets emit the same comparable text.
     */
    public fun renderReport(report: Report): String {
        val lines = mutableListOf(
            "R001 determinism qualification",
            "fixture set: $FIXTURE_SET_ID v$FIXTURE_SET_VERSION",
            "determinism contract version: ${CanonicalEnvelope.CONTRACT_VERSION}",
            "",
        )
        for (result in report.fixtureResults) {
            lines += "fixture ${result.id}"
            lines += "  direct state hash : ${result.directStateHashHex}"
            lines += "  replay state hash : ${result.replayStateHashHex}"
            lines += "  replay matches    : ${result.replayMatches}"
            lines += "  frames applied    : ${result.framesApplied}"
            lines += "  journal digest    : ${result.journalDigestHex}"
            lines += "  saturations       : ${result.saturationCount}"
        }
        lines += ""
        lines += "fixed-point digest   : ${report.fixedPointDigestHex}"
        lines += "random digest        : ${report.randomDigestHex}"
        lines += "lookup digest        : ${report.lookupDigestHex}"
        lines += "serialization digest : ${report.serializationDigestHex}"
        lines += "domain isolation     : ${report.domainIsolationHolds}"
        lines += "all replays match    : ${report.allReplaysMatch}"
        lines += ""
        lines += "R001_EVIDENCE_DIGEST=${report.evidenceDigestHex}"
        return lines.joinToString(separator = "\n")
    }
}
