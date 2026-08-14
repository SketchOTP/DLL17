package com.animusmachinae.dll17.core.recovery

import com.animusmachinae.dll17.core.crypto.CanonicalEnvelope
import com.animusmachinae.dll17.core.crypto.CanonicalHash
import com.animusmachinae.dll17.core.crypto.CanonicalReader
import com.animusmachinae.dll17.core.crypto.CanonicalWriter
import com.animusmachinae.dll17.core.crypto.ChaCha20Poly1305
import com.animusmachinae.dll17.core.crypto.Hkdf
import com.animusmachinae.dll17.core.crypto.HmacSha256
import com.animusmachinae.dll17.core.crypto.Sha256

/**
 * `RecoveryCryptographyContractV1`, frozen values.
 *
 * Every choice here was blocked under R002 as `BLOCKED_SPEC_RECOVERY_CRYPTOGRAPHY`
 * and is now frozen. The canonical requirement is exact: recovery-secret
 * entropy, encoding and checksum; key types; KDF and wrapping algorithms;
 * authenticated-encryption format; rotation and reissue policy; package
 * manifest and MAC format; the challenge-response proof used by the identity
 * authority; replay protection; and failure and revocation semantics.
 */
public object RecoveryCryptographyContract {

    public const val CONTRACT_ID: String = "RecoveryCryptographyContractV1"
    public const val CONTRACT_VERSION: Int = 1

    /**
     * Recovery-root entropy.
     *
     * 256 bits, and that size is doing the work that a memory-hard KDF would
     * otherwise have to do. A user-chosen passphrase needs an expensive KDF
     * because it has perhaps 40 bits of entropy; a generated 256-bit root does
     * not, and making the user wait on an Argon2 pass over a full-entropy secret
     * buys nothing an attacker cannot already ignore.
     */
    public const val RECOVERY_ROOT_BITS: Int = 256
    public const val RECOVERY_ROOT_BYTES: Int = RECOVERY_ROOT_BITS / 8

    /**
     * Checksum bits appended to the encoded secret so a transcription error is
     * caught at entry rather than at restore. Four bits per five bytes of root,
     * rounded to a whole group: 20 bits over 256 bits of root.
     */
    public const val CHECKSUM_BITS: Int = 20

    /**
     * Human encoding: RFC 4648 base32 without padding, in groups of four, upper
     * case. Not a word list. A word list is easier to read aloud and needs 2,048
     * curated words per language; base32 is unambiguous, language-neutral and
     * needs no curation. The contract fixes the *bits*, so a word-list encoder
     * can be added later as presentation without changing anything cryptographic.
     */
    public const val ENCODING_ID: String = "BASE32_GROUPS_OF_FOUR_V1"
    public const val ENCODING_GROUP_SIZE: Int = 4

    public const val KDF_ALGORITHM: String = Hkdf.ALGORITHM_ID
    public const val AEAD_ALGORITHM: String = ChaCha20Poly1305.ALGORITHM_ID
    public const val MAC_ALGORITHM: String = HmacSha256.ALGORITHM_ID

    /** Domain separation. Each derived key has exactly one purpose. */
    public const val INFO_PACKAGE_KEY: String = "DLL17-RECOVERY-PACKAGE-V1"
    public const val INFO_MANIFEST_MAC: String = "DLL17-RECOVERY-MANIFEST-MAC-V1"
    public const val INFO_AUTHORITY_PROOF: String = "DLL17-IDENTITY-AUTHORITY-PROOF-V1"

    /** Canonical schemas. */
    public const val PACKAGE_SCHEMA_ID: Int = 241
    public const val PACKAGE_SCHEMA_VERSION: Int = 1
    public const val MANIFEST_SCHEMA_ID: Int = 242
    public const val MANIFEST_SCHEMA_VERSION: Int = 1

