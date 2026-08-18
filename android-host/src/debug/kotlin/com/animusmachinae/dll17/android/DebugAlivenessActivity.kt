package com.animusmachinae.dll17.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animusmachinae.dll17.research.aliveness.HabitatObject
import com.animusmachinae.dll17.research.aliveness.InteractionKind
import com.animusmachinae.dll17.research.aliveness.SpikeExpressionContract
import kotlinx.coroutines.delay

/** Disposable owner-only D016-X experience. Not part of the release APK. */
internal class DebugAlivenessActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OwnerAlivenessScreen(OwnerAlivenessHarness())
                }
            }
        }
    }
}

@Composable
private fun OwnerAlivenessScreen(harness: OwnerAlivenessHarness) {
    var frame by remember { mutableStateOf(harness.frame) }

    LaunchedEffect(harness) {
        while (true) {
            harness.advance()
            frame = harness.frame
            delay(OWNER_TICK_MILLIS)
        }
    }

    fun submit(kind: InteractionKind, target: HabitatObject? = null) {
        harness.submit(kind, target)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF17151D))
            .padding(16.dp),
    ) {
        Text(
            text = "Digital Living Lifeform",
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFFF2EAFB),
        )
        Text(
            text = "Spend a little time with it. Touch the creature or the things around it.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFC8BFD0),
        )
        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF211D2A), RoundedCornerShape(20.dp))
                .pointerInput(Unit) {
                    detectTapGestures { submit(InteractionKind.TOUCH, HabitatObject.PERSON_ALPHA) }
                },
        ) {
            CreatureCanvas(
                frame = frame,
                objects = harness.presentObjects,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = "Tap the creature to touch it. Tap a person to call, food to offer, a play object to show, or the red object to startle.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFC8BFD0),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp),
        ) {
            items(harness.presentObjects.filter { it != HabitatObject.SEALED_GATE }) { objectId ->
                HabitatButton(objectId) {
                    when (objectId.kind) {
                        com.animusmachinae.dll17.research.aliveness.ObjectKind.SOCIAL ->
                            submit(InteractionKind.CALL, objectId)
                        com.animusmachinae.dll17.research.aliveness.ObjectKind.RESOURCE ->
                            submit(InteractionKind.OFFER_FOOD, objectId)
                        com.animusmachinae.dll17.research.aliveness.ObjectKind.PLAY ->
                            submit(InteractionKind.PRESENT_OBJECT, objectId)
                        com.animusmachinae.dll17.research.aliveness.ObjectKind.AVERSIVE ->
                            submit(InteractionKind.STARTLE, objectId)
                        else -> Unit
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { submit(InteractionKind.WITHDRAW_ATTENTION) }) {
                Text("Look away", color = Color(0xFFE0C7FF))
            }
        }
    }
}

@Composable
private fun HabitatButton(objectId: HabitatObject, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(objectLabel(objectId), fontSize = 11.sp, maxLines = 1)
    }
}

private fun objectLabel(objectId: HabitatObject): String = when (objectId) {
    HabitatObject.PERSON_ALPHA -> "Call alpha"
    HabitatObject.PERSON_BETA -> "Call beta"
    HabitatObject.FOOD_TROUGH, HabitatObject.FOOD_CACHE -> "Offer food"
    HabitatObject.PLAY_BALL -> "Show ball"
    HabitatObject.PLAY_CUBE -> "Show cube"
    HabitatObject.PLAY_CHIME -> "Show chime"
    HabitatObject.PLAY_MIRROR -> "Show mirror"
    HabitatObject.AVERSIVE_BUZZER -> "Startle"
    else -> objectId.name.lowercase().replace('_', ' ')
}

