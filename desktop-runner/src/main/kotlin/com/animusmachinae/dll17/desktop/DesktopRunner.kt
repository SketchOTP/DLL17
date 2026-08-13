package com.animusmachinae.dll17.desktop

import com.animusmachinae.dll17.core.continuity.CoreContinuityModule
import com.animusmachinae.dll17.core.continuity.R002QualificationKernel
import com.animusmachinae.dll17.core.crypto.CoreCryptoModule
import com.animusmachinae.dll17.core.math.CoreMathModule
import com.animusmachinae.dll17.core.state.CoreStateModule
import com.animusmachinae.dll17.core.state.R001QualificationKernel

/**
 * The desktop JVM reference runner.
 *
 * From R001 this is a qualification target, not a status printer. The canonical
 * determinism matrix names it as one of the platforms that must produce
 * byte-identical canonical output, so it runs the same
 * [R001QualificationKernel] the Android targets run and emits the same
 * `R001_EVIDENCE_DIGEST` line for comparison.
 *
 * It reads no clock, no environment variable and no file. If its output ever
 * differs between two runs on the same machine, the defect is in the kernel, not
 * in the environment.
 */
public object DesktopRunner {
    public data class ModuleReport(
        val id: String,
        val runtimeBoundary: String,
        val canonicalLogicImplemented: Boolean,
    )

    public fun moduleInventory(): List<ModuleReport> = listOf(
        ModuleReport(
            CoreMathModule.ID,
            CoreMathModule.RUNTIME_BOUNDARY,
            CoreMathModule.CANONICAL_LOGIC_IMPLEMENTED,
        ),
        ModuleReport(
            CoreCryptoModule.ID,
            CoreCryptoModule.RUNTIME_BOUNDARY,
            CoreCryptoModule.CANONICAL_LOGIC_IMPLEMENTED,
        ),
        ModuleReport(
            CoreStateModule.ID,
            CoreStateModule.RUNTIME_BOUNDARY,
            CoreStateModule.CANONICAL_LOGIC_IMPLEMENTED,
        ),
        ModuleReport(
            CoreContinuityModule.ID,
            CoreContinuityModule.RUNTIME_BOUNDARY,
            CoreContinuityModule.CANONICAL_LOGIC_IMPLEMENTED,
        ),
    )

    public fun report(): String {
        val lines = mutableListOf(
            "Digital Living Lifeform - desktop runner",
            "phase: R002 lifecycle, durability, trusted time and reconciliation",
            "canonical logic: DeterminismContractV1 v1 and ContinuityDurabilityContractV1 v1 " +
                "frozen; deterministic core and continuity foundation implemented",
            "organism behavior: none. Physiology, drives, learning, memory and " +
                "relationships remain gated on R003 behind A001.",
            "modules:",
        )
        moduleInventory().forEach { module ->
            lines += "  - ${module.id} [${module.runtimeBoundary}] " +
                "canonicalLogicImplemented=${module.canonicalLogicImplemented}"
        }
        lines += ""
        lines += R001QualificationKernel.renderReport(R001QualificationKernel.run())
        lines += ""
        lines += R002QualificationKernel.renderReport(R002QualificationKernel.run())
        return lines.joinToString(separator = "\n")
    }
}

public fun main() {
    println(DesktopRunner.report())
}
