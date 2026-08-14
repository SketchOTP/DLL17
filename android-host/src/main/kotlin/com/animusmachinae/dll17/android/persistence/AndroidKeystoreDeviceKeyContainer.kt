package com.animusmachinae.dll17.android.persistence

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import com.animusmachinae.dll17.core.persistence.DeviceKeyContainer
import com.animusmachinae.dll17.core.persistence.KeyFault
import com.animusmachinae.dll17.core.persistence.KeyStateFault
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory

/**
 * The Android production implementation of the frozen [DeviceKeyContainer].
 *
 * `LocalStorageCryptographyContractV1` fixes the shape: a device-bound container
 * yields a root secret, HKDF turns that into a wrapping key, and the wrapping
 * key wraps a random data key. Only the container is platform-specific, and this
 * class is the whole of it.
 *
 * ### Why an HMAC key and not an AES key
 *
 * The contract requires the container's material to be non-exportable and the
 * root secret to be *stable* — the same bytes after every restart, or nothing
 * written yesterday opens today. An AndroidKeyStore AES key satisfies the first
 * and not the second: `Cipher` in GCM mode generates its own IV, so encrypting a
 * fixed label yields different bytes every time, and pinning the IV to make it
 * stable would be nonce reuse on a long-lived key.
 *
 * An HMAC-SHA256 key satisfies both. The key never leaves the keystore; what
 * leaves is `HMAC(key, label)`, which is a derived secret, deterministic, and
 * exactly as strong as the key it came from. Two different labels give two
 * unrelated secrets, which is how the fingerprint below is derived without a
 * second key.
 *
 * ### Why generation is a separate call
 *
 * [openExisting] never creates a key. That asymmetry is the single most
 * important line in this file. If opening generated a missing key, then losing
 * the keystore — an OS update, a restored image, a copied data directory —
 * would produce a *working* container holding different material, the wrapped
 * data key would fail to authenticate, and the natural next step for any caller
 * that treats "no readable state" as "no organism" is a birth. Refusing to
 * generate is what turns that path into `CONTAINER_UNAVAILABLE`.
 */
