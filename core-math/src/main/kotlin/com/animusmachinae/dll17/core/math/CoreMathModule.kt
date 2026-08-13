package com.animusmachinae.dll17.core.math

/**
 * R000 module marker for `core-math`.
 *
 * The module exists so that the runtime boundary, build wiring and test harness
 * are provable before any canonical logic is written. Fixed-point types,
 * saturating arithmetic, bounded interpolation and lookup tables belong to R001
 * and may only be implemented after `DeterminismContractV1` is frozen.
 */
public object CoreMathModule {
    public const val ID: String = "core-math"

    public const val RESPONSIBILITY: String =
        "Fixed-point types, saturating arithmetic, bounded interpolation and decay, " +
            "lookup tables, numeric diagnostics."

    /** Declared runtime boundary. `core-*` modules never link the Android framework. */
    public const val RUNTIME_BOUNDARY: String = "pure-kotlin-jvm"

    /** True only once R001 canonical numeric logic exists in this module. */
    public const val CANONICAL_LOGIC_IMPLEMENTED: Boolean = false
}
