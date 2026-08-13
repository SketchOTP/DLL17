package com.animusmachinae.dll17.core.continuity

/**
 * Module marker for `core-continuity`, added in R002.
 *
 * It owns continuity and durability: the four clocks, durable time anchors,
 * trusted elapsed time, blind-decay credit, bounded debt reconciliation, offline
 * reconciliation modes, journal generations, compaction, durability admission,
 * platform protection, the encrypted-record boundary, restart recovery and
 * version boundaries.
 *
 * It owns no organism. Physiology, drives, action selection, learning, memory,
 * relationships and development remain gated on R003 behind A001, and the two
 * neutral reserves here exist only so a reconciliation has something observable
 * to act on.
 */
public object CoreContinuityModule {
    public const val ID: String = "core-continuity"

    public const val RESPONSIBILITY: String =
        "Four-clock time model, durable anchors, time confidence, blind-decay credit, " +
            "unresolved-time debt, offline reconciliation, journal generations and compaction, " +
            "durability admission and safe hold, platform protection, encrypted-record boundary, " +
            "restart recovery, identity binding and version boundaries."

    /** Declared runtime boundary. `core-*` modules never link the Android framework. */
    public const val RUNTIME_BOUNDARY: String = "pure-kotlin-jvm"

    /** True since R002: the continuity and durability contract is implemented here. */
    public const val CANONICAL_LOGIC_IMPLEMENTED: Boolean = true
}
