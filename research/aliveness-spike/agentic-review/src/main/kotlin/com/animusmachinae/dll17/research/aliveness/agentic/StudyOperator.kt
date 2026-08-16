package com.animusmachinae.dll17.research.aliveness.agentic

/**
 * A participant response, as *recorded* by the operator.
 *
 * The operator can only ever be a scribe. [sourceAttestation] names the human
 * act the record came from; there is no factory in this package that produces
 * one without being handed it, and no operator method returns a
 * [ParticipantResponse] it was not given.
 */
public class ParticipantResponse(
    public val participantId: String,
    public val sessionOrder: List<String>,
    public val sourceAttestation: String,
)

/** D016-J eligibility and pre-session protections, expressed as attestations. */
public data class ParticipantEligibility(
    public val attestsUsAdult18Plus: Boolean,
    public val canProvideOwnConsent: Boolean,
    public val isPrisoner: Boolean,
    public val ownerContactSupplied: Boolean,
    public val compensationTermsDisclosed: Boolean,
)

/** Validates the approved pre-session scope without collecting exact age or demographics. */
public object ParticipantEligibilityGate {
    public fun authorize(eligibility: ParticipantEligibility) {
        require(eligibility.attestsUsAdult18Plus) {
            "a U.S. adult 18+ attestation is required"
        }
        require(eligibility.canProvideOwnConsent) { "legally effective self-consent is required" }
        require(!eligibility.isPrisoner) { "prisoners are excluded from this protocol version" }
        require(eligibility.ownerContactSupplied) {
            "a real study-owner contact must be supplied before the session begins"
        }
        require(eligibility.compensationTermsDisclosed) {
            "compensation terms must be disclosed before consent"
        }
    }
}

/** One line of the operator's append-only audit log. */
public class AuditEntry(public val sequence: Int, public val action: String, public val detail: String)

/**
 * `IndependentAgenticStudyOperatorV1`.
 *
 * Administers the protocol and nothing else. The authority boundary is enforced
 * two ways at once: [refuseIfBeyondAuthority] rejects any attempt to use a
 * capability the role contract does not hold, and the class deliberately exposes
 * no operation that returns participant data it was not given, so "the operator
 * cannot fabricate human evidence" is a property of its surface rather than a
 * promise in a comment. `StudyOperatorBoundaryTest` asserts that surface.
 */
public class StudyOperator(
    public val contract: RoleContract = AgenticRoleContracts.STUDY_OPERATOR,
) {
    private val log = mutableListOf<AuditEntry>()
    private val recorded = mutableListOf<ParticipantResponse>()
    private var sealed = false

    /** Deterministic counterbalancing: odd participants see FULL first, even see the comparator. */
    public fun assignOrder(participantIndex: Int, full: String, comparator: String): List<String> {
        require(!sealed) { "the operator cannot alter assignment after sealing" }
        val order = if (participantIndex % 2 == 1) listOf(full, comparator) else listOf(comparator, full)
        append("ASSIGN_ORDER", "participant=$participantIndex order=${order.joinToString(">")}")
        return order
    }

    /** Records what a human actually did. The attestation is supplied, never invented. */
    public fun record(response: ParticipantResponse) {
        require(!sealed) { "the operator cannot add records after sealing" }
        require(response.sourceAttestation.isNotBlank()) {
            "a participant record without a human attestation is not evidence"
        }
        recorded += response
        append("RECORD", "participant=${response.participantId} source=${response.sourceAttestation}")
    }

    /** Seals the evidence. After this the operator can only read and log. */
    public fun seal(): String {
        require(!sealed) { "already sealed" }
        sealed = true
        val digest = sha256(recorded.joinToString("\n") { "${it.participantId}|${it.sourceAttestation}" })
        append("SEAL", "records=${recorded.size} digest=$digest")
        return digest
    }

    /**
     * The only thing the operator may pass on from the variance pilot.
     *
     * Mirrors the frozen `BlindVariancePilot.PilotRelease` restriction: the
     * dispersion and whether the protocol held, never the direction.
     */
    public fun releaseVariancePilot(pairedDifferenceSd: Double, protocolValid: Boolean): String {
        append("PILOT_RELEASE", "fields=pairedDifferenceSd,protocolValid")
        return "pairedDifferenceSd=$pairedDifferenceSd protocolValid=$protocolValid"
    }

    /** Refuses any action outside the operator's frozen authority. */
    public fun refuseIfBeyondAuthority(action: Authority): String? =
        if (action in contract.authorities) {
            null
        } else {
            "REFUSED: ${contract.role.roleId} does not hold ${action.name}"
        }

    public fun auditLog(): List<AuditEntry> = log.toList()

    public fun recordCount(): Int = recorded.size

    private fun append(action: String, detail: String) {
        log += AuditEntry(log.size + 1, action, detail)
    }
}
