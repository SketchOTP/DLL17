package com.animusmachinae.dll17.core.math

/**
 * R000 module marker for `core-math`.
 *
 * R001 implemented the canonical numeric foundation named below, under the
 * frozen `DeterminismContractV1`: `Fixed64` semantics, saturating arithmetic,
 * bounded interpolation and decay, digest-verified lookup tables and saturation
 * diagnostics. No organism parameter, equation or threshold lives here.
 */
public object CoreMathModule {
    public const val ID: String = "core-math"

    public const val RESPONSIBILITY: String =
        "Fixed-point types, saturating arithmetic, bounded interpolation and decay, " +
            "lookup tables, numeric diagnostics."

    /** Declared runtime boundary. `core-*` modules never link the Android framework. */
    public const val RUNTIME_BOUNDARY: String = "pure-kotlin-jvm"

    /** True since R001: canonical numeric logic exists in this module. */
    public const val CANONICAL_LOGIC_IMPLEMENTED: Boolean = true
}
