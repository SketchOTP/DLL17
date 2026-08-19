package com.animusmachinae.dll17.android

import com.animusmachinae.dll17.research.aliveness.HabitatObject
import com.animusmachinae.dll17.research.aliveness.InteractionKind
import com.animusmachinae.dll17.research.aliveness.SpikeExpressionContract
import com.animusmachinae.dll17.research.aliveness.StepRecord

/** Shared debug-only UI boundary for the old owner scaffold and D016-AB. */
internal interface DebugExperienceHarness {
    val frame: SpikeExpressionContract.ExpressionFrame
    val showManualControls: Boolean
    val statusText: String
    fun submit(kind: InteractionKind, target: HabitatObject? = null)
    fun advance(): StepRecord
}
