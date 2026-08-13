package com.animusmachinae.dll17.core.crypto

/**
 * R000 module marker for `core-crypto`.
 *
 * R001 implemented canonical hashing, the canonical byte codec and envelope,
 * deterministic random-domain derivation and serialized PRNG substreams, under
 * the frozen `DeterminismContractV1`. The module name does not imply that
 * ordinary random draws use expensive cryptography: they do not, by contract.
 */
public object CoreCryptoModule {
    public const val ID: String = "core-crypto"

    public const val RESPONSIBILITY: String =
        "Canonical hashing, byte-level codecs, deterministic random-domain derivation, " +
            "serialized PRNG substreams."

    /** Declared runtime boundary. `core-*` modules never link the Android framework. */
    public const val RUNTIME_BOUNDARY: String = "pure-kotlin-jvm"

    /** True since R001: canonical hashing and codec logic exists in this module. */
    public const val CANONICAL_LOGIC_IMPLEMENTED: Boolean = true
}
