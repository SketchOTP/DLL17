package com.animusmachinae.dll17.core.state

/**
 * R000 module marker for `core-state`.
 *
 * R001 implemented the canonical snapshot, the single-threaded pure reducer,
 * normalized events, durability classes, the durable journal and replay kernel,
 * the Class W staged protocol, the panic witness and schema migration. All of it
 * is determinism mechanism: no organism state, drive, physiology or behavior is
 * defined here, and none may be until R003 opens behind the A001 gate.
 */
public object CoreStateModule {
    public const val ID: String = "core-state"

    public const val RESPONSIBILITY: String =
        "Canonical state, reducer, normalized events, commitments, transition journal " +
            "contracts, snapshot and commit frame models, migrations."

    /** Declared runtime boundary. `core-*` modules never link the Android framework. */
    public const val RUNTIME_BOUNDARY: String = "pure-kotlin-jvm"

    /** True since R001: the canonical state and reducer contract exists here. */
    public const val CANONICAL_LOGIC_IMPLEMENTED: Boolean = true
}
