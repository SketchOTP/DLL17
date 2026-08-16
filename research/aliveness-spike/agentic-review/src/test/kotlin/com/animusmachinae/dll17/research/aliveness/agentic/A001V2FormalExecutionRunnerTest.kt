package com.animusmachinae.dll17.research.aliveness.agentic

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class A001V2FormalExecutionRunnerTest {
    private val valid = """
        {"scores":{"A":{"APPARENT_AUTONOMY":10,"BEHAVIORAL_COHERENCE":20,"ADAPTIVE_RESPONSIVENESS":30,"INDIVIDUALITY_AND_HISTORY":40,"SPONTANEOUS_SENSIBLE_ACTIVITY":50,"OVERALL_APPARENT_ALIVENESS":60},"B":{"APPARENT_AUTONOMY":11,"BEHAVIORAL_COHERENCE":21,"ADAPTIVE_RESPONSIVENESS":31,"INDIVIDUALITY_AND_HISTORY":41,"SPONTANEOUS_SENSIBLE_ACTIVITY":51,"OVERALL_APPARENT_ALIVENESS":61}},"PAIRWISE_PREFERENCE":"B","EVIDENCE_GROUNDED_RATIONALE":"B shows the more coherent visible sequence."}
    """.trimIndent()

    @Test
    fun `valid response parses and malformed response fails closed`() {
        assertTrue(A001V2ResponseParser.parse(valid).scoresA.isNotEmpty())
        assertFailsWith<IllegalArgumentException> {
            A001V2ResponseParser.parse("{\"PAIRWISE_PREFERENCE\":\"A\"}")
        }
    }

    @Test
    fun `completed slot cannot be silently overwritten`() {
        val directory = Files.createTempDirectory("a001-slot")
        val ledger = A001V2SlotLedger(directory)
        ledger.claim("CAL-P01-A")
        assertFailsWith<IllegalArgumentException> { ledger.claim("CAL-P01-A") }
        ledger.persistRaw(A001V2RawExecution("CAL-P01-A", valid, "test", "model", "exec-1"))
        assertFailsWith<IllegalArgumentException> {
            ledger.persistRaw(A001V2RawExecution("CAL-P01-A", valid, "test", "model", "exec-2"))
        }
    }
}
