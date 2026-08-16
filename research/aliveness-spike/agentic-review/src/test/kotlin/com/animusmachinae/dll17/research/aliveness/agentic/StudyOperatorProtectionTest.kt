package com.animusmachinae.dll17.research.aliveness.agentic

import kotlin.test.Test
import kotlin.test.assertFailsWith

class StudyOperatorProtectionTest {
    private val valid = ParticipantEligibility(true, true, false, true, true)

    @Test
    fun `valid adult US participant with owner contact may be authorized`() {
        ParticipantEligibilityGate.authorize(valid)
    }

    @Test
    fun `session refuses missing owner contact`() {
        assertFailsWith<IllegalArgumentException> {
            ParticipantEligibilityGate.authorize(valid.copy(ownerContactSupplied = false))
        }
    }

    @Test
    fun `session refuses non-adult non-US prisoner or non-consenting participant`() {
        assertFailsWith<IllegalArgumentException> {
            ParticipantEligibilityGate.authorize(valid.copy(attestsUsAdult18Plus = false))
        }
        assertFailsWith<IllegalArgumentException> {
            ParticipantEligibilityGate.authorize(valid.copy(isPrisoner = true))
        }
        assertFailsWith<IllegalArgumentException> {
            ParticipantEligibilityGate.authorize(valid.copy(canProvideOwnConsent = false))
        }
    }
}
