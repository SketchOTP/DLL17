package com.animusmachinae.dll17.research.aliveness.analysis

import com.animusmachinae.dll17.research.aliveness.agentic.sha256

/** D016-K: deterministic baseline qualification freeze; no owner or reviewer. */
public object BaselineQualificationFreezeV1 {
    public const val FREEZE_ID: String = "BaselineQualificationFreezeV1"
    public const val COMPARATOR_ID: String = "ScriptedPetBaselineV1-vs-DegradedScriptedControlV1"
    public const val PROTOCOL_ID: String = "BaselineQualificationProtocolV1"
    public const val INSTRUMENT_ID: String = "BaselineCompetenceInstrumentV1"
    public const val PRESENTATION_ID: String = "SpikeExpressionContractV1-blinded-counterbalanced"
    public const val EXCLUSIONS_ID: String = "BaselineQualificationExclusionsV1"
    public const val ANALYSIS_ID: String = "A001AnalysisV1-paired-t-interval"
    public const val MANIFEST_SHA256: String = "0403e06eb5004bf09e8d402abc54a153bec7857c4e4f3d7c6301808cef0ff18d"

    public val canonicalDescriptor: String = listOf(
        FREEZE_ID, COMPARATOR_ID, PROTOCOL_ID, INSTRUMENT_ID,
        PRESENTATION_ID, EXCLUSIONS_ID, ANALYSIS_ID,
    ).joinToString("\n")

    public val descriptorHash: String get() = sha256(canonicalDescriptor)
}
