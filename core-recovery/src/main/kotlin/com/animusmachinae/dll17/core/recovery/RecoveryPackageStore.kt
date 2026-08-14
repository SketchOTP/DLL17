package com.animusmachinae.dll17.core.recovery

import com.animusmachinae.dll17.core.crypto.CanonicalHash
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * `RecoveryPackageStoreContractV1`.
 *
 * Provider-neutral, and deliberately small. A provider stores bytes and confirms
 * what it stored. It is never canonical authority for anything else: it cannot
 * say what the organism is, when it lived, or whether a package is current — the
 * manifest says that, and the manifest is authenticated with a key the provider
 * does not have.
 *
 * The rule that shapes the whole interface: **ordinary local organism operation
 * must not depend on any of this**. Every operation may fail, and every failure
 * is an ordinary outcome that leaves the local organism untouched.
 */
public object RecoveryPackageStoreContract {

    public const val CONTRACT_ID: String = "RecoveryPackageStoreContractV1"
    public const val CONTRACT_VERSION: Int = 1

    /** Object naming. Fixed so a destination device can find a package cold. */
    public fun objectName(organismId: Long, packageSequence: Long): String =
        "dll17-%016x-%012d.pkg".format(organismId, packageSequence)
}

/** Why a provider operation did not succeed. Never conflated with corruption. */
public enum class ProviderOutcome(public val ordinal32: Int) {
    OK(1),
    NETWORK_INTERRUPTED(2),
    PROVIDER_UNAVAILABLE(3),
    PARTIAL_WRITE(4),
    NOT_FOUND(5),
    REJECTED(6),
    CANCELLED(7),
}

public class ProviderException(
    public val outcome: ProviderOutcome,
    message: String,
) : RuntimeException(message)

/** What a provider confirms after storing an object. */
public class StoredObjectReceipt(
    public val receiptIdentifier: String,
    public val objectName: String,
    public val objectSizeBytes: Long,
    public val checksumHex: String,
    public val packageSequence: Long,
)

/** A listed object, for current-point lookup. */
public class StoredObjectSummary(
    public val objectName: String,
    public val packageSequence: Long,
    public val objectSizeBytes: Long,
)

/**
 * The provider interface.
 *
 * `put` must be idempotent on `(organismId, packageSequence)`: a retry after an
 * ambiguous network failure has to be safe, because the caller genuinely cannot
 * tell whether the first attempt landed.
 */
public interface RecoveryPackageStoreV1 {
    public val providerId: String

    public fun put(organismId: Long, packageSequence: Long, bytes: ByteArray): StoredObjectReceipt

    public fun get(organismId: Long, packageSequence: Long): ByteArray

    public fun list(organismId: Long): List<StoredObjectSummary>

    /** The highest stored sequence, or null. Providers that cannot list may return null. */
    public fun currentPoint(organismId: Long): StoredObjectSummary?

    public fun delete(organismId: Long, packageSequence: Long)
}

/**
 * The qualifying provider: a directory-backed object store.
 *
 * This is a real provider, not a mock. It writes real bytes through a staging
 * file and an atomic rename, it survives process death, and it is what the
 * end-to-end cold-recovery qualification actually runs against. What it is not
 * is a *network* provider: a user-chosen cloud backend is a later integration
 * that must satisfy this same contract and pass the same conformance suite, and
 * choosing one is a product decision that needs credentials and an owner.
 *
 * The failure switches are here rather than in a subclass because every
 * interesting property of a recovery provider is a statement about a failure,
 * and a failure that needs a real network outage to reproduce is a property
 * nobody re-tests.
 */
public class FilesystemRecoveryPackageStore(
    private val root: File,
) : RecoveryPackageStoreV1 {

    override val providerId: String = "FILESYSTEM_OBJECT_STORE_V1"

    /** Fails the next call with the given outcome, then clears itself. */
    public var failNextWith: ProviderOutcome? = null

    /** Writes a truncated object and then fails. Models an interrupted upload. */
    public var truncateNextWrite: Boolean = false

    /** Refuses every call. Models a provider outage. */
    public var unavailable: Boolean = false

    init {
        if (!root.exists() && !root.mkdirs()) {
            throw ProviderException(
                ProviderOutcome.PROVIDER_UNAVAILABLE,
                "cannot create provider root ${root.absolutePath}",
            )
        }
    }

    private fun checkAvailable() {
        if (unavailable) {
            throw ProviderException(ProviderOutcome.PROVIDER_UNAVAILABLE, "provider is unavailable")
        }
        failNextWith?.let {
            failNextWith = null
            throw ProviderException(it, "injected provider failure: $it")
        }
    }

    private fun objectFile(organismId: Long, packageSequence: Long): File =
        File(root, RecoveryPackageStoreContract.objectName(organismId, packageSequence))

    override fun put(
        organismId: Long,
        packageSequence: Long,
        bytes: ByteArray,
    ): StoredObjectReceipt {
        checkAvailable()
        val target = objectFile(organismId, packageSequence)
        val staging = File(root, target.name + ".staging")
        try {
            if (truncateNextWrite) {
                truncateNextWrite = false
                staging.writeBytes(bytes.copyOf(bytes.size / 2))
                throw ProviderException(
                    ProviderOutcome.PARTIAL_WRITE,
                    "upload interrupted after ${bytes.size / 2} of ${bytes.size} bytes",
                )
            }
            staging.writeBytes(bytes)
            // Atomic rename, so a reader never observes a half-written object and
            // an interrupted upload leaves the previous object intact. This is
            // also what makes a retry idempotent.
            Files.move(
                staging.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (io: IOException) {
            staging.delete()
            throw ProviderException(ProviderOutcome.NETWORK_INTERRUPTED, "upload failed: ${io.message}")
        }
        return StoredObjectReceipt(
            receiptIdentifier = CanonicalHash.hex(CanonicalHash.ofEnvelope(bytes)).take(32),
            objectName = target.name,
            objectSizeBytes = target.length(),
            checksumHex = CanonicalHash.hex(CanonicalHash.ofEnvelope(bytes)),
            packageSequence = packageSequence,
        )
    }

    override fun get(organismId: Long, packageSequence: Long): ByteArray {
        checkAvailable()
        val target = objectFile(organismId, packageSequence)
        if (!target.exists()) {
            throw ProviderException(
                ProviderOutcome.NOT_FOUND,
                "no stored package for sequence $packageSequence",
            )
        }
        return target.readBytes()
    }

    override fun list(organismId: Long): List<StoredObjectSummary> {
        checkAvailable()
        val prefix = "dll17-%016x-".format(organismId)
        return (root.listFiles() ?: emptyArray())
            .filter { it.isFile && it.name.startsWith(prefix) && it.name.endsWith(".pkg") }
            .map {
                StoredObjectSummary(
                    objectName = it.name,
                    packageSequence = it.name.removePrefix(prefix).removeSuffix(".pkg").toLong(),
                    objectSizeBytes = it.length(),
                )
            }
            .sortedBy { it.packageSequence }
    }

    override fun currentPoint(organismId: Long): StoredObjectSummary? =
        list(organismId).lastOrNull()

    override fun delete(organismId: Long, packageSequence: Long) {
        checkAvailable()
        val target = objectFile(organismId, packageSequence)
        // Deleting something that is already gone is a success, not an error:
        // a retried revocation must converge rather than fail.
        target.delete()
    }
}
