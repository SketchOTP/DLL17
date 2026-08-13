package com.animusmachinae.dll17.research.aliveness.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GovernanceAuditTest {

    private val items = AlivenessGovernanceAudit.audit()

    @Test
    fun `the audit covers every canonical verification item`() {
        assertEquals(20, items.size)
        assertEquals(items.size, items.map { it.id }.distinct().size)
    }

    @Test
    fun `no item claims PASS on the strength of human evidence that does not exist`() {
        val humanDependent = setOf("GA-03", "GA-04", "GA-05", "GA-10", "GA-11", "GA-15", "GA-16")
        for (item in items.filter { it.id in humanDependent }) {
            assertTrue(
                item.state != AuditState.PASS,
                "${item.id} claims PASS but depends on evidence that does not exist",
            )
        }
    }

    @Test
    fun `A001 remains blocked while the reviewer roster is unassigned`() {
        val roster = items.single { it.id == "GA-15" }
        assertEquals(AuditState.BLOCKED, roster.state)
        assertTrue(roster.detail.contains("BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED"))
        assertTrue(
            AlivenessGovernanceAudit.render(items)
                .contains("A001_ACTIVATION=BLOCKED_GOVERNANCE_REVIEWER_UNASSIGNED"),
        )
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
