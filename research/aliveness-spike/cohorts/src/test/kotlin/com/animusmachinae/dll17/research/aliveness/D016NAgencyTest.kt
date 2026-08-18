package com.animusmachinae.dll17.research.aliveness

import com.animusmachinae.dll17.core.math.ArithmeticContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class D016NAgencyTest {
    private val fx = Fx(ArithmeticContext.unattributed())

    @Test
    fun `night alone does not make fresh rest or sleep eligible`() {
        val state = OrganismState(1L, Mechanism.QUALIFIED_SET)
        val habitat = Habitat(1L, HabitatCondition.STATIC)
        val proposals = Controller(fx).propose(state, habitat, SpikeContract.TICKS_PER_VIRTUAL_DAY * 3L / 4L)
        assertTrue(proposals.none { it.action == SpikeAction.REST || it.action == SpikeAction.SLEEP })
    }

    @Test
    fun `all salient inputs enter one bounded pending slot and orient without forced compliance`() {
        val agent = OrganismAgent(Cohort.FULL, 7L, fx)
        val habitat = Habitat(7L, HabitatCondition.STATIC)
        val cases = listOf(
            InteractionKind.TOUCH to HabitatObject.PERSON_ALPHA,
            InteractionKind.CALL to HabitatObject.PERSON_ALPHA,
            InteractionKind.OFFER_FOOD to HabitatObject.FOOD_TROUGH,
            InteractionKind.PRESENT_OBJECT to HabitatObject.PLAY_BALL,
            InteractionKind.STARTLE to HabitatObject.AVERSIVE_BUZZER,
        )
        for ((kind, target) in cases) {
            agent.receive(InteractionEvent(0L, kind, target, target.takeIf { it.kind == ObjectKind.SOCIAL }), 0L)
            assertEquals(kind, agent.state.pendingStimulus?.kind)
            val choice = agent.decide(habitat, 0L)
            val expected = if (kind == InteractionKind.TOUCH) SpikeAction.RESPOND_TO_TOUCH
            else if (kind == InteractionKind.STARTLE) SpikeAction.WITHDRAW else SpikeAction.ORIENT
            assertEquals(expected, choice.action)
            assertEquals(null, agent.state.pendingStimulus)
            assertEquals(kind, agent.state.interactionEpisodeKind)
        }
    }

    @Test
    fun `active salient stimulus interrupts sleep and permits bounded resumption`() {
        val agent = OrganismAgent(Cohort.FULL, 9L, fx)
        val habitat = Habitat(9L, HabitatCondition.STATIC)
        agent.state.committedAction = SpikeAction.SLEEP
        agent.state.committedTarget = null
        agent.state.commitmentRemaining = SpikeContract.COMMITMENT_TICKS_SLEEP
        agent.receive(InteractionEvent(1L, InteractionKind.CALL, HabitatObject.PERSON_ALPHA, HabitatObject.PERSON_ALPHA), 1L)
        val choice = agent.decide(habitat, 1L)
        assertEquals(SpikeAction.ORIENT, choice.action)
        assertEquals(SpikeAction.SLEEP, agent.state.interruptedAction)
        assertEquals(SpikeContract.COMMITMENT_TICKS_INTERACTION, agent.state.commitmentRemaining)
    }

    @Test
    fun `owner acknowledgement persists as a bounded episode before arbitration resumes`() {
        val agent = OrganismAgent(Cohort.FULL, 11L, fx)
        val habitat = Habitat(11L, HabitatCondition.STATIC)
        agent.receive(
            InteractionEvent(0L, InteractionKind.CALL, HabitatObject.PERSON_ALPHA, HabitatObject.PERSON_ALPHA),
            0L,
        )
        assertEquals(SpikeAction.ORIENT, agent.decide(habitat, 0L).action)
        repeat(SpikeContract.COMMITMENT_TICKS_INTERACTION - 1) { offset ->
            val tick = offset + 1L
            agent.advance(habitat, tick)
            assertEquals(SpikeAction.ORIENT, agent.decide(habitat, tick).action)
        }
        assertEquals(InteractionKind.CALL, agent.state.interactionEpisodeKind)
        agent.advance(habitat, SpikeContract.COMMITMENT_TICKS_INTERACTION.toLong())
        assertEquals(null, agent.state.interactionEpisodeKind)
    }

    @Test
    fun `startle still interrupts an ordinary intention immediately`() {
        val agent = OrganismAgent(Cohort.FULL, 13L, fx)
        val habitat = Habitat(13L, HabitatCondition.STATIC)
        agent.state.committedAction = SpikeAction.EXPLORE
        agent.state.committedTarget = HabitatObject.PLAY_BALL
        agent.state.committedTier = 4
        agent.state.commitmentRemaining = SpikeContract.COMMITMENT_TICKS_DEFAULT
        agent.receive(InteractionEvent(1L, InteractionKind.STARTLE, HabitatObject.AVERSIVE_BUZZER, null), 1L)
        assertEquals(SpikeAction.WITHDRAW, agent.decide(habitat, 1L).action)
    }
}