public class AndroidKeystoreDeviceKeyContainer private constructor(
    public val alias: String,
    private val key: SecretKey,
    override val deviceFingerprint: Long,
    /** What the platform reported about where this key lives. Evidence, not policy. */
    public val backing: String,
) : DeviceKeyContainer {

    override val available: Boolean
        get() = try {
            mac()
            true
        } catch (invalidated: KeyPermanentlyInvalidatedException) {
            false
        } catch (failure: GeneralFailure) {
            false
        }

    override fun rootSecret(): ByteArray = derive(ROOT_SECRET_LABEL)

    /**
     * Removes the container material.
     *
     * `LocalStorageCryptographyContractV1` fixes the order: wrapping material
     * first, then local files. Reversing it leaves a window in which the
     * ciphertext is still on disk and the key that opens it is still in the
     * keystore.
     */
    public fun deleteContainerMaterial() {
        keyStore().deleteEntry(alias)
    }

    private fun derive(label: String): ByteArray = try {
        mac().doFinal(label.toByteArray(Charsets.US_ASCII))
    } catch (invalidated: KeyPermanentlyInvalidatedException) {
        throw KeyStateFault(
            KeyFault.CONTAINER_UNAVAILABLE,
            "keystore key '$alias' is permanently invalidated: ${invalidated.message}",
        )
    } catch (failure: Exception) {
        throw KeyStateFault(
            KeyFault.CONTAINER_UNAVAILABLE,
            "keystore key '$alias' would not produce material: ${failure.message}",
        )
    }

    private fun mac(): Mac = Mac.getInstance(MAC_ALGORITHM).apply { init(key) }

    private class GeneralFailure(cause: Throwable) : RuntimeException(cause)

    public companion object {

        public const val PROVIDER: String = "AndroidKeyStore"
        public const val MAC_ALGORITHM: String = "HmacSHA256"
        public const val DEFAULT_ALIAS: String = "dll17.local.wrap.v1"

        /**
         * Domain separation inside the container, so the fingerprint cannot be
         * used to learn anything about the root secret or the reverse.
         */
        public const val ROOT_SECRET_LABEL: String = "DLL17-CONTAINER-ROOT-V1"
        public const val FINGERPRINT_LABEL: String = "DLL17-CONTAINER-FINGERPRINT-V1"

        private fun keyStore(): KeyStore =
            KeyStore.getInstance(PROVIDER).apply { load(null) }

        /** Whether container material exists. Does not create it. */
        public fun exists(alias: String = DEFAULT_ALIAS): Boolean =
            keyStore().containsAlias(alias)

        /**
         * Opens existing container material, or returns `null` if there is none.
         *
         * Never generates. See the class comment: generating here is how a
         * cryptographic failure becomes a new organism.
         */
        public fun openExisting(alias: String = DEFAULT_ALIAS): AndroidKeystoreDeviceKeyContainer? {
            val store = keyStore()
            if (!store.containsAlias(alias)) return null
            val key = try {
                store.getKey(alias, null) as? SecretKey
            } catch (unrecoverable: UnrecoverableKeyException) {
                throw KeyStateFault(
                    KeyFault.CONTAINER_UNAVAILABLE,
                    "keystore key '$alias' exists but is unrecoverable: ${unrecoverable.message}",
                )
            } ?: throw KeyStateFault(
                KeyFault.CONTAINER_UNAVAILABLE,
                "keystore alias '$alias' does not hold a secret key",
            )
            return build(alias, key)
        }

        /**
         * Creates container material for a new organism.
         *
         * Refuses if material already exists: overwriting it orphans every
         * record the existing wrap protects.
         */
        public fun create(
            alias: String = DEFAULT_ALIAS,
            requestStrongBox: Boolean = true,
        ): AndroidKeystoreDeviceKeyContainer {
            if (exists(alias)) {
                throw KeyStateFault(
                    KeyFault.CONTAINER_UNAVAILABLE,
                    "container material '$alias' already exists; creating would orphan records",
                )
            }
            val key = generate(alias, requestStrongBox)
            return build(alias, key)
        }

        /** Opens existing material, creating it only when there is none at all. */
        public fun openOrCreate(
            alias: String = DEFAULT_ALIAS,
            requestStrongBox: Boolean = true,
        ): AndroidKeystoreDeviceKeyContainer =
            openExisting(alias) ?: create(alias, requestStrongBox)

        private fun generate(alias: String, requestStrongBox: Boolean): SecretKey {
            // StrongBox is reported when present and never required. Requiring
            // it would make the organism unopenable on every device without a
            // secure element, and the canonical contract does not ask for it.
            if (requestStrongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    return generateWith(alias, strongBox = true)
                } catch (unavailable: StrongBoxUnavailableException) {
                    // Fall through to the ordinary TEE- or software-backed key.
                } catch (failure: Exception) {
                    // Some implementations report StrongBox absence as a generic
                    // provider failure rather than the documented exception.
                }
            }
            return generateWith(alias, strongBox = false)
        }

        private fun generateWith(alias: String, strongBox: Boolean): SecretKey {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, PROVIDER)
            val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
                .setKeySize(256)
                .setDigests(KeyProperties.DIGEST_SHA256)
                // No user-authentication binding. A creature that becomes
                // unreadable because the owner changed their screen lock is a
                // creature the product killed, and the canonical contract binds
                // local storage to the device, not to an unlock event.
                .setUserAuthenticationRequired(false)
                .apply {
                    if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        setIsStrongBoxBacked(true)
                    }
                }
                .build()
            generator.init(spec)
            return generator.generateKey()
        }

        private fun build(alias: String, key: SecretKey): AndroidKeystoreDeviceKeyContainer {
            val fingerprint = try {
                val mac = Mac.getInstance(MAC_ALGORITHM).apply { init(key) }
                val digest = mac.doFinal(FINGERPRINT_LABEL.toByteArray(Charsets.US_ASCII))
                ByteBuffer.wrap(digest, 0, 8).long
            } catch (invalidated: KeyPermanentlyInvalidatedException) {
                throw KeyStateFault(
                    KeyFault.CONTAINER_UNAVAILABLE,
                    "keystore key '$alias' is permanently invalidated",
                )
            }
            return AndroidKeystoreDeviceKeyContainer(alias, key, fingerprint, describeBacking(key))
        }

        /**
         * What the platform says about this key's protection.
         *
         * Reported, never asserted. `PersistenceBackendContractV1` and
         * `LocalStorageCryptographyContractV1` require non-exportability, which
         * the AndroidKeyStore provider gives on every backing; they do not
         * require a secure element, and turning an observation into a
         * correctness requirement would exclude devices the canonical contract
         * includes.
         */
        private fun describeBacking(key: SecretKey): String = try {
            val factory = SecretKeyFactory.getInstance(key.algorithm, PROVIDER)
            val info = factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                when (info.securityLevel) {
                    KeyProperties.SECURITY_LEVEL_STRONGBOX -> "STRONGBOX"
                    KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> "TRUSTED_ENVIRONMENT"
                    KeyProperties.SECURITY_LEVEL_SOFTWARE -> "SOFTWARE"
                    KeyProperties.SECURITY_LEVEL_UNKNOWN_SECURE -> "UNKNOWN_SECURE"
                    else -> "UNKNOWN"
                }
            } else {
                @Suppress("DEPRECATION")
                if (info.isInsideSecureHardware) "SECURE_HARDWARE" else "SOFTWARE"
            }
        } catch (failure: Exception) {
            "UNREPORTED"
        }
    }
}
