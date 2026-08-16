package com.animusmachinae.dll17.research.aliveness.agentic

/** Owner/operator acceptance, deliberately not a population statistic. */
public object OwnerPixelAlivenessAcceptanceV1 {
    public const val CONTRACT_ID: String = "OwnerPixelAlivenessAcceptanceV1"
    public const val REVIEWER: String = "OWNER_ONLY"
    public const val REVIEWERS: Int = 1
    public const val REQUIRES_REAL_DEVICE: Boolean = true
    public const val REQUIRES_LIFECYCLE_INTERRUPTION: Boolean = true

    public class ReviewRecord(
        public val aiQualificationPassed: Boolean,
        public val ciPassed: Boolean,
        public val commitSha: String,
        public val apkIdentity: String,
        public val applicationVersion: String,
        public val deviceIdentity: String,
        public val androidVersion: String,
        public val organismIdentity: String,
        public val timestamp: String,
        public val lifecycleInterruptionPerformed: Boolean,
        public val outcome: Outcome,
    ) {
        init {
            require(commitSha.matches(Regex("[0-9a-fA-F]{40,64}")))
            require(apkIdentity.isNotBlank() && applicationVersion.isNotBlank())
            require(deviceIdentity.isNotBlank() && androidVersion.isNotBlank())
            require(organismIdentity.isNotBlank() && timestamp.isNotBlank())
            if (outcome == Outcome.OWNER_PIXEL_ALIVENESS_PASS) {
                require(aiQualificationPassed && ciPassed && lifecycleInterruptionPerformed)
            }
        }
    }

    public enum class Outcome { OWNER_PIXEL_ALIVENESS_PASS, OWNER_PIXEL_ALIVENESS_FAIL, NOT_RUN }

    public enum class FinalGate {
        A001_V2_PASS,
        A001_V2_FAIL_OWNER_ACCEPTANCE,
        A001_V2_FAIL_AI_QUALIFICATION,
        A001_V2_BLOCKED_INVALID_AI_PANEL,
        A001_V2_PENDING_OWNER_PIXEL_ACCEPTANCE,
    }

    public fun finalGate(aiResult: A001V2Aggregation.Result, owner: Outcome): FinalGate = when {
        aiResult == A001V2Aggregation.Result.A001_AI_PANEL_INVALID -> FinalGate.A001_V2_BLOCKED_INVALID_AI_PANEL
        aiResult != A001V2Aggregation.Result.A001_AI_QUALIFICATION_PASS -> FinalGate.A001_V2_FAIL_AI_QUALIFICATION
        owner == Outcome.NOT_RUN -> FinalGate.A001_V2_PENDING_OWNER_PIXEL_ACCEPTANCE
        owner == Outcome.OWNER_PIXEL_ALIVENESS_FAIL -> FinalGate.A001_V2_FAIL_OWNER_ACCEPTANCE
        else -> FinalGate.A001_V2_PASS
    }
}