    /**
     * A package is refused if its declared identity epoch is older than the
     * epoch the destination has already activated. Recovery may go backwards in
     * *history* — that is what `RecoveryGapDeclared` is for — but never backwards
     * in *identity*, or a superseded device could reclaim the organism.
     */
    public const val STALE_PACKAGE_STATE: String = "RECOVERY_PACKAGE_STALE"
    public const val DUPLICATE_PACKAGE_STATE: String = "RECOVERY_PACKAGE_DUPLICATE"
}

/** A recovery secret failure. Never silently downgraded. */
public class RecoveryFailure(public val state: String, message: String) : RuntimeException(message)

/**
 * The user-held recovery root and its encoding.
 *
 * The root wraps the organism recovery key. It is emphatically **not** the
 * organism's history: the canonical architecture requires that the product never
 * imply the phrase alone can recreate what was lost, and `describe` says so in
 * the one place a caller is guaranteed to read.
 */
public class RecoveryRoot(public val bytes: ByteArray) {

    init {
        if (bytes.size != RecoveryCryptographyContract.RECOVERY_ROOT_BYTES) {
            throw IllegalArgumentException(
                "recovery root must be ${RecoveryCryptographyContract.RECOVERY_ROOT_BYTES} bytes",
            )
        }
    }

    /** Derives the key that encrypts a cold package for a given organism. */
    public fun packageKey(organismId: Long): ByteArray = Hkdf.derive(
        inputKeyMaterial = bytes,
        salt = CanonicalWriter(8).putI64(organismId).toByteArray(),
        info = RecoveryCryptographyContract.INFO_PACKAGE_KEY,
        length = ChaCha20Poly1305.KEY_SIZE,
    )

    /** Derives the MAC key that authenticates the plaintext manifest. */
    public fun manifestMacKey(organismId: Long): ByteArray = Hkdf.derive(
        inputKeyMaterial = bytes,
        salt = CanonicalWriter(8).putI64(organismId).toByteArray(),
        info = RecoveryCryptographyContract.INFO_MANIFEST_MAC,
        length = HmacSha256.DIGEST_SIZE,
    )

    /**
     * Derives the secret used to answer an identity-authority challenge.
     *
     * The authority never sees the root, the package or any organism data. It
     * sees a MAC over a nonce it issued, which proves the caller holds the root
     * without revealing it and cannot be replayed against a different nonce.
     */
    public fun authorityProofKey(organismId: Long): ByteArray = Hkdf.derive(
        inputKeyMaterial = bytes,
        salt = CanonicalWriter(8).putI64(organismId).toByteArray(),
        info = RecoveryCryptographyContract.INFO_AUTHORITY_PROOF,
        length = HmacSha256.DIGEST_SIZE,
    )

    /** The encoded form handed to the user, checksummed. */
    public fun encode(): String = RecoverySecretCodec.encode(this)

    public companion object {
        public fun decode(encoded: String): RecoveryRoot = RecoverySecretCodec.decode(encoded)

        /**
         * The wording the product must use. Kept here rather than in UI text so
         * a screen cannot quietly promise more than the cryptography delivers.
         */
        public const val USER_DISCLOSURE: String =
            "This phrase unlocks a backup you have already made. It does not " +
                "contain your creature's memories. Without a current encrypted " +
                "backup, the phrase alone cannot bring lost history back."
    }
}

