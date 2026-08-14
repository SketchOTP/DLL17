package com.animusmachinae.dll17.research.aliveness.sim

import com.animusmachinae.dll17.research.aliveness.*

/** Ad-hoc diagnostic entry point; not part of the evidence pipeline. */
public object Probe2 {
    @JvmStatic
    public fun main(args: Array<String>) {
        for (cohort in listOf(Cohort.SCRIPTED_PET_BASELINE, Cohort.DEGRADED_SCRIPTED_CONTROL, Cohort.FULL)) {
            val r = AcceleratedSimulator.run(
                RunConfig("cmp", cohort, 606L, 40, HabitatCondition.CONTROLLED_NOVELTY, windowDays = 15),
            )
            val m = r.measures
            println("${cohort.cohortId}: H=${RunMeasures.d6(m.windowActionEntropyBits())} " +
                "obj/day=${RunMeasures.d6(m.distinctObjectsInspectedPerDay())} " +
                "occ=${RunMeasures.d6(m.maxWindowOccupancy())} " +
                "inactive=${RunMeasures.d6(m.windowInactivityShare())} " +
                "reg=${RunMeasures.d6(m.cycleRegularity())} " +
                "revisits/day=${RunMeasures.d6(m.revisitationsPerDay())}")
        }
    }
}
