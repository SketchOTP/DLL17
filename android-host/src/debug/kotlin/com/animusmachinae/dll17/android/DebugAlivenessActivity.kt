package com.animusmachinae.dll17.android

import android.os.Bundle
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.animusmachinae.dll17.research.aliveness.SpikeExpressionContract
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** D016-AB research-only phone-as-body embodiment. Not part of the release APK. */
internal class DebugAlivenessActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var textToSpeech: TextToSpeech? = null
    private lateinit var phoneBodyHarness: PhoneBodyHarness

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = java.util.Locale.US
                textToSpeech?.setPitch(1.28f)
                textToSpeech?.setSpeechRate(0.92f)
            }
        }
        phoneBodyHarness = PhoneBodyHarness { utterance ->
            textToSpeech?.speak(utterance, TextToSpeech.QUEUE_FLUSH, null, "D016-AB-${System.nanoTime()}")
        }
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFF4C978),
                    surface = Color(0xFF17251E),
                    onSurface = Color(0xFFF5F0DF),
                ),
            ) {
                OwnerPetExperience(phoneBodyHarness)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun onPause() {
        sensorManager.unregisterListener(this)
        super.onPause()
    }

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.values.size < 3) return
        val magnitude = kotlin.math.sqrt(
            (event.values[0] * event.values[0] +
                event.values[1] * event.values[1] +
                event.values[2] * event.values[2]).toDouble(),
        )
        val linearMagnitude = if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            kotlin.math.abs(magnitude - SensorManager.GRAVITY_EARTH.toDouble())
        } else {
            magnitude
        }
        phoneBodyHarness.submitSensorSample(
            event.timestamp,
            (linearMagnitude * 1_000.0 / SensorManager.GRAVITY_EARTH).toInt(),
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

@Composable
private fun OwnerPetExperience(harness: DebugExperienceHarness) {
    val adapter = remember(harness) { OwnerEmbodimentAdapter(harness.frame) }
    var embodiment by remember { mutableStateOf(adapter.current) }
    var animationMillis by remember { mutableLongStateOf(0L) }
    var statusText by remember { mutableStateOf(harness.statusText) }

    LaunchedEffect(harness) {
        while (true) {
            embodiment = adapter.consume(harness.advance())
            statusText = harness.statusText
            delay(OWNER_TICK_MILLIS)
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { animationMillis = it }
        }
    }

    var offeredCue by remember { mutableStateOf<CompanionCue?>(null) }

    val animatedX by animateFloatAsState(
        targetValue = embodiment.position.x,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "pet-x",
    )
    val animatedY by animateFloatAsState(
        targetValue = embodiment.position.y,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "pet-y",
    )
    val displayed = embodiment.copy(position = ScenePoint(animatedX, animatedY))

    fun submit(interaction: OwnerInteraction, offered: CompanionCue? = null) {
        offeredCue = offered
        val (kind, target) = interaction.toEvent()
        harness.submit(kind, target)
    }

    LaunchedEffect(offeredCue) {
        if (offeredCue != null) {
            delay(2200L)
            offeredCue = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101816))
            .safeDrawingPadding()
            .padding(8.dp),
    ) {
        CompanionScreen(
            embodiment = displayed,
            offeredCue = offeredCue,
            animationMillis = animationMillis,
            onInteraction = { submit(it) },
            modifier = Modifier.fillMaxSize(),
        )

        Surface(
            color = Color(0xB824312E),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
        ) {
            Text(
                text = statusText,
                color = Color(0xFFF3DCA7),
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        if (harness.showManualControls) Surface(
            color = Color(0x8F24312E),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PetAffordance("♥") { submit(OwnerInteraction.CALL, CompanionCue.ATTENTION) }
                PetAffordance("🍎") { submit(OwnerInteraction.OFFER_FOOD, CompanionCue.FOOD) }
                PetAffordance("⚽") { submit(OwnerInteraction.PRESENT_OBJECT, CompanionCue.PLAY) }
                PetAffordance("♪") { submit(OwnerInteraction.STARTLE, CompanionCue.ALERT) }
            }
        }
    }
}

@Composable
private fun PetAffordance(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = label,
            color = Color(0xFFF3DCA7),
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CompanionScreen(
    embodiment: OwnerEmbodimentFrame,
    offeredCue: CompanionCue?,
    animationMillis: Long,
    onInteraction: (OwnerInteraction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .background(Color(0xFF17211F), RoundedCornerShape(32.dp))
            .pointerInput(embodiment.position) {
                detectTapGestures { tap ->
                    onInteraction(interactionAt(
                        normalizedTap = ScenePoint(tap.x / size.width, tap.y / size.height),
                        petPosition = embodiment.position,
                    ))
                }
            },
    ) {
        drawCompanionWorld(embodiment, offeredCue, animationMillis)
    }
}

private fun DrawScope.drawCompanionWorld(
    embodiment: OwnerEmbodimentFrame,
    offeredCue: CompanionCue?,
    animationMillis: Long,
) {
    drawRect(
        brush = Brush.radialGradient(
            listOf(Color(0xFF344740), Color(0xFF17211F), Color(0xFF0E1514)),
            center = Offset(size.width * 0.50f, size.height * 0.46f),
            radius = size.maxDimension * 0.72f,
        ),
    )
    val petCenter = point(embodiment.position)
    val phase = ((animationMillis % 1600L).toDouble() / 1600.0 * 2.0 * PI).toFloat()
    val unit = size.minDimension / 245f * embodiment.depthScale

    drawOval(
        Brush.radialGradient(
            listOf(Color(0x5945574F), Color.Transparent),
            center = Offset(petCenter.x, petCenter.y + 92f * unit),
            radius = 150f * unit,
        ),
        Offset(petCenter.x - 170f * unit, petCenter.y + 55f * unit),
        Size(340f * unit, 90f * unit),
    )

    val visibleCue = embodiment.cue ?: offeredCue
    if (visibleCue == CompanionCue.FOOD || embodiment.behavior == EmbodiedBehavior.EATING) {
        drawFoodBowl(
            Offset(petCenter.x + 84f * unit, petCenter.y + 92f * unit),
            72f * unit,
            embodiment.behavior,
        )
    }
    if (visibleCue == CompanionCue.PLAY || embodiment.behavior == EmbodiedBehavior.PLAYING) {
        drawBall(
            Offset(petCenter.x + 100f * unit, petCenter.y + 70f * unit),
            24f * unit,
            embodiment.behavior,
            animationMillis,
        )
    }

    withTransform({ rotate(embodiment.bodyLean, petCenter) }) {
        drawPet(petCenter, unit, embodiment, phase)
    }
    drawIntentionCue(visibleCue, Offset(petCenter.x + 104f * unit, petCenter.y - 118f * unit), 20f * unit)
}

private fun DrawScope.drawIntentionCue(cue: CompanionCue?, center: Offset, radius: Float) {
    if (cue == null || cue == CompanionCue.FOOD || cue == CompanionCue.PLAY) return
    drawCircle(Color(0xDDF3E7C6), radius * 1.15f, center)
    drawCircle(Color(0xFF26352F), radius * 0.82f, center)
    when (cue) {
        CompanionCue.ATTENTION -> {
            drawCircle(Color(0xFFF3DCA7), radius * 0.21f, Offset(center.x - radius * 0.28f, center.y))
            drawCircle(Color(0xFFF3DCA7), radius * 0.21f, Offset(center.x + radius * 0.28f, center.y))
        }
        CompanionCue.ALERT -> {
            drawLine(Color(0xFFF3DCA7), Offset(center.x, center.y - radius * 0.42f), Offset(center.x, center.y + radius * 0.15f), radius * 0.16f, StrokeCap.Round)
            drawCircle(Color(0xFFF3DCA7), radius * 0.09f, Offset(center.x, center.y + radius * 0.46f))
        }
        CompanionCue.SLEEP -> drawTextMark("z", center, radius * 0.72f, Color(0xFFF3DCA7))
        CompanionCue.FOOD, CompanionCue.PLAY -> Unit
    }
}

private fun DrawScope.point(point: ScenePoint): Offset = Offset(size.width * point.x, size.height * point.y)

private fun DrawScope.drawCloud(center: Offset, radius: Float) {
    val cloud = Color(0xBFEAF0E6)
    drawOval(cloud, Offset(center.x - radius, center.y), Size(radius * 2f, radius * 0.72f))
    drawCircle(cloud, radius * 0.48f, Offset(center.x - radius * 0.38f, center.y))
    drawCircle(cloud, radius * 0.58f, Offset(center.x + radius * 0.22f, center.y - radius * 0.12f))
}

private fun DrawScope.drawSun(center: Offset, radius: Float) {
    drawCircle(Color(0xFFFFDA83), radius * 1.45f, center, alpha = 0.24f)
    drawCircle(Color(0xFFFFD36A), radius, center)
}

private fun DrawScope.drawTree(base: Offset, height: Float) {
    drawRoundRect(
        Color(0xFF664A34),
        Offset(base.x - height * 0.09f, base.y - height * 0.36f),
        Size(height * 0.18f, height * 0.45f),
        CornerRadius(height * 0.05f),
    )
    drawCircle(Color(0xFF31563B), height * 0.32f, Offset(base.x, base.y - height * 0.48f))
    drawCircle(Color(0xFF416E48), height * 0.24f, Offset(base.x - height * 0.24f, base.y - height * 0.40f))
    drawCircle(Color(0xFF4D7B50), height * 0.25f, Offset(base.x + height * 0.22f, base.y - height * 0.42f))
}

private fun DrawScope.drawShrub(center: Offset, radius: Float) {
    drawCircle(Color(0xFF294D35), radius, center)
    drawCircle(Color(0xFF3D6B43), radius * 0.72f, Offset(center.x - radius * 0.65f, center.y + radius * 0.10f))
    drawCircle(Color(0xFF47784B), radius * 0.66f, Offset(center.x + radius * 0.62f, center.y + radius * 0.14f))
}

private fun DrawScope.drawBed(center: Offset, width: Float) {
    drawOval(
        Color(0x55211119),
        Offset(center.x - width * 0.58f, center.y + width * 0.20f),
        Size(width * 1.16f, width * 0.35f),
    )
    drawRoundRect(
        Color(0xFF8B5E4B),
        Offset(center.x - width * 0.55f, center.y - width * 0.20f),
        Size(width * 1.10f, width * 0.58f),
        CornerRadius(width * 0.22f),
    )
    drawRoundRect(
        Color(0xFFD6B88B),
        Offset(center.x - width * 0.43f, center.y - width * 0.12f),
        Size(width * 0.86f, width * 0.38f),
        CornerRadius(width * 0.18f),
    )
    drawPath(
        Path().apply {
            moveTo(center.x - width * 0.44f, center.y + width * 0.03f)
            quadraticTo(center.x, center.y + width * 0.22f, center.x + width * 0.44f, center.y + width * 0.03f)
        },
        Color(0xFFB98963),
        style = Stroke(width * 0.05f, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawFoodBowl(center: Offset, width: Float, behavior: EmbodiedBehavior) {
    drawOval(
        Color(0x44211119),
        Offset(center.x - width * 0.55f, center.y + width * 0.17f),
        Size(width * 1.10f, width * 0.30f),
    )
    drawOval(
        Color(0xFF6E403A),
        Offset(center.x - width * 0.50f, center.y - width * 0.20f),
        Size(width, width * 0.52f),
    )
    drawOval(
        Color(0xFFDFC49A),
        Offset(center.x - width * 0.40f, center.y - width * 0.16f),
        Size(width * 0.80f, width * 0.26f),
    )
    repeat(5) { index ->
        val x = center.x + (index - 2) * width * 0.13f
        val lift = if (behavior == EmbodiedBehavior.EATING && index % 2 == 0) width * 0.06f else 0f
        drawCircle(Color(0xFF765A37), width * 0.045f, Offset(x, center.y - width * 0.04f - lift))
    }
}

private fun DrawScope.drawBall(
    center: Offset,
    radius: Float,
    behavior: EmbodiedBehavior,
    animationMillis: Long,
) {
    val bounce = if (behavior == EmbodiedBehavior.PLAYING) {
        kotlin.math.abs(sin(animationMillis / 140.0)).toFloat() * radius * 0.55f
    } else {
        0f
    }
    val ballCenter = Offset(center.x, center.y - bounce)
    drawOval(
        Color(0x44211119),
        Offset(center.x - radius, center.y + radius * 0.72f),
        Size(radius * 2f, radius * 0.52f),
    )
    drawCircle(
        Brush.radialGradient(
            listOf(Color(0xFFFFD772), Color(0xFFD46C45)),
            center = Offset(ballCenter.x - radius * 0.32f, ballCenter.y - radius * 0.35f),
            radius = radius * 1.6f,
        ),
        radius,
        ballCenter,
    )
    drawArc(
        Color(0xFFF7E0A4),
        210f,
        100f,
        false,
        Offset(ballCenter.x - radius * 0.62f, ballCenter.y - radius * 0.65f),
        Size(radius * 1.25f, radius * 1.25f),
        style = Stroke(radius * 0.10f, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawWindChime(center: Offset, size0: Float) {
    drawLine(Color(0xFF6A4B34), Offset(center.x, center.y - size0), Offset(center.x, center.y), size0 * 0.08f)
    drawLine(Color(0xFFE4C87A), Offset(center.x - size0 * 0.34f, center.y - size0 * 0.20f), Offset(center.x, center.y - size0 * 0.52f), size0 * 0.12f)
    drawLine(Color(0xFFE4C87A), Offset(center.x + size0 * 0.34f, center.y - size0 * 0.20f), Offset(center.x, center.y - size0 * 0.52f), size0 * 0.12f)
    drawCircle(Color(0xFFF1D886), size0 * 0.12f, Offset(center.x, center.y + size0 * 0.12f))
}

private fun DrawScope.drawPet(
    center: Offset,
    unit: Float,
    state: OwnerEmbodimentFrame,
    phase: Float,
) {
    val behavior = state.behavior
    val resting = behavior == EmbodiedBehavior.RESTING || behavior == EmbodiedBehavior.SLEEPING
    val walking = behavior in setOf(
        EmbodiedBehavior.APPROACHING,
        EmbodiedBehavior.EXPLORING,
        EmbodiedBehavior.RETREATING,
        EmbodiedBehavior.RESUMING,
        EmbodiedBehavior.RETRYING,
    )
    val lively = behavior in setOf(
        EmbodiedBehavior.PLAYING,
        EmbodiedBehavior.SEEKING_OWNER,
        EmbodiedBehavior.RESPONDING_TO_TOUCH,
    )
    val idleShift = if (behavior == EmbodiedBehavior.IDLING &&
        state.microMovement in setOf("weight-shift", "small-step")
    ) {
        sin(phase) * 4f
    } else {
        0f
    }
    val breath = sin(phase) * if (resting) 2.8f else 1.4f
    val gait = if (walking) sin(phase * 2f) * 8f else 0f
    val bob = when {
        behavior == EmbodiedBehavior.PLAYING -> kotlin.math.abs(sin(phase * 1.5f)) * -5f
        walking -> kotlin.math.abs(sin(phase * 2f)) * -2.5f
        else -> breath * 0.45f
    }
    val bodyColor = expressionColor(state.expression)
    val darkFur = Color(0xFF334D40)
    val cream = Color(0xFFF0D8A7)
    val outline = Color(0xFF1E3028)

    drawOval(
        Color(0x44211119),
        Offset(center.x - 92f * unit, center.y + 52f * unit),
        Size(184f * unit, 34f * unit),
    )

    withTransform({
        translate(center.x + idleShift * unit, center.y + bob * unit)
        scale(state.facing * unit, unit, Offset.Zero)
    }) {
        val tailWag = if (lively) sin(phase * 3.5f) * 24f else sin(phase) * 5f
        val tailY = if (behavior == EmbodiedBehavior.RETREATING) 34f else -2f
        val tail = Path().apply {
            moveTo(-72f, 4f)
            cubicTo(-105f, -14f + tailY, -118f, -45f + tailY, -91f, -64f + tailWag)
        }
        drawPath(tail, outline, style = Stroke(31f, cap = StrokeCap.Round))
        drawPath(tail, bodyColor, style = Stroke(23f, cap = StrokeCap.Round))
        drawCircle(cream, 9f, Offset(-91f, -64f + tailWag))

        if (!resting) {
            val frontLift = when (behavior) {
                EmbodiedBehavior.PLAYING,
                EmbodiedBehavior.SEEKING_OWNER,
                EmbodiedBehavior.RESPONDING_TO_TOUCH,
                -> -18f
                else -> 0f
            }
            drawLeg(-45f, 35f + gait, bodyColor, outline)
            drawLeg(-10f, 38f - gait, bodyColor, outline)
            drawLeg(35f, 32f - gait + frontLift, bodyColor, outline)
            drawLeg(58f, 36f + gait, bodyColor, outline)
        }

        val body = Path().apply {
            if (resting) {
                moveTo(-82f, 20f)
                cubicTo(-58f, -20f - breath, 35f, -24f - breath, 78f, 14f)
                cubicTo(60f, 48f, -55f, 52f, -82f, 20f)
            } else {
                moveTo(-76f, 22f)
                cubicTo(-72f, -34f - breath, 15f, -52f - breath, 72f, -10f)
                cubicTo(91f, 16f, 63f, 43f, 5f, 46f)
                cubicTo(-45f, 50f, -78f, 42f, -76f, 22f)
            }
            close()
        }
        drawPath(body, outline)
        withTransform({ scale(0.94f, 0.90f, Offset.Zero) }) {
            drawPath(body, bodyColor)
        }

        val headDown = behavior == EmbodiedBehavior.EATING ||
            behavior == EmbodiedBehavior.PLAYING ||
            behavior == EmbodiedBehavior.EXPLORING
        val actionHeadTilt = when (behavior) {
            EmbodiedBehavior.INSPECTING -> -11f
            EmbodiedBehavior.ORIENTING -> 5f
            EmbodiedBehavior.RESPONDING_TO_TOUCH -> -6f
            EmbodiedBehavior.RETRYING -> 8f
            EmbodiedBehavior.RESUMING -> -3f
            else -> 0f
        }
        val microHeadTilt = if (behavior == EmbodiedBehavior.IDLING &&
            state.microMovement == "head-tilt"
        ) {
            -9f
        } else {
            0f
        }
        val headTilt = actionHeadTilt + microHeadTilt
        val headCenter = when {
            resting -> Offset(60f, 9f)
            headDown -> Offset(65f, 9f)
            else -> Offset(57f, -38f)
        }

        withTransform({ rotate(headTilt, headCenter) }) {
            val earsBack = behavior == EmbodiedBehavior.RETREATING || behavior == EmbodiedBehavior.SLEEPING
            val leftEar = Path().apply {
                moveTo(headCenter.x - 39f, headCenter.y - 29f)
                lineTo(headCenter.x - if (earsBack) 54f else 28f, headCenter.y - if (earsBack) 45f else 72f)
                lineTo(headCenter.x - 5f, headCenter.y - 39f)
                close()
            }
            val rightEar = Path().apply {
                moveTo(headCenter.x + 12f, headCenter.y - 39f)
                lineTo(headCenter.x + if (earsBack) 53f else 43f, headCenter.y - if (earsBack) 50f else 72f)
                lineTo(headCenter.x + 46f, headCenter.y - 18f)
                close()
            }
            drawPath(leftEar, outline)
            drawPath(rightEar, outline)
            drawPath(leftEar, darkFur)
            drawPath(rightEar, darkFur)

            val head = Path().apply {
                moveTo(headCenter.x - 47f, headCenter.y - 22f)
                cubicTo(headCenter.x - 56f, headCenter.y + 15f, headCenter.x - 30f, headCenter.y + 45f, headCenter.x + 7f, headCenter.y + 45f)
                cubicTo(headCenter.x + 53f, headCenter.y + 43f, headCenter.x + 62f, headCenter.y + 4f, headCenter.x + 42f, headCenter.y - 28f)
                cubicTo(headCenter.x + 17f, headCenter.y - 48f, headCenter.x - 27f, headCenter.y - 45f, headCenter.x - 47f, headCenter.y - 22f)
                close()
            }
            drawPath(head, outline)
            withTransform({ scale(0.94f, 0.93f, headCenter) }) { drawPath(head, bodyColor) }

            drawOval(
                cream,
                Offset(headCenter.x - 4f, headCenter.y + 8f),
                Size(50f, 31f),
            )
            drawCircle(outline, 6f, Offset(headCenter.x + 35f, headCenter.y + 20f))

            val sleeping = behavior == EmbodiedBehavior.SLEEPING
            val blinking = behavior == EmbodiedBehavior.IDLING &&
                state.microMovement == "slow-blink" && cos(phase) > 0f
            val wary = behavior == EmbodiedBehavior.RETREATING
            if (sleeping || blinking) {
                drawLine(outline, Offset(headCenter.x - 21f, headCenter.y + 1f), Offset(headCenter.x - 7f, headCenter.y + 3f), 3.5f, StrokeCap.Round)
                drawLine(outline, Offset(headCenter.x + 10f, headCenter.y + 3f), Offset(headCenter.x + 23f, headCenter.y + 1f), 3.5f, StrokeCap.Round)
            } else {
                val eyeY = if (wary) -1f else 0f
                drawOval(outline, Offset(headCenter.x - 22f, headCenter.y - 5f + eyeY), Size(9f, 13f))
                drawOval(outline, Offset(headCenter.x + 10f, headCenter.y - 5f + eyeY), Size(9f, 13f))
                drawCircle(Color(0xFFF8E9BD), 2.2f, Offset(headCenter.x - 18f, headCenter.y - 1f + eyeY))
                drawCircle(Color(0xFFF8E9BD), 2.2f, Offset(headCenter.x + 14f, headCenter.y - 1f + eyeY))
            }

            val mouthOpen = behavior == EmbodiedBehavior.VOCALIZING || behavior == EmbodiedBehavior.EATING
            if (mouthOpen) {
                drawOval(Color(0xFF3A2525), Offset(headCenter.x + 12f, headCenter.y + 25f), Size(18f, 13f))
                drawOval(Color(0xFFE88D8B), Offset(headCenter.x + 16f, headCenter.y + 31f), Size(10f, 6f))
            } else {
                drawArc(
                    outline,
                    20f,
                    120f,
                    false,
                    Offset(headCenter.x + 8f, headCenter.y + 21f),
                    Size(25f, 16f),
                    style = Stroke(2.8f, cap = StrokeCap.Round),
                )
            }

            if (behavior == EmbodiedBehavior.VOCALIZING || state.vocalizing) {
                drawArc(Color(0xFFF5E6B2), -45f, 90f, false, Offset(headCenter.x + 48f, headCenter.y - 8f), Size(28f, 28f), style = Stroke(3f, cap = StrokeCap.Round))
                drawArc(Color(0xFFF5E6B2), -45f, 90f, false, Offset(headCenter.x + 54f, headCenter.y - 15f), Size(42f, 42f), style = Stroke(2.4f, cap = StrokeCap.Round))
            }
        }

        if (behavior == EmbodiedBehavior.SLEEPING) {
            drawTextMark("z", Offset(92f, -55f), 17f, Color(0xFFDCE8D5))
            drawTextMark("z", Offset(108f, -78f), 13f, Color(0xA8DCE8D5))
        }
    }
}

private fun DrawScope.drawLeg(x: Float, y: Float, bodyColor: Color, outline: Color) {
    drawRoundRect(outline, Offset(x - 12f, y - 5f), Size(24f, 49f), CornerRadius(11f))
    drawRoundRect(bodyColor, Offset(x - 9f, y - 3f), Size(18f, 43f), CornerRadius(9f))
    drawOval(Color(0xFFF0D8A7), Offset(x - 11f, y + 31f), Size(25f, 12f))
}

/** Tiny canvas glyph used only for sleep; no diagnostic or action label is shown. */
private fun DrawScope.drawTextMark(mark: String, at: Offset, radius: Float, color: Color) {
    if (mark != "z") return
    drawLine(color, Offset(at.x - radius * 0.35f, at.y - radius * 0.45f), Offset(at.x + radius * 0.35f, at.y - radius * 0.45f), radius * 0.12f, StrokeCap.Round)
    drawLine(color, Offset(at.x + radius * 0.35f, at.y - radius * 0.45f), Offset(at.x - radius * 0.35f, at.y + radius * 0.45f), radius * 0.12f, StrokeCap.Round)
    drawLine(color, Offset(at.x - radius * 0.35f, at.y + radius * 0.45f), Offset(at.x + radius * 0.35f, at.y + radius * 0.45f), radius * 0.12f, StrokeCap.Round)
}

private fun expressionColor(expression: SpikeExpressionContract.Expression): Color = when (expression) {
    SpikeExpressionContract.Expression.NEUTRAL -> Color(0xFF9BBE91)
    SpikeExpressionContract.Expression.ALERT -> Color(0xFFA8C99A)
    SpikeExpressionContract.Expression.CONTENT -> Color(0xFFB1D5A1)
    SpikeExpressionContract.Expression.WARY -> Color(0xFF91AA84)
    SpikeExpressionContract.Expression.TIRED -> Color(0xFF829C7B)
    SpikeExpressionContract.Expression.EXCITED -> Color(0xFFC1DDA5)
    SpikeExpressionContract.Expression.WITHDRAWN -> Color(0xFF728A72)
}