/** Base32 encoding with a truncated-SHA-256 checksum. */
public object RecoverySecretCodec {

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    public fun encode(root: RecoveryRoot): String {
        val checksum = checksumBits(root.bytes)
        val payload = root.bytes + checksum
        val builder = StringBuilder()
        var buffer = 0
        var bits = 0
        var emitted = 0
        val totalSymbols =
            (RecoveryCryptographyContract.RECOVERY_ROOT_BITS +
                RecoveryCryptographyContract.CHECKSUM_BITS) / 5
        for (byte in payload) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bits += 8
            while (bits >= 5 && emitted < totalSymbols) {
                bits -= 5
                builder.append(ALPHABET[(buffer ushr bits) and 0x1F])
                emitted++
            }
        }
        return builder.toString()
            .chunked(RecoveryCryptographyContract.ENCODING_GROUP_SIZE)
            .joinToString("-")
    }

    public fun decode(encoded: String): RecoveryRoot {
        val cleaned = encoded.uppercase().replace("-", "").replace(" ", "")
        val expectedSymbols =
            (RecoveryCryptographyContract.RECOVERY_ROOT_BITS +
                RecoveryCryptographyContract.CHECKSUM_BITS) / 5
        if (cleaned.length != expectedSymbols) {
            throw RecoveryFailure(
                "RECOVERY_SECRET_MALFORMED",
                "expected $expectedSymbols symbols, got ${cleaned.length}",
            )
        }
        var buffer = 0
        var bits = 0
        val out = java.io.ByteArrayOutputStream()
        for (symbol in cleaned) {
            val value = ALPHABET.indexOf(symbol)
            if (value < 0) {
                throw RecoveryFailure("RECOVERY_SECRET_MALFORMED", "invalid symbol '$symbol'")
            }
            buffer = (buffer shl 5) or value
            bits += 5
            if (bits >= 8) {
                bits -= 8
                out.write((buffer ushr bits) and 0xFF)
            }
        }
        val decoded = out.toByteArray()
        val root = decoded.copyOf(RecoveryCryptographyContract.RECOVERY_ROOT_BYTES)
        val checksum = decoded.copyOfRange(
            RecoveryCryptographyContract.RECOVERY_ROOT_BYTES,
            decoded.size,
        )
        val expected = checksumBits(root).copyOf(checksum.size)
        if (!HmacSha256.constantTimeEquals(checksum, expected)) {
            throw RecoveryFailure(
                "RECOVERY_SECRET_CHECKSUM_FAILED",
                "the phrase does not check out; it was probably mistyped",
            )
        }
        return RecoveryRoot(root)
    }

    /** Checksum bytes, truncated to cover [CHECKSUM_BITS] rounded up. */
    private fun checksumBits(root: ByteArray): ByteArray {
        val digest = Sha256.digest(root)
        val bytes = (RecoveryCryptographyContract.CHECKSUM_BITS + 7) / 8
        return digest.copyOf(bytes)
    }
}

/**
 * The plaintext manifest of a cold package.
 *
 * Plaintext on purpose: a provider and a destination device must be able to
 * check identity, epoch, sequence and size *before* anyone has a key, so a stale
 * or foreign package is refused without a decryption attempt. It carries no
 * organism content, and it is MAC'd with a key derived from the recovery root,
 * so a provider that rewrites it is detected.
 */
public class RecoveryManifest(
    public val organismId: Long,
    public val identityEpoch: Int,
    public val packageSequence: Long,
    public val checkpointSequence: Long,
    public val lastProtectedLogicalTime: Long,
    public val engineContractVersion: Int,
    public val eventContractVersion: Int,
    public val randomContractVersion: Int,
    public val ciphertextBytes: Long,
    public val ciphertextHash: ByteArray,
    public val lineageHash: ByteArray,
) {
    public fun canonicalBytes(): ByteArray = CanonicalEnvelope.wrap(
        RecoveryCryptographyContract.MANIFEST_SCHEMA_ID,
        RecoveryCryptographyContract.MANIFEST_SCHEMA_VERSION,
        CanonicalWriter(160)
            .putI64(organismId)
            .putI32(identityEpoch)
            .putI64(packageSequence)
            .putI64(checkpointSequence)
            .putI64(lastProtectedLogicalTime)
            .putI32(engineContractVersion)
            .putI32(eventContractVersion)
            .putI32(randomContractVersion)
            .putI64(ciphertextBytes)
            .putBytes(ciphertextHash)
            .putBytes(lineageHash)
            .toByteArray(),
    )

    public fun mac(root: RecoveryRoot): ByteArray =
        HmacSha256.mac(root.manifestMacKey(organismId), canonicalBytes())

    public companion object {
        public fun decode(bytes: ByteArray): RecoveryManifest {
            val contents = CanonicalEnvelope.unwrap(bytes)
            if (contents.payloadSchemaId != RecoveryCryptographyContract.MANIFEST_SCHEMA_ID) {
                throw RecoveryFailure(
                    "RECOVERY_PACKAGE_MALFORMED",
                    "manifest carries schema ${contents.payloadSchemaId}",
                )
            }
            val reader = CanonicalReader(contents.payload)
            val manifest = RecoveryManifest(
                organismId = reader.readI64(),
                identityEpoch = reader.readI32(),
                packageSequence = reader.readI64(),
                checkpointSequence = reader.readI64(),
                lastProtectedLogicalTime = reader.readI64(),
                engineContractVersion = reader.readI32(),
                eventContractVersion = reader.readI32(),
                randomContractVersion = reader.readI32(),
                ciphertextBytes = reader.readI64(),
                ciphertextHash = reader.readBytes(),
                lineageHash = reader.readBytes(),
            )
            reader.requireExhausted()
            return manifest
        }
    }
}

