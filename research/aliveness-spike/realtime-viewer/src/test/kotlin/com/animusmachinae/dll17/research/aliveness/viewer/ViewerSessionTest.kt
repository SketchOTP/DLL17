package com.animusmachinae.dll17.research.aliveness.viewer

import com.animusmachinae.dll17.research.aliveness.Cohort
import com.animusmachinae.dll17.research.aliveness.HabitatObject
import com.animusmachinae.dll17.research.aliveness.InteractionKind
import com.animusmachinae.dll17.research.aliveness.SpikeContract
import com.animusmachinae.dll17.research.aliveness.SpikeExpressionContract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The viewer is headless-testable on purpose: blinding, timing and the study
 * event log are protocol properties, and a property that can only be checked by
 * looking at a window is a property nobody checks.
 */
class ViewerSessionTest {

    private fun session(cohort: Cohort) = ViewerSession(
        sessionId = "T-${cohort.name}",
        displayLabel = "Creature",
        cohort = cohort,
        seed = 4242L,
        durationSeconds = 20,
    )

    @Test
    fun `no public member of a session exposes its cohort`() {
        // Plain JVM reflection rather than kotlin-reflect: the point is that no
        // accessor exists at all, which the class surface answers directly.
        val accessors = ViewerSession::class.java.methods.map { it.name } +
            ViewerSession::class.java.fields.map { it.name }
        assertTrue(
            accessors.none { it.contains("ohort") },
            "ViewerSession exposes cohort: $accessors",
        )
        assertEquals("Creature", session(Cohort.FULL).displayLabel)
    }

    @Test
    fun `every cohort presents the identical label and duration`() {
        val labels = Cohort.entries.map { session(it).displayLabel }.distinct()
        val durations = Cohort.entries.map { session(it).durationSeconds }.distinct()
        assertEquals(1, labels.size, "cohorts must be indistinguishable by label: $labels")
        assertEquals(1, durations.size, "cohorts must share one session duration")
    }

    @Test
    fun `every cohort advances organism time at the identical rate`() {
        for (cohort in Cohort.entries) {
            val s = session(cohort)
            repeat(50) { s.advance() }
            assertEquals(50L, s.tick, "${cohort.cohortId} advanced a different number of ticks")
            assertEquals(50L * SpikeContract.VIEWER_TICK_MILLIS, s.elapsedMillis)
        }
    }

    @Test
    fun `every cohort renders only frozen presentation vocabulary`() {
        for (cohort in Cohort.entries) {
            val s = session(cohort)
            repeat(300) {
                s.advance()
                val f = s.frame
                assertTrue(f.microMovement in SpikeExpressionContract.MICRO_MOVEMENTS)
                assertTrue(f.motionAmplitude in 0L..1_000_000L)
            }
        }
    }

    @Test
    fun `a session completes exactly at its frozen duration`() {
        val s = session(Cohort.SCRIPTED_PET_BASELINE)
        val expectedTicks = 20 * 1_000 / SpikeContract.VIEWER_TICK_MILLIS
        repeat(expectedTicks - 1) { s.advance() }
        assertTrue(!s.complete, "session ended early")
        s.advance()
        assertTrue(s.complete, "session did not end on time")
    }

    @Test
    fun `rater inputs are recorded as normalized study events`() {
        val s = session(Cohort.FULL)
        s.submit(InteractionKind.TOUCH, HabitatObject.PERSON_ALPHA)
        s.advance()
        s.submit(InteractionKind.PRESENT_OBJECT, HabitatObject.PLAY_BALL)
        s.advance()
        val log = s.studyEvents()
        assertEquals(2, log.size)
        assertEquals(InteractionKind.TOUCH, log[0].kind)
        assertEquals(HabitatObject.PLAY_BALL, log[1].target)
        assertEquals(0L, log[0].tick)
    }

    @Test
    fun `the paired session counterbalances order without revealing it`() {
        val forward = PairedSession("P0", Cohort.FULL, Cohort.SCRIPTED_PET_BASELINE, 7L, 0)
        val reversed = PairedSession("P1", Cohort.FULL, Cohort.SCRIPTED_PET_BASELINE, 7L, 1)
        assertEquals("Creature A", forward.first.displayLabel)
        assertEquals("Creature A", reversed.first.displayLabel)
        // Same labels either way round; the assignment differs underneath, which
        // is exactly what counterbalancing means.
        fun signature(s: ViewerSession): String {
            repeat(200) { s.advance() }
            return s.frameSignature()
        }
        assertTrue(
            signature(forward.first) != signature(reversed.first),
            "order index did not change which controller sat behind Creature A",
        )
    }

    @Test
    fun `both instances of a pair start from the same matched condition`() {
        val pair = PairedSession("P2", Cohort.FULL, Cohort.SCRIPTED_PET_BASELINE, 31L, 0)
        assertEquals(31L, pair.matchedSeed)
        assertEquals(pair.first.presentObjects(), pair.second.presentObjects())
    }
}
