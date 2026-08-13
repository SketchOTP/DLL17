package com.animusmachinae.dll17.core.crypto

/**
 * R000 module marker for `core-crypto`.
 *
 * Canonical hashing, byte-level codecs, deterministic random-domain derivation
 * and serialized PRNG substreams belong to R001 and are not implemented here.
 * The module name does not imply that ordinary random draws use expensive
 * cryptography.
 */
public object CoreCryptoModule {
    public const val ID: String = "core-crypto"

    public const val RESPONSIBILITY: String =
        "Canonical hashing, byte-level codecs, deterministic random-domain derivation, " +
            "serialized PRNG substreams."

    /** Declared runtime boundary. `core-*` modules never link the Android framework. */
    public const val RUNTIME_BOUNDARY: String = "pure-kotlin-jvm"

    /** True only once R001 canonical hashing and codec logic exists in this module. */
    public const val CANONICAL_LOGIC_IMPLEMENTED: Boolean = false
}
