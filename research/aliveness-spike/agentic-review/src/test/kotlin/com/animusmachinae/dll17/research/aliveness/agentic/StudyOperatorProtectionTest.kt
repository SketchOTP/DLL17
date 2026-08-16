package com.animusmachinae.dll17.research.aliveness.agentic

import kotlin.test.Test
import kotlin.test.assertFailsWith

class StudyOperatorProtectionTest {
    private val valid = ParticipantEligibility(18, "US", true, false, true, true)

    @Test
    fun `valid adult US participant with owner contact may be authorized`() {
        StudyOperator().authorizeSession(valid)
    }

    @Test
    fun `session refuses missing owner contact`() {
        assertFailsWith<IllegalArgumentException> {
            StudyOperator().authorizeSession(valid.copy(ownerContactSupplied = false))
        }
    }

    @Test
    fun `session refuses non-adult non-US prisoner or non-consenting participant`() {
        assertFailsWith<IllegalArgumentException> {
            StudyOperator().authorizeSession(valid.copy(age = 17))
        }
        assertFailsWith<IllegalArgumentException> {
            StudyOperator().authorizeSession(valid.copy(countryCode = "CA"))
        }
        assertFailsWith<IllegalArgumentException> {
            StudyOperator().authorizeSession(valid.copy(isPrisoner = true))
        }
        assertFailsWith<IllegalArgumentException> {
            StudyOperator().authorizeSession(valid.copy(canProvideOwnConsent = false))
        }
    }
}
