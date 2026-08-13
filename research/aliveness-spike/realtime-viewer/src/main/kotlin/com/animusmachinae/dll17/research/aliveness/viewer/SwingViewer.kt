package com.animusmachinae.dll17.research.aliveness.viewer

import com.animusmachinae.dll17.research.aliveness.Cohort
import com.animusmachinae.dll17.research.aliveness.HabitatObject
import com.animusmachinae.dll17.research.aliveness.InteractionKind
import com.animusmachinae.dll17.research.aliveness.ObjectKind
import com.animusmachinae.dll17.research.aliveness.SpikeContract
import com.animusmachinae.dll17.research.aliveness.SpikeExpressionContract
import java.awt.BasicStroke
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridLayout
import java.awt.RenderingHints
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.Timer

/**
 * The crude real-time observer viewer.
 *
 * It is deliberately plain. The canonical requirement is that every cohort share
 * the exact same appearance, expression vocabulary, gaze mapping, idle library,
 * camera, layout and timing — so the honest way to build it is to draw entirely
 * from `ExpressionFrame` and give the renderer no other input at all. This class
 * has no reference to `Cohort`, to any mechanism, or to a decision trace.
 */
public class CreaturePanel(private val session: ViewerSession) : JPanel() {

    init {
        preferredSize = Dimension(560, 420)
        background = Color(0x1B1B20)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val frame = session.frame
        drawHabitat(g2)
        drawCreature(g2, frame)
    }

    private fun drawHabitat(g2: Graphics2D) {
        val objects = session.presentObjects()
        val radius = 150
        val cx = width / 2
        val cy = height / 2 + 20
        g2.font = Font(Font.SANS_SERIF, Font.PLAIN, 10)
        for ((index, obj) in objects.withIndex()) {
            val angle = 2.0 * Math.PI * index / objects.size - Math.PI / 2
            val x = cx + (StrictMath.cos(angle) * radius).toInt()
            val y = cy + (StrictMath.sin(angle) * radius * 0.7).toInt()
            g2.color = colorFor(obj)
            g2.fillRoundRect(x - 14, y - 10, 28, 20, 6, 6)
            g2.color = Color(0xB0, 0xB0, 0xB8)
            val label = obj.name.substringBefore('_').take(6)
            g2.drawString(label, x - g2.fontMetrics.stringWidth(label) / 2, y + 24)

            if (session.frame.attentionObject == obj) {
                g2.color = Color(0xF0, 0xD0, 0x60)
                g2.stroke = BasicStroke(2f)
                g2.drawRoundRect(x - 17, y - 13, 34, 26, 8, 8)
            }
        }
    }

    private fun colorFor(obj: HabitatObject): Color = when (obj.kind) {
        ObjectKind.RESOURCE -> Color(0x4C, 0x8C, 0x4A)
        ObjectKind.SHELTER -> Color(0x6B, 0x5B, 0x3E)
        ObjectKind.SOCIAL -> Color(0x46, 0x6C, 0x9C)
        ObjectKind.PLAY -> Color(0x8A, 0x5C, 0x9E)
        ObjectKind.AVERSIVE -> Color(0x9C, 0x46, 0x46)
        ObjectKind.BLOCKED -> Color(0x50, 0x50, 0x55)
        ObjectKind.NOVELTY -> Color(0xC0, 0x94, 0x30)
    }

    private fun drawCreature(g2: Graphics2D, frame: SpikeExpressionContract.ExpressionFrame) {
        val cx = width / 2
        val cy = height / 2 + 20
        val amplitude = frame.motionAmplitude.toDouble() / 1_000_000.0

        // Posture governs the body's proportions and offset; micro-movement adds
        // the small idle displacement. Both come from the shared contract.
        val (bodyW, bodyH, dy) = when (frame.posture) {
            SpikeExpressionContract.Posture.STAND -> Triple(70, 64, 0)
            SpikeExpressionContract.Posture.CROUCH -> Triple(80, 48, 10)
            SpikeExpressionContract.Posture.LEAN_IN -> Triple(74, 60, -6)
            SpikeExpressionContract.Posture.LEAN_AWAY -> Triple(66, 60, 8)
            SpikeExpressionContract.Posture.LIE_DOWN -> Triple(92, 36, 18)
            SpikeExpressionContract.Posture.PLAY_BOW -> Triple(78, 54, 6)
        }
        val jitter = (MICRO_OFFSETS[frame.microMovement] ?: 0)
        val wobble = (amplitude * 6.0).toInt()

        g2.color = expressionColor(frame.expression)
        g2.fillOval(cx - bodyW / 2 + jitter, cy - bodyH / 2 + dy + wobble, bodyW, bodyH)

        // Gaze: the head leans toward the attention target's bearing.
        val objects = session.presentObjects()
        val gazeIndex = objects.indexOf(frame.gazeTarget)
        val gazeAngle = if (gazeIndex >= 0) {
            2.0 * Math.PI * gazeIndex / objects.size - Math.PI / 2
        } else {
            -Math.PI / 2
        }
        val hx = cx + (StrictMath.cos(gazeAngle) * 22).toInt() + jitter
        val hy = cy + dy + (StrictMath.sin(gazeAngle) * 12).toInt() - bodyH / 2
        g2.fillOval(hx - 18, hy - 16, 36, 32)

        g2.color = Color(0x10, 0x10, 0x14)
        val eyeOpen = if (frame.expression == SpikeExpressionContract.Expression.TIRED) 2 else 5
        g2.fillOval(hx - 9, hy - 4, 5, eyeOpen)
        g2.fillOval(hx + 4, hy - 4, 5, eyeOpen)

        if (frame.vocalizing) {
            g2.color = Color(0xE8, 0xE0, 0xC0)
            g2.stroke = BasicStroke(2f)
            g2.drawArc(hx + 14, hy - 18, 20, 20, -30, 90)
            g2.drawArc(hx + 18, hy - 24, 30, 30, -30, 90)
        }
    }

