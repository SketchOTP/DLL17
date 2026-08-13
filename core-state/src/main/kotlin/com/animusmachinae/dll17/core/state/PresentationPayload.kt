package com.animusmachinae.dll17.core.state

/**
 * The assisted-payload zero-physics invariant, per Implementation Plan E2E work
 * package R001.8.
 *
 * ```
 * for every AssistedPayload:
 *     dynamicRigidBody == false
 *     collisionParticipation == false
 *     canonicalDoseEffect does not depend on render-path physics
 * ```
 *
 * The design goal is that this cannot be violated by a future author who has not
 * read the contract. So it is not expressed as a rule, a test or a review
 * checklist: [AssistedPayload] declares both properties `final` at `false`, and
 * Kotlin will not compile a subclass that overrides them. A payload that
 * participated in the solver would have to stop being an `AssistedPayload`
 * first, which is a visible structural change rather than a quiet one.
 *
 * Assisted care itself does not exist yet. This is the interface-level invariant
 * arriving before the mechanism that needs it, which is the order the
 * architecture asks for.
 */

/** Anything the presentation layer can be asked to show. */
public interface PresentationPayload {
    public val payloadKindOrdinal: Int

    /** True if the physics solver may treat this payload as a dynamic body. */
    public val dynamicRigidBody: Boolean

    /** True if this payload may generate collision events. */
    public val collisionParticipation: Boolean
}

/**
 * Base class for every assisted-care payload.
 *
 * Both physics properties are `final` and `false`. This is the enforcement
 * mechanism, not documentation of an intention.
 */
public abstract class AssistedPayload(
    final override val payloadKindOrdinal: Int,
) : PresentationPayload {

    final override val dynamicRigidBody: Boolean = false

    final override val collisionParticipation: Boolean = false

    /**
     * The canonical effect of this payload, in fixed-point units.
     *
     * It is supplied as data, never computed from render-path state. That is the
     * third clause of the invariant: a dose whose magnitude depended on where a
     * renderer decided the payload landed would make canonical state a function
     * of frame timing.
     */
    public abstract val canonicalDoseEffect: Long

    /**
     * A noncolliding kinematic proxy is permitted as presentation state. It may
     * not produce solver impulses or canonical collision evidence, which is
     * structurally true here because this class exposes no impulse or collision
     * surface at all.
     */
    public open val kinematicPresentationProxy: Boolean = true
}

/**
 * The R001 mechanism-proof payload.
 *
 * It carries no care semantics — dose *meaning* belongs to the phase that owns
 * assisted care. It exists so the invariant has a concrete instance to be
 * verified against.
 */
public class QualificationAssistedPayload(
    override val canonicalDoseEffect: Long,
) : AssistedPayload(PAYLOAD_KIND_ORDINAL) {
    public companion object {
        public const val PAYLOAD_KIND_ORDINAL: Int = 1
    }
}
