package com.animusmachinae.dll17.research.aliveness

import com.animusmachinae.dll17.core.math.FixedPoint

/**
 * A000.2 abstract habitat.
 *
 * No Android, persistence, Torpor/ICU, spatial simulation, AR, NavMesh or
 * pathfinding. Objects are stable abstract affordances; "location" is
 * proximity to one object at a time, which is the least spatial model that
 * still lets approach, withdraw and commitment mean anything.
 */
public enum class HabitatObject(
    public val ordinal0: Int,
    public val kind: ObjectKind,
) {
    FOOD_TROUGH(0, ObjectKind.RESOURCE),
    FOOD_CACHE(1, ObjectKind.RESOURCE),
    SHELTER(2, ObjectKind.SHELTER),
    PERSON_ALPHA(3, ObjectKind.SOCIAL),
    PERSON_BETA(4, ObjectKind.SOCIAL),
    PLAY_BALL(5, ObjectKind.PLAY),
    PLAY_CUBE(6, ObjectKind.PLAY),
    PLAY_CHIME(7, ObjectKind.PLAY),
    PLAY_MIRROR(8, ObjectKind.PLAY),
    AVERSIVE_BUZZER(9, ObjectKind.AVERSIVE),
    SEALED_GATE(10, ObjectKind.BLOCKED),
    NOVEL_SLOT(11, ObjectKind.NOVELTY);

    public companion object {
        public val ALL: List<HabitatObject> = entries.toList()
        public const val COUNT: Int = 12
    }
}

public enum class ObjectKind { RESOURCE, SHELTER, SOCIAL, PLAY, AVERSIVE, BLOCKED, NOVELTY }

public enum class Safety { SAFE, UNSAFE, BLOCKED }

/**
 * The action vocabulary. Canonical §6 asks for roughly 12–20 actions; this is
 * 15, with idle expressed as three distinguishable variations so a viewer can
 * show idle variety without the controller treating them as one action.
 */
public enum class SpikeAction(
    public val objectDirected: Boolean,
    public val tier: Int,
) {
    OBSERVE(true, 4),
    ORIENT(true, 4),
    APPROACH(true, 3),
    /** Promoted to Tier 0 by the controller only on real or learned danger. */
    WITHDRAW(true, 4),
    SEEK_INTERACTION(true, 3),
    RESPOND_TO_TOUCH(true, 2),
    VOCALIZE(true, 3),
    EXPLORE(true, 4),
    PLAY(true, 4),
    EAT(true, 3),
    REST(false, 3),
    SLEEP(false, 3),
    RESUME_INTERRUPTED(false, 2),
    RETRY(true, 3),
    IDLE_VARIATION(false, 5);

    public companion object {
        public val ALL: List<SpikeAction> = entries.toList()
    }
}

/** A normalized user input. The viewer emits these; the accelerated sim scripts them. */
public data class InteractionEvent(
    public val tick: Long,
    public val kind: InteractionKind,
    public val target: HabitatObject?,
    public val personId: HabitatObject?,
)

/** One bounded salient event awaiting a visible response. */
public data class PendingStimulus(
    public val kind: InteractionKind,
    public val target: HabitatObject?,
    public val arrivalTick: Long,
) {
    public fun activeAt(tick: Long): Boolean =
        tick - arrivalTick in 0L..SpikeContract.PENDING_STIMULUS_LIFETIME_TICKS
}

public enum class InteractionKind {
    TOUCH,
    CALL,
    OFFER_FOOD,
    PRESENT_OBJECT,
    WITHDRAW_ATTENTION,
    STARTLE,
}

/**
 * Habitat state. Deterministic: the only randomness is the novelty schedule,
 * drawn from its own substream so it can never perturb organism tie-breaking.
 */