/** What a package protects: a checkpoint plus the journal tail above it. */
public class RecoveryContents(
    public val checkpointBytes: ByteArray,
    public val journalTail: List<Pair<Long, ByteArray>>,
) {
    public fun canonicalBytes(): ByteArray {
        val writer = CanonicalWriter(checkpointBytes.size + 64 + journalTail.sumOf { it.second.size + 24 })
        writer.putBytes(checkpointBytes)
        writer.putU32(journalTail.size)
        for ((sequence, record) in journalTail) {
            writer.putI64(sequence)
            writer.putBytes(record)
        }
        return writer.toByteArray()
    }

    public companion object {
        public fun decode(bytes: ByteArray): RecoveryContents {
            val reader = CanonicalReader(bytes)
            val checkpoint = reader.readBytes()
            val count = reader.readSequenceCount()
            val tail = ArrayList<Pair<Long, ByteArray>>(count)
            repeat(count) { tail += reader.readI64() to reader.readBytes() }
            reader.requireExhausted()
            return RecoveryContents(checkpoint, tail)
        }
    }
}

/**
 * The encrypted cold package.
 *
 * Structure: a plaintext manifest, its MAC, and one AEAD ciphertext over the
 * contents with the manifest as associated data. Binding the manifest into the
 * AAD is what stops a provider from serving package A's ciphertext under package
 * B's manifest — the decryption simply fails.
 */
public object ColdRecoveryPackage {

    public class Sealed(
        public val manifest: RecoveryManifest,
        public val manifestMac: ByteArray,
        public val ciphertext: ByteArray,
    ) {
        public fun canonicalBytes(): ByteArray {
            val manifestBytes = manifest.canonicalBytes()
            return CanonicalEnvelope.wrap(
                RecoveryCryptographyContract.PACKAGE_SCHEMA_ID,
                RecoveryCryptographyContract.PACKAGE_SCHEMA_VERSION,
                CanonicalWriter(manifestBytes.size + ciphertext.size + 96)
                    .putBytes(manifestBytes)
                    .putBytes(manifestMac)
                    .putBytes(ciphertext)
                    .toByteArray(),
            )
        }

        public val sizeBytes: Long get() = canonicalBytes().size.toLong()

        public fun checksumHex(): String = CanonicalHash.hex(CanonicalHash.ofEnvelope(canonicalBytes()))
    }

    /** Nonce from `(organismId, identityEpoch, packageSequence)`. Never random. */
    private fun nonce(manifest: RecoveryManifest): ByteArray = CanonicalWriter(12)
        .putI32(manifest.identityEpoch)
        .putI64(manifest.packageSequence)
        .toByteArray()