    private fun expressionColor(e: SpikeExpressionContract.Expression): Color = when (e) {
        SpikeExpressionContract.Expression.NEUTRAL -> Color(0xC8, 0xC4, 0xBC)
        SpikeExpressionContract.Expression.ALERT -> Color(0xDA, 0xD2, 0xA8)
        SpikeExpressionContract.Expression.CONTENT -> Color(0xBC, 0xD8, 0xB4)
        SpikeExpressionContract.Expression.WARY -> Color(0xD6, 0xB0, 0xA0)
        SpikeExpressionContract.Expression.TIRED -> Color(0x9C, 0x9C, 0xA8)
        SpikeExpressionContract.Expression.EXCITED -> Color(0xF0, 0xCE, 0x88)
        SpikeExpressionContract.Expression.WITHDRAWN -> Color(0x8C, 0x8C, 0x96)
    }

    private companion object {
        /** Small deterministic displacements, shared by every cohort. */
        val MICRO_OFFSETS: Map<String, Int> =
            SpikeExpressionContract.MICRO_MOVEMENTS.withIndex().associate { (i, name) ->
                name to (i % 5) - 2
            }
    }
}

/** The observation window: creature, allowed interactions, and a session clock. */
public class ViewerWindow(private val session: ViewerSession) : JFrame() {

    private val clock = JLabel("", SwingConstants.CENTER)
    private val panel = CreaturePanel(session)

    init {
        title = session.displayLabel
        defaultCloseOperation = DISPOSE_ON_CLOSE
        layout = BorderLayout()

        val header = JLabel(session.displayLabel, SwingConstants.CENTER)
        header.font = Font(Font.SANS_SERIF, Font.BOLD, 16)
        add(header, BorderLayout.NORTH)
        add(panel, BorderLayout.CENTER)

        val controls = JPanel(GridLayout(2, 4, 4, 4))
        controls.add(button("Touch", InteractionKind.TOUCH, HabitatObject.PERSON_ALPHA))
        controls.add(button("Call", InteractionKind.CALL, HabitatObject.PERSON_ALPHA))
        controls.add(button("Offer food", InteractionKind.OFFER_FOOD, HabitatObject.FOOD_TROUGH))
        controls.add(button("Show ball", InteractionKind.PRESENT_OBJECT, HabitatObject.PLAY_BALL))
        controls.add(button("Show cube", InteractionKind.PRESENT_OBJECT, HabitatObject.PLAY_CUBE))
        controls.add(button("Look away", InteractionKind.WITHDRAW_ATTENTION, null))
        controls.add(button("Startle", InteractionKind.STARTLE, HabitatObject.AVERSIVE_BUZZER))
        controls.add(clock)

        add(controls, BorderLayout.SOUTH)
        pack()
        setLocationRelativeTo(null)
    }

    private fun button(label: String, kind: InteractionKind, target: HabitatObject?): JButton {
        val b = JButton(label)
        b.addActionListener { session.submit(kind, target) }
        return b
    }

    /** Starts the fixed-duration session. Timing policy is identical per cohort. */
    public fun start() {
        isVisible = true
        val timer = Timer(SpikeContract.VIEWER_TICK_MILLIS) { event ->
            session.advance()
            val remaining = (session.durationSeconds * 1_000L - session.elapsedMillis) / 1_000L
            clock.text = "${maxOf(0L, remaining)}s"
            panel.repaint()
            if (session.complete) {
                (event.source as Timer).stop()
                clock.text = "session complete"
            }
        }
        timer.start()
    }
}

/**
 * Entry point.
 *
 * `--pair` runs the standardized live paired session for the primary endpoint.
 * A single cohort may be launched by name for engineering inspection only; that
 * mode is not a study configuration and prints so.
 */
public object ViewerMain {
    @JvmStatic
    public fun main(args: Array<String>) {
        val seed = args.firstOrNull { it.startsWith("--seed=") }
            ?.removePrefix("--seed=")?.toLongOrNull() ?: 20260813L
        val durationSeconds = args.firstOrNull { it.startsWith("--seconds=") }
            ?.removePrefix("--seconds=")?.toIntOrNull() ?: SpikeContract.PRIMARY_SESSION_SECONDS
        val orderIndex = args.firstOrNull { it.startsWith("--order=") }
            ?.removePrefix("--order=")?.toIntOrNull() ?: 0

        if (args.any { it == "--pair" }) {
            val pair = PairedSession(
                pairId = "PAIR-$seed-$orderIndex",
                firstCohort = Cohort.FULL,
                secondCohort = Cohort.SCRIPTED_PET_BASELINE,
                seed = seed,
                orderIndex = orderIndex,
                durationSeconds = durationSeconds,
            )
            println("Blinded paired session. Order index $orderIndex. No cohort information is shown.")
            ViewerWindow(pair.first).start()
            ViewerWindow(pair.second).start()
            return
        }

        val name = args.firstOrNull { it.startsWith("--cohort=") }?.removePrefix("--cohort=")
        val cohort = Cohort.entries.firstOrNull { it.name == name } ?: Cohort.FULL
        println(
            "Engineering inspection mode: cohort ${cohort.cohortId} is named on this console " +
                "and this configuration is NOT a valid study session.",
        )
        ViewerWindow(
            ViewerSession(
                sessionId = "INSPECT-${cohort.name}",
                displayLabel = "Creature",
                cohort = cohort,
                seed = seed,
                durationSeconds = durationSeconds,
            ),
        ).start()
    }
}
