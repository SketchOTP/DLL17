package com.animusmachinae.dll17.research.aliveness.sim

import com.animusmachinae.dll17.research.aliveness.*

public object Probe2 {
    @JvmStatic
    public fun main(args: Array<String>) {
        val (fx, sat) = Fx.counting()
        val habitat = Habitat(11L, HabitatCondition.STATIC)
        val agent = OrganismAgent(Cohort.FULL, 11L, fx)
        val rt = SpikeRuntime("p2", agent, habitat,
            OutcomeModel(aversiveSafeFromTick = 20L * SpikeContract.TICKS_PER_VIRTUAL_DAY), fx)
        val pair = HashMap<String, Int>()
        for (t in 0 until 40L * SpikeContract.TICKS_PER_VIRTUAL_DAY) {
            val r = rt.step(t)
            val k = r.choice.action.name + "@" + (r.choice.target?.name ?: "-")
            pair[k] = (pair[k] ?: 0) + 1
        }
        println("top pairs: " + pair.entries.sortedByDescending { it.value }.take(18)
            .joinToString(" ") { "${it.key}=${it.value}" })
        val s = agent.state
        println("pref: " + HabitatObject.ALL.joinToString(" ") { "${it.name.take(6)}=${RunMeasures.fx(s.preference[it.ordinal0])}" })
        println("habit: " + HabitatObject.ALL.joinToString(" ") { "${it.name.take(6)}=${RunMeasures.fx(s.habituation[it.ordinal0])}" })
        println("fear=${RunMeasures.fx(s.fear[HabitatObject.AVERSIVE_BUZZER.ordinal0])} peak=${RunMeasures.fx(s.fearPeak[HabitatObject.AVERSIVE_BUZZER.ordinal0])}")
        println("eatHabit trough=${RunMeasures.fx(s.habit[s.index(SpikeAction.EAT, HabitatObject.FOOD_TROUGH)])} cache=${RunMeasures.fx(s.habit[s.index(SpikeAction.EAT, HabitatObject.FOOD_CACHE)])}")
    }
}
