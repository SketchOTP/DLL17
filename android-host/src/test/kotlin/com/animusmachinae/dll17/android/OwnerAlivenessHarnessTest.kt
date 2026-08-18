package com.animusmachinae.dll17.android

import com.animusmachinae.dll17.research.aliveness.HabitatObject
import com.animusmachinae.dll17.research.aliveness.InteractionKind
import com.animusmachinae.dll17.research.aliveness.SpikeExpressionContract
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
        val expected = listOf(
            OwnerInteraction.TOUCH,
            OwnerInteraction.CALL,
            OwnerInteraction.OFFER_FOOD,
            OwnerInteraction.PRESENT_OBJECT,
            OwnerInteraction.WITHDRAW_ATTENTION,
            OwnerInteraction.STARTLE,
        )

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
    fun presentationProjectionChangesWhenExpressionFrameChanges() {
        val neutral = SpikeExpressionContract.ExpressionFrame(
            posture = SpikeExpressionContract.Posture.STAND,
            expression = SpikeExpressionContract.Expression.NEUTRAL,
            gazeTarget = null,
            motionAmplitude = 0L,
            vocalizing = false,
            microMovement = "settle",
            attentionObject = null,
        )
        val changed = SpikeExpressionContract.ExpressionFrame(
            posture = SpikeExpressionContract.Posture.LEAN_AWAY,
            expression = SpikeExpressionContract.Expression.WARY,
            gazeTarget = HabitatObject.AVERSIVE_BUZZER,
            motionAmplitude = 850_000L,
            vocalizing = true,
            microMovement = "head-tilt",
            attentionObject = HabitatObject.AVERSIVE_BUZZER,
        )

        assertNotEquals(renderPose(neutral), renderPose(changed))
        assertEquals(SpikeExpressionContract.Posture.LEAN_AWAY, renderPose(changed).posture)
        assertEquals(HabitatObject.AVERSIVE_BUZZER, renderPose(changed).gazeTarget)
        assertTrue(renderPose(changed).vocalizing)
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
}
