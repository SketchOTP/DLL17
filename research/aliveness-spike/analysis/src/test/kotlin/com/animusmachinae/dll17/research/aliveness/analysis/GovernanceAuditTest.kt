package com.animusmachinae.dll17.research.aliveness.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GovernanceAuditTest {

    private val items = AlivenessGovernanceAudit.audit()

    @Test
    fun `the audit covers every canonical activation prerequisite`() {
        assertEquals(27, items.size)
        assertEquals(items.size, items.map { it.id }.distinct().size)
    }

    @Test
    fun `no item claims PASS on the strength of human evidence that does not exist`() {
        // GA-24 is deliberately absent: the owner's resource ceiling is a human
        // decision that now actually exists, so it is the one item in this family
        // that may legitimately pass. Every other item here still depends on
        // evidence nobody has produced.
        val humanDependent = setOf("GA-04", "GA-05", "GA-06", "GA-10", "GA-15", "GA-16")
        for (item in items.filter { it.id in humanDependent }) {
            assertTrue(
                item.state != AuditState.PASS,
                "${item.id} claims PASS but depends on evidence that does not exist",
            )
        }
    }

    @Test
    fun `every blocked item names its exact blocking state and no other item does`() {
        for (item in items) {
            if (item.state == AuditState.BLOCKED) {
                val blocking = item.blockingState
                assertNotNull(blocking, "${item.id} is BLOCKED but names no state")
                assertTrue(
                    blocking.startsWith("BLOCKED_"),
                    "${item.id} blocking state is not a blocking token",
                )
            } else {
                assertNull(item.blockingState, "${item.id} is not BLOCKED but names a state")
            }
        }
    }

    @Test
    fun `activation is blocked and recruitment is refused while any prerequisite is missing`() {
        assertFalse(AlivenessGovernanceAudit.recruitmentPermitted(items))
        assertEquals(
            "BLOCKED_BASELINE_NOT_INDEPENDENTLY_QUALIFIED",
            AlivenessGovernanceAudit.activationState(items),
        )
        val rendered = AlivenessGovernanceAudit.render(items)
        assertTrue(rendered.contains("HUMAN_SCORED_RECRUITMENT=BLOCKED"))
        assertTrue(rendered.contains("A001_PROGRAM_STATE=ALIVENESS_UNTESTED"))
    }

    @Test
    fun `the outstanding blockers are exactly the ones with no repository level fix`() {
        assertEquals(
            listOf(
                "BLOCKED_BASELINE_NOT_INDEPENDENTLY_QUALIFIED",
                "BLOCKED_VARIANCE_PILOT_NOT_REGISTERED",
                "BLOCKED_SPEC_PAIRED_DIFFERENCE_SD",
                "BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED",
            ),
            AlivenessGovernanceAudit.blockers(items),
        )
    }

    @Test
    fun `the activation state is derived from the items and cannot be declared separately`() {
        // Remove every blocking item and the gate opens; leave one and it does not.
        val unblocked = items.filter { it.state != AuditState.BLOCKED }
        assertEquals("A001_READY_FOR_ACTIVATION", AlivenessGovernanceAudit.activationState(unblocked))
        assertTrue(AlivenessGovernanceAudit.recruitmentPermitted(unblocked))

        val oneLeft = unblocked + items.first { it.id == "GA-15" }
        assertEquals(
            "BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED",
            AlivenessGovernanceAudit.activationState(oneLeft),
        )
        assertFalse(AlivenessGovernanceAudit.recruitmentPermitted(oneLeft))
    }

    @Test
    fun `the roster item stays blocked and carries no placeholder name`() {
        val roster = items.single { it.id == "GA-15" }
        assertEquals(AuditState.BLOCKED, roster.state)
        assertEquals("BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED", roster.blockingState)
        assertTrue(roster.detail.contains("unassigned"))
    }

    @Test
    fun `the owner resource ceiling item is read from the frozen value`() {
        val ceiling = A001FeasibilityBudget.FROZEN_OWNER_CEILING
        assertNotNull(ceiling, "the owner ceiling is frozen, so GA-24 must have a value to read")
        assertEquals(400, ceiling.maxFundableParticipants)
        assertEquals(250.0, ceiling.maxParticipantHours)

        val budget = items.single { it.id == "GA-24" }
        assertEquals(AuditState.PASS, budget.state)
        assertNull(budget.blockingState)
        // The detail must carry the actual frozen numbers, so the audit cannot
        // report a ceiling that disagrees with the one the calculator uses.
        assertTrue(budget.detail.contains("maxFundableParticipants=400"))
        assertTrue(budget.detail.contains("maxParticipantHours=250.000"))
    }

    @Test
    fun `the frozen ceiling is consistent with the frozen per-participant schedule`() {
        val ceiling = A001FeasibilityBudget.FROZEN_OWNER_CEILING
        assertNotNull(ceiling)
        // 400 participants at the frozen schedule is 246.667 participant-hours,
        // so the participant count binds first. If the schedule ever lengthens
        // enough to invert that, the owner is being held to a ceiling that no
        // longer means what was decided, and this fails.
        val hoursAtParticipantCeiling =
            ceiling.maxFundableParticipants.toDouble() *
                A001FeasibilityBudget.PARTICIPANT_SECONDS / 3600.0
        assertTrue(
            hoursAtParticipantCeiling <= ceiling.maxParticipantHours,
            "the participant ceiling implies $hoursAtParticipantCeiling hours, " +
                "which exceeds the frozen hour ceiling",
        )
    }

    @Test
    fun `the audit records that no external ethical approval is claimed`() {
        val approval = items.single { it.id == "GA-26" }
        assertEquals(AuditState.REQUIRES_SIGNED_GOVERNANCE_EVIDENCE, approval.state)
        assertTrue(approval.detail.contains("none is claimed"))
    }

    @Test
    fun `the audit exercises no organism behaviour`() {
        // Canonical: governance audit items must not be marked PASS by running
        // organism code. The audit function takes no simulator input at all, so
        // the property is structural: it returns the same items every time.
        assertEquals(
            items.map { it.id to it.state },
            AlivenessGovernanceAudit.audit().map { it.id to it.state },
        )
    }
}