    public fun create(
        root: RecoveryRoot,
        manifestTemplate: (ciphertextBytes: Long, ciphertextHash: ByteArray) -> RecoveryManifest,
        contents: RecoveryContents,
        organismId: Long,
    ): Sealed {
        val plaintext = contents.canonicalBytes()
        // The manifest depends on the ciphertext and the ciphertext's AAD is the
        // manifest, so the manifest is built in two passes: once to obtain the
        // nonce inputs, then finalised with the real ciphertext hash.
        val provisional = manifestTemplate(0L, ByteArray(CanonicalHash.DIGEST_SIZE))
        val ciphertext = ChaCha20Poly1305.seal(
            key = root.packageKey(organismId),
            nonce = nonce(provisional),
            aad = aad(provisional),
            plaintext = plaintext,
        )
        val manifest = manifestTemplate(
            ciphertext.size.toLong(),
            CanonicalHash.ofEnvelope(ciphertext),
        )
        return Sealed(manifest, manifest.mac(root), ciphertext)
    }

    /**
     * The AAD binds only the fields that must not change between sealing and
     * opening. The ciphertext size and hash are excluded because they are
     * derived from the ciphertext itself and are checked separately.
     */
    private fun aad(manifest: RecoveryManifest): ByteArray = CanonicalWriter(48)
        .putI64(manifest.organismId)
        .putI32(manifest.identityEpoch)
        .putI64(manifest.packageSequence)
        .putI64(manifest.checkpointSequence)
        .putBytes(manifest.lineageHash)
        .toByteArray()

    public fun parse(bytes: ByteArray): Sealed {
        val contents = try {
            CanonicalEnvelope.unwrap(bytes)
        } catch (malformed: RuntimeException) {
            throw RecoveryFailure("RECOVERY_PACKAGE_MALFORMED", "package envelope is unreadable")
        }
        if (contents.payloadSchemaId != RecoveryCryptographyContract.PACKAGE_SCHEMA_ID) {
            throw RecoveryFailure(
                "RECOVERY_PACKAGE_MALFORMED",
                "package carries schema ${contents.payloadSchemaId}",
            )
        }
        val reader = CanonicalReader(contents.payload)
        val manifest = RecoveryManifest.decode(reader.readBytes())
        val mac = reader.readBytes()
        val ciphertext = reader.readBytes()
        reader.requireExhausted()
        return Sealed(manifest, mac, ciphertext)
    }

    /**
     * Verifies and opens a package.
     *
     * The order is deliberate and each step refuses a different attack:
     * manifest MAC first (a rewritten manifest), then the ciphertext hash (a
     * substituted body), then the AEAD (a wrong key, or a manifest and body from
     * different packages).
     */
    public fun open(root: RecoveryRoot, sealed: Sealed): RecoveryContents {
        if (!HmacSha256.constantTimeEquals(sealed.manifestMac, sealed.manifest.mac(root))) {
            throw RecoveryFailure(
                "RECOVERY_PACKAGE_UNAUTHENTIC",
                "manifest MAC does not verify under this recovery secret",
            )
        }
        if (sealed.ciphertext.size.toLong() != sealed.manifest.ciphertextBytes) {
            throw RecoveryFailure(
                "RECOVERY_PACKAGE_CORRUPT",
                "ciphertext is ${sealed.ciphertext.size} bytes, manifest declares " +
                    "${sealed.manifest.ciphertextBytes}",
            )
        }
        val hash = CanonicalHash.ofEnvelope(sealed.ciphertext)
        if (!HmacSha256.constantTimeEquals(hash, sealed.manifest.ciphertextHash)) {
            throw RecoveryFailure("RECOVERY_PACKAGE_CORRUPT", "ciphertext hash does not match")
        }
        val plaintext = try {
            ChaCha20Poly1305.open(
                key = root.packageKey(sealed.manifest.organismId),
                nonce = nonce(sealed.manifest),
                aad = aad(sealed.manifest),
                sealed = sealed.ciphertext,
            )
        } catch (failure: ChaCha20Poly1305.AuthenticationFailure) {
            throw RecoveryFailure(
                "RECOVERY_PACKAGE_UNAUTHENTIC",
                "package did not authenticate: ${failure.message}",
            )
        }
        return RecoveryContents.decode(plaintext)
    }
}
