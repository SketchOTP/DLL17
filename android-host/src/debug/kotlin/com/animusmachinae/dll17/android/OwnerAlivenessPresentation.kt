package com.animusmachinae.dll17.android

import com.animusmachinae.dll17.research.aliveness.HabitatObject
import com.animusmachinae.dll17.research.aliveness.SpikeExpressionContract

/**
 * Android receives this projection of the frozen frame contract only. It has
 * no action, utility, drive, mechanism or decision-trace input from which it
 * could select behaviour independently.
 */
internal data class CreatureRenderPose(
    val posture: SpikeExpressionContract.Posture,
    val expression: SpikeExpressionContract.Expression,
    val gazeTarget: HabitatObject?,
    val motionAmplitude: Long,
    val vocalizing: Boolean,
    val microMovement: String,
    val attentionObject: HabitatObject?,
)

internal fun renderPose(frame: SpikeExpressionContract.ExpressionFrame): CreatureRenderPose =
    CreatureRenderPose(
        posture = frame.posture,
        expression = frame.expression,
        gazeTarget = frame.gazeTarget,
        motionAmplitude = frame.motionAmplitude,
        vocalizing = frame.vocalizing,
        microMovement = frame.microMovement,
        attentionObject = frame.attentionObject,
    )
