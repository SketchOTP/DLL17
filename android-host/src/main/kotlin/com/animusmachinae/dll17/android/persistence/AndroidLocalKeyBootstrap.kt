package com.animusmachinae.dll17.android.persistence

import com.animusmachinae.dll17.core.persistence.DeviceKeyContainer
import com.animusmachinae.dll17.core.persistence.KeyFault
import com.animusmachinae.dll17.core.persistence.KeyStateFault
import com.animusmachinae.dll17.core.persistence.LocalKeyStore
import com.animusmachinae.dll17.core.persistence.Quarantine
import com.animusmachinae.dll17.core.persistence.WrappedKeyState
import java.io.File

/**
 * Startup key resolution on Android, and the one decision it must never get
 * wrong.
 *
 * `LocalStorageCryptographyContractV1` states the rule the whole of this file
 * exists to enforce: **a cryptographic failure never creates a new organism.**
 * `LocalKeyStore` already refuses at the level of a single operation. What it
 * cannot see is the shape of the *startup* decision, which is where the
 * dangerous ambiguity lives: a device with no readable key material looks
 * exactly like a device that has never had an organism on it, and the natural
 * reading of "nothing here" is "so create one".
 *
 * This class makes that reading impossible by consulting two independent
 * witnesses before it will permit a birth: the persisted key state on disk, and
 * the container material in the keystore. A birth is permitted only when *both*
 * are absent. Any other combination is either an ordinary open or a quarantine.
 *
 * | key state | container material | outcome |
 * |---|---|---|
 * | absent | absent | [Outcome.BIRTH_PERMITTED] |
 * | absent | present | [Outcome.BIRTH_PERMITTED], reusing the container |
 * | present | absent | [Outcome.QUARANTINED] — never a birth |
 * | present | present, unusable | [Outcome.QUARANTINED] |
 * | present | present, usable | [Outcome.OPENED] |
 *
 * The second row is the only one that needs defending. Container material with
 * no key state is not a lost organism: there is nothing on disk that the missing
 * state could have protected. It happens after the app's data is cleared while
 * the keystore entry survives, and refusing it would strand the installation
 * with no way forward but an uninstall.
 */
public class AndroidLocalKeyBootstrap(
    private val keyDirectory: File,
    private val organismId: Long,
    private val alias: String = AndroidKeystoreDeviceKeyContainer.DEFAULT_ALIAS,
    /** Injectable so the decision table can be qualified without a keystore. */
    private val containerPresent: (String) -> Boolean = { AndroidKeystoreDeviceKeyContainer.exists(it) },
    private val openContainer: (String) -> DeviceKeyContainer? = {
        AndroidKeystoreDeviceKeyContainer.openExisting(it)
    },
    private val createContainer: (String) -> DeviceKeyContainer = {
        AndroidKeystoreDeviceKeyContainer.openOrCreate(it)
    },
) {

    public enum class Outcome {
        /** Existing state opened under existing container material. */
        OPENED,

        /** No organism has ever existed here. A birth may proceed. */
        BIRTH_PERMITTED,

        /** State exists and cannot be opened. Refused and recorded. */
        QUARANTINED,

        /** A previous refusal is still in force. */
        ALREADY_QUARANTINED,
    }

    public class Resolution(
        public val outcome: Outcome,
        public val fault: KeyFault,
        public val detail: String,
        /** Present only for [Outcome.OPENED]. */
        public val state: WrappedKeyState?,
        public val container: DeviceKeyContainer?,
    ) {
        /** The only outcome under which a caller may write a first record. */
        public val mayCreateOrganism: Boolean get() = outcome == Outcome.BIRTH_PERMITTED
    }

    public fun resolve(): Resolution {
        if (Quarantine.isQuarantined(keyDirectory)) {
            return Resolution(
                Outcome.ALREADY_QUARANTINED,
                KeyFault.CONTAINER_UNAVAILABLE,
                Quarantine.reason(keyDirectory) ?: "quarantined",
                state = null,
                container = null,
            )
        }

        val stateExists = File(
            keyDirectory,
            com.animusmachinae.dll17.core.persistence.PersistenceBackendContract.KEYSTATE_FILE,
        ).exists()
        val materialExists = containerPresent(alias)

        if (!stateExists) {
            // No key state means no protected records, so there is nothing a
            // wrong decision here could orphan.
            val container = (if (materialExists) openContainer(alias) else createContainer(alias))
                ?: return quarantine(
                    KeyFault.CONTAINER_UNAVAILABLE,
                    "container material vanished between the check and the open",
                )
            return Resolution(
                Outcome.BIRTH_PERMITTED,
                KeyFault.NONE,
                if (materialExists) "reusing existing container material" else "created container material",
                state = null,
                container = container,
            )
        }

        if (!materialExists) {
            // The dangerous row. Key state protects records that exist, and the
            // material that opens it is gone. Generating a replacement would
            // produce a container that works perfectly and opens nothing.
            return quarantine(
                KeyFault.CONTAINER_UNAVAILABLE,
                "key state is present and container material '$alias' is absent",
            )
        }

        val container = try {
            openContainer(alias)
        } catch (fault: KeyStateFault) {
            return quarantine(fault.fault, fault.message ?: "container refused")
        } ?: return quarantine(
            KeyFault.CONTAINER_UNAVAILABLE,
            "container material '$alias' could not be opened",
        )

        val store = LocalKeyStore(keyDirectory, container, organismId)
        return try {
            // An interrupted rewrap is resolved before anything is unwrapped, so
            // a rotation that died halfway cannot present as an unauthentic key.
            val resolved = store.resumeRewrap(store.load())
            store.unwrap(resolved)
            Resolution(Outcome.OPENED, KeyFault.NONE, "opened at epoch ${resolved.keyEpoch}", resolved, container)
        } catch (fault: KeyStateFault) {
            quarantine(fault.fault, fault.message ?: "key state refused")
        }
    }

    private fun quarantine(fault: KeyFault, detail: String): Resolution {
        // Key state is retained through a quarantine. Deleting it would destroy
        // the only thing a later cold recovery could verify against.
        Quarantine.mark(keyDirectory, QUARANTINE_REASONS.getValue(fault), detail)
        return Resolution(Outcome.QUARANTINED, fault, detail, state = null, container = null)
    }

    private companion object {
        val QUARANTINE_REASONS: Map<KeyFault, String> = mapOf(
            KeyFault.NONE to "UNKNOWN",
            KeyFault.CONTAINER_UNAVAILABLE to "CONTAINER_UNAVAILABLE",
            KeyFault.WRAPPED_KEY_UNAUTHENTIC to "WRAPPED_KEY_UNAUTHENTIC",
            KeyFault.EPOCH_MISMATCH to "EPOCH_MISMATCH",
            KeyFault.DEVICE_MISMATCH to "DEVICE_MISMATCH",
        )
    }
}