@Composable
private fun CreatureCanvas(
    frame: SpikeExpressionContract.ExpressionFrame,
    objects: List<HabitatObject>,
    modifier: Modifier = Modifier,
) {
    val pose = renderPose(frame)
    Canvas(modifier = modifier.padding(12.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f + 24f)
        drawHabitat(center, objects, pose.attentionObject)
        drawCreature(center, pose)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHabitat(
    center: Offset,
    objects: List<HabitatObject>,
    attentionObject: HabitatObject?,
) {
    val radius = size.minDimension * 0.36f
    objects.forEachIndexed { index, objectId ->
        val angle = (2.0 * Math.PI * index / objects.size) - Math.PI / 2.0
        val position = Offset(
            center.x + (kotlin.math.cos(angle) * radius).toFloat(),
            center.y + (kotlin.math.sin(angle) * radius * 0.68).toFloat(),
        )
        val color = habitatColor(objectId)
        drawRoundRect(
            color = color,
            topLeft = Offset(position.x - 28f, position.y - 16f),
            size = androidx.compose.ui.geometry.Size(56f, 32f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(9f, 9f),
        )
        if (objectId == attentionObject) {
            drawRoundRect(
                color = Color(0xFFFFD36A),
                topLeft = Offset(position.x - 33f, position.y - 21f),
                size = androidx.compose.ui.geometry.Size(66f, 42f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(11f, 11f),
                style = Stroke(width = 3f),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCreature(
    center: Offset,
    pose: CreatureRenderPose,
) {
    val amplitude = pose.motionAmplitude / 1_000_000f
    val body = when (pose.posture) {
        SpikeExpressionContract.Posture.STAND -> androidx.compose.ui.geometry.Size(148f, 132f)
        SpikeExpressionContract.Posture.CROUCH -> androidx.compose.ui.geometry.Size(164f, 100f)
        SpikeExpressionContract.Posture.LEAN_IN -> androidx.compose.ui.geometry.Size(156f, 124f)
        SpikeExpressionContract.Posture.LEAN_AWAY -> androidx.compose.ui.geometry.Size(138f, 124f)
        SpikeExpressionContract.Posture.LIE_DOWN -> androidx.compose.ui.geometry.Size(190f, 76f)
        SpikeExpressionContract.Posture.PLAY_BOW -> androidx.compose.ui.geometry.Size(170f, 112f)
    }
    val verticalOffset = when (pose.posture) {
        SpikeExpressionContract.Posture.CROUCH -> 18f
        SpikeExpressionContract.Posture.LEAN_AWAY -> 12f
        SpikeExpressionContract.Posture.LIE_DOWN -> 28f
        else -> 0f
    }
    val microOffset = when (pose.microMovement) {
        "weight-shift", "small-step" -> 5f
        "head-tilt", "look-away" -> -4f
        else -> 0f
    }
    val bodyCenter = Offset(
        center.x + microOffset,
        center.y + verticalOffset + amplitude * 8f,
    )
    drawOval(
        color = expressionColor(pose.expression),
        topLeft = Offset(bodyCenter.x - body.width / 2f, bodyCenter.y - body.height / 2f),
        size = body,
    )

    val gazeBearing = pose.gazeTarget?.ordinal0?.let { ordinal ->
        val normalized = ordinal.toFloat() / 12f
        (normalized - 0.5f) * 2f
    } ?: 0f
    val head = Offset(bodyCenter.x + gazeBearing * 24f, bodyCenter.y - body.height * 0.42f)
    drawOval(
        color = expressionColor(pose.expression),
        topLeft = Offset(head.x - 42f, head.y - 34f),
        size = androidx.compose.ui.geometry.Size(84f, 68f),
    )
    val eyeHeight = if (pose.expression == SpikeExpressionContract.Expression.TIRED) 3f else 10f
    drawOval(Color(0xFF17151D), Offset(head.x - 20f, head.y - 5f), androidx.compose.ui.geometry.Size(8f, eyeHeight))
    drawOval(Color(0xFF17151D), Offset(head.x + 12f, head.y - 5f), androidx.compose.ui.geometry.Size(8f, eyeHeight))

    if (pose.vocalizing) {
        drawArc(
            color = Color(0xFFF1E4B6),
            startAngle = -35f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(head.x + 28f, head.y - 30f),
            size = androidx.compose.ui.geometry.Size(34f, 34f),
            style = Stroke(width = 4f),
        )
    }
}

private fun habitatColor(objectId: HabitatObject): Color = when (objectId.kind) {
    com.animusmachinae.dll17.research.aliveness.ObjectKind.RESOURCE -> Color(0xFF4C8C4A)
    com.animusmachinae.dll17.research.aliveness.ObjectKind.SHELTER -> Color(0xFF6B5B3E)
    com.animusmachinae.dll17.research.aliveness.ObjectKind.SOCIAL -> Color(0xFF466C9C)
    com.animusmachinae.dll17.research.aliveness.ObjectKind.PLAY -> Color(0xFF8A5C9E)
    com.animusmachinae.dll17.research.aliveness.ObjectKind.AVERSIVE -> Color(0xFF9C4646)
    com.animusmachinae.dll17.research.aliveness.ObjectKind.BLOCKED -> Color(0xFF505055)
    com.animusmachinae.dll17.research.aliveness.ObjectKind.NOVELTY -> Color(0xFFC09430)
}

private fun expressionColor(expression: SpikeExpressionContract.Expression): Color = when (expression) {
    SpikeExpressionContract.Expression.NEUTRAL -> Color(0xFFC8C4BC)
    SpikeExpressionContract.Expression.ALERT -> Color(0xFFDAD2A8)
    SpikeExpressionContract.Expression.CONTENT -> Color(0xFFBCD8B4)
    SpikeExpressionContract.Expression.WARY -> Color(0xFFD6B0A0)
    SpikeExpressionContract.Expression.TIRED -> Color(0xFF9C9CA8)
    SpikeExpressionContract.Expression.EXCITED -> Color(0xFFF0CE88)
    SpikeExpressionContract.Expression.WITHDRAWN -> Color(0xFF8C8C96)
}
