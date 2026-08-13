package com.animusmachinae.dll17.core.continuity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The R002 qualification kernel, run on the desktop JVM.
 *
 * Two separate claims are asserted here and they must not be conflated: that
 * every exploit defence held, and that the run reproduces the frozen golden
 * digest. The first is about the organism's honesty; the second is about
 * cross-target determinism.
 */
class R002QualificationKernelTest {

    @Test
    fun `every continuity defence holds`() {
        val report = R002QualificationKernel.run()
        val failures = report.fixtureResults.filterNot { it.defenceHeld }
        assertTrue(
            failures.isEmpty(),
            "continuity defences failed:\n" +
                failures.joinToString("\n") { "  ${it.id} [${it.area}] — ${it.detail}" },
        )
    }

    @Test
    fun `the run is reproducible within this process`() {
        val first = R002QualificationKernel.run()
        val second = R002QualificationKernel.run()
        assertEquals(
            first.evidenceDigestHex,
            second.evidenceDigestHex,
            "two runs of the kernel disagreed, so nothing about it is deterministic",
        )
    }

    @Test
    fun `the run reproduces the frozen golden digest`() {
        val report = R002QualificationKernel.run()
        assertEquals(
            R002QualificationKernel.GOLDEN_EVIDENCE_DIGEST,
            report.evidenceDigestHex,
            "this target computed a different canonical result; R002 determinism has failed",
        )
    }
}