public class Habitat(
    public val seed: Long,
    public val condition: HabitatCondition,
) {
    private val novelty = SpikeRandom(seed, SpikeRandomDomain.HABITAT_NOVELTY)

    /** Present objects. `NOVEL_SLOT` appears and disappears under `condition`. */
    private val present = BooleanArray(HabitatObject.COUNT) { true }

    /** Ticks at which a controlled novelty event last occurred, per object. */
    public val lastChangeTick: LongArray = LongArray(HabitatObject.COUNT) { Long.MIN_VALUE / 4 }

    public var novelSlotIdentity: Int = 0
        private set

    init {
        present[HabitatObject.NOVEL_SLOT.ordinal0] = false
    }

    public fun isPresent(o: HabitatObject): Boolean = present[o.ordinal0]

    public fun safetyOf(o: HabitatObject): Safety = when (o.kind) {
        ObjectKind.BLOCKED -> Safety.BLOCKED
        ObjectKind.AVERSIVE -> Safety.UNSAFE
        else -> if (present[o.ordinal0]) Safety.SAFE else Safety.BLOCKED
    }

    /** Valid affordances for an object. Invalid affordances invalidate proposals. */
    public fun affordances(o: HabitatObject): Set<SpikeAction> =
        if (present[o.ordinal0]) AFFORDANCES_BY_KIND.getValue(o.kind) else emptySet()

    private companion object {
        /**
         * Precomputed once. Rebuilding these sets per object per tick was a
         * measurable share of simulator runtime for no expressive benefit.
         */
        val AFFORDANCES_BY_KIND: Map<ObjectKind, Set<SpikeAction>> = buildAffordances()

        fun buildAffordances(): Map<ObjectKind, Set<SpikeAction>> = ObjectKind.entries.associateWith { kind ->
        when (kind) {
            ObjectKind.RESOURCE -> setOf(
                SpikeAction.OBSERVE, SpikeAction.ORIENT, SpikeAction.APPROACH,
                SpikeAction.EAT, SpikeAction.EXPLORE, SpikeAction.RETRY,
            )
            ObjectKind.SHELTER -> setOf(
                SpikeAction.OBSERVE, SpikeAction.ORIENT, SpikeAction.APPROACH,
                SpikeAction.EXPLORE,
            )
            // RESPOND_TO_TOUCH is deliberately absent: it is a reaction to a
            // normalized user input, not something the organism may propose for
            // itself. Leaving it in the affordance set would give every social
            // object a permanently active Tier 2 candidate.
            ObjectKind.SOCIAL -> setOf(
                SpikeAction.OBSERVE, SpikeAction.ORIENT, SpikeAction.APPROACH,
                SpikeAction.SEEK_INTERACTION, SpikeAction.VOCALIZE,
                SpikeAction.PLAY, SpikeAction.WITHDRAW,
            )
            ObjectKind.PLAY, ObjectKind.NOVELTY -> setOf(
                SpikeAction.OBSERVE, SpikeAction.ORIENT, SpikeAction.APPROACH,
                SpikeAction.EXPLORE, SpikeAction.PLAY, SpikeAction.WITHDRAW,
            )
            // EXPLORE is present for the same reason the aversive object punishes
            // attention: without a comparably attractive affordance the organism
            // simply never engages it, and the avoidance arm has nothing to
            // condition on.
            ObjectKind.AVERSIVE -> setOf(
                SpikeAction.OBSERVE, SpikeAction.ORIENT, SpikeAction.WITHDRAW,
                SpikeAction.APPROACH, SpikeAction.EXPLORE,
            )
            ObjectKind.BLOCKED -> setOf(SpikeAction.OBSERVE, SpikeAction.RETRY)
        }
        }
    }

    /**
     * Advance the habitat. In `STATIC` the habitat never changes after tick zero,
     * which is exactly the behavioural-death fixture canonical §9 demands.
     */
    public fun advance(tick: Long) {
        when (condition) {
            HabitatCondition.STATIC -> Unit

            HabitatCondition.CONTROLLED_NOVELTY -> {
                val period = 3L * SpikeContract.TICKS_PER_VIRTUAL_DAY
                if (tick > 0L && tick % period == 0L) {
                    val slot = HabitatObject.NOVEL_SLOT.ordinal0
                    present[slot] = !present[slot]
                    if (present[slot]) novelSlotIdentity = novelty.nextInt(1 shl 20)
                    lastChangeTick[slot] = tick
                }
            }

            HabitatCondition.SHIFTING_CONTEXT -> {
                val period = 2L * SpikeContract.TICKS_PER_VIRTUAL_DAY
                if (tick > 0L && tick % period == 0L) {
                    val target = HabitatObject.ALL[novelty.nextInt(HabitatObject.COUNT)]
                    if (target.kind == ObjectKind.PLAY) lastChangeTick[target.ordinal0] = tick
                }
            }
        }
    }

    /** True when a qualifying causal change happened recently enough to matter. */
    public fun recentCausalChange(o: HabitatObject, tick: Long): Boolean =
        tick - lastChangeTick[o.ordinal0] in 0L until SpikeContract.TICKS_PER_VIRTUAL_HOUR.toLong()

    /** Circadian phase in `[0, 1)`, from logical time only. */
    public fun circadianPhase(tick: Long): Long =
        Math.floorMod(tick, SpikeContract.TICKS_PER_VIRTUAL_DAY.toLong()) *
            FixedPoint.SCALE / SpikeContract.TICKS_PER_VIRTUAL_DAY

    /** Night is the half of the cycle where rest is circadian-aligned. */
    public fun isNight(tick: Long): Boolean =
        circadianPhase(tick) >= FixedPoint.of(0L, 750_000L) ||
            circadianPhase(tick) < FixedPoint.of(0L, 250_000L)
}

public enum class HabitatCondition {
    /** No new objects, no changes. The canonical behavioural-death fixture. */
    STATIC,

    /** An object appears and disappears on a fixed schedule. */
    CONTROLLED_NOVELTY,

    /** Context shifts without new objects. */
    SHIFTING_CONTEXT,
}
