package com.animusmachinae.dll17.android

import com.animusmachinae.dll17.research.aliveness.AgentChoice
import com.animusmachinae.dll17.research.aliveness.HabitatObject
import com.animusmachinae.dll17.research.aliveness.InteractionKind
import com.animusmachinae.dll17.research.aliveness.Outcome
import com.animusmachinae.dll17.research.aliveness.SpikeAction
import com.animusmachinae.dll17.research.aliveness.SpikeExpressionContract
import com.animusmachinae.dll17.research.aliveness.StepRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerAlivenessHarnessTest {

    @Test
    fun autonomousTicksAdvanceWithoutInput() {
        val harness = OwnerAlivenessHarness()
        repeat(12) { harness.advance() }
        assertEquals(12L, harness.tick)
        assertEquals(0, harness.deliveredInteractions.size)
    }

    @Test
    fun allSixOwnerInteractionsReachTheFullOrganism() {
        val harness = OwnerAlivenessHarness()
        val expected = OwnerInteraction.entries.toList()

        expected.forEach { interaction ->
            val (kind, target) = interaction.toEvent()
            harness.submit(kind, target)
            harness.advance()
        }

        assertEquals(expected.size, harness.deliveredInteractions.size)
        assertEquals(
            expected.map { it.toEvent().first },
            harness.deliveredInteractions.map { it.kind },
        )
        assertEquals(HabitatObject.PERSON_ALPHA, harness.deliveredInteractions.first().personId)
    }

    @Test
    fun adapterConsumesStepRecordWithoutLosingActionOrTarget() {
        val adapter = OwnerEmbodimentAdapter(initialFrame())
        val record = record(SpikeAction.PLAY, HabitatObject.PLAY_BALL, 4L)

        val rendered = adapter.consume(record)

        assertEquals(record.choice.action, rendered.action)
        assertEquals(record.choice.target, rendered.target)
        assertEquals(EmbodiedBehavior.PLAYING, rendered.behavior)
        assertEquals(record.frame.expression, rendered.expression)
    }

    @Test
    fun everyCanonicalActionHasItsOwnEmbodiedBehavior() {
        val mapping = SpikeAction.ALL.associateWith(::embodiedBehaviorFor)

        assertEquals(SpikeAction.ALL.size, mapping.size)
        assertEquals(SpikeAction.ALL.size, mapping.values.toSet().size)
        assertNotEquals(mapping.getValue(SpikeAction.EAT), mapping.getValue(SpikeAction.APPROACH))
        assertNotEquals(mapping.getValue(SpikeAction.OBSERVE), mapping.getValue(SpikeAction.ORIENT))
        assertNotEquals(mapping.getValue(SpikeAction.PLAY), mapping.getValue(SpikeAction.EXPLORE))
    }

    @Test
    fun actionPosesStayLocalAndExpressDepthRatherThanHabitatTravel() {
        val idleAdapter = OwnerEmbodimentAdapter(initialFrame())
        val idle = idleAdapter.consume(record(SpikeAction.IDLE_VARIATION, null, 1L))
        val approachAdapter = OwnerEmbodimentAdapter(initialFrame())
        val approach = approachAdapter.consume(record(SpikeAction.APPROACH, HabitatObject.PLAY_BALL, 1L))
        val withdrawAdapter = OwnerEmbodimentAdapter(initialFrame())
        val withdraw = withdrawAdapter.consume(record(SpikeAction.WITHDRAW, HabitatObject.AVERSIVE_BUZZER, 1L))

        assertEquals(ScenePoint(0.50f, 0.53f), idle.position)
        assertTrue(approach.depthScale > idle.depthScale)
        assertTrue(withdraw.depthScale < idle.depthScale)
        assertTrue(listOf(idle, approach, withdraw).all { it.position.x in 0.45f..0.55f })
        assertTrue(listOf(idle, approach, withdraw).all { it.position.y in 0.45f..0.60f })
        assertEquals(EmbodiedBehavior.APPROACHING, approach.behavior)
        assertEquals(EmbodiedBehavior.RETREATING, withdraw.behavior)
    }

    @Test
    fun directCreatureAndBackgroundTapsHaveNaturalMeaning() {
        val pet = ScenePoint(0.50f, 0.53f)

        assertEquals(OwnerInteraction.TOUCH, interactionAt(pet, pet))
        assertEquals(
            OwnerInteraction.WITHDRAW_ATTENTION,
            interactionAt(ScenePoint(0.05f, 0.08f), pet),
        )

        assertEquals(InteractionKind.CALL, OwnerInteraction.CALL.toEvent().first)
        assertEquals(InteractionKind.WITHDRAW_ATTENTION, OwnerInteraction.WITHDRAW_ATTENTION.toEvent().first)
        assertEquals(InteractionKind.STARTLE, OwnerInteraction.STARTLE.toEvent().first)
    }

    @Test
    fun propsAndIntentionCuesAppearOnlyForRelevantSelectedActions() {
        val adapter = OwnerEmbodimentAdapter(initialFrame())

        val eat = adapter.consume(record(SpikeAction.EAT, HabitatObject.FOOD_TROUGH, 1L))
        val play = adapter.consume(record(SpikeAction.PLAY, HabitatObject.PLAY_BALL, 2L))
        val sleep = adapter.consume(record(SpikeAction.SLEEP, HabitatObject.SHELTER, 3L))
        val idle = adapter.consume(record(SpikeAction.IDLE_VARIATION, null, 4L))

        assertEquals(CompanionCue.FOOD, eat.cue)
        assertEquals(CompanionCue.PLAY, play.cue)
        assertEquals(CompanionCue.SLEEP, sleep.cue)
        assertEquals(null, idle.cue)
    }

    @Test
    fun ownerInteractionMappingUsesOnlyExistingInteractionVocabulary() {
        assertEquals(InteractionKind.TOUCH, OwnerInteraction.TOUCH.toEvent().first)
        assertEquals(InteractionKind.CALL, OwnerInteraction.CALL.toEvent().first)
        assertEquals(InteractionKind.OFFER_FOOD, OwnerInteraction.OFFER_FOOD.toEvent().first)
        assertEquals(InteractionKind.PRESENT_OBJECT, OwnerInteraction.PRESENT_OBJECT.toEvent().first)
        assertEquals(InteractionKind.WITHDRAW_ATTENTION, OwnerInteraction.WITHDRAW_ATTENTION.toEvent().first)
        assertEquals(InteractionKind.STARTLE, OwnerInteraction.STARTLE.toEvent().first)
    }

    private fun initialFrame(): SpikeExpressionContract.ExpressionFrame =
        SpikeExpressionContract.frameFor(
            SpikeExpressionContract.PresentationInput(
                action = SpikeAction.IDLE_VARIATION,
                target = null,
                intensity = 0L,
                valence = 0L,
                tick = 0L,
            ),
        )

    private fun record(action: SpikeAction, target: HabitatObject?, tick: Long): StepRecord =
        StepRecord(
            tick = tick,
            choice = AgentChoice(action, target, action.tier, null),
            outcome = Outcome(true, 0L, false, false, null),
            frame = SpikeExpressionContract.frameFor(
                SpikeExpressionContract.PresentationInput(
                    action = action,
                    target = target,
                    intensity = 500_000L,
                    valence = 0L,
                    tick = tick,
                ),
            ),
            trace = null,
            attribution = null,
            spontaneous = true,
        )
}
