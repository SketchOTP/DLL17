package com.animusmachinae.dll17.core.state

/**
 * R000 module marker for `core-state`.
 *
 * Canonical state, the single-threaded reducer, normalized events, commitments,
 * transition-journal contracts, snapshot/commit frames and migrations belong to
 * R001 and later phases. No organism state or behavior is defined here.
 */
public object CoreStateModule {
    public const val ID: String = "core-state"

    public const val RESPONSIBILITY: String =
        "Canonical state, reducer, normalized events, commitments, transition journal " +
            "contracts, snapshot and commit frame models, migrations."

    /** Declared runtime boundary. `core-*` modules never link the Android framework. */
    public const val RUNTIME_BOUNDARY: String = "pure-kotlin-jvm"

    /** True only once the R001 canonical state and reducer contract exists here. */
    public const val CANONICAL_LOGIC_IMPLEMENTED: Boolean = false
}
