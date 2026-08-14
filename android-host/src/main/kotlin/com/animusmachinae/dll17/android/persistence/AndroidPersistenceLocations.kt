package com.animusmachinae.dll17.android.persistence

import android.content.Context
import java.io.File

/**
 * Where canonical state lives in app-private storage.
 *
 * This is deliberately the thinnest thing that could be called an adapter. It
 * resolves directories and nothing else: no framing, no ordering, no durability
 * policy, no key handling. All of that is `PersistenceBackendContractV1` and
 * `LocalStorageCryptographyContractV1`, already frozen and already qualified off
 * device, and moving any of it here would put canonical behaviour behind a
 * platform boundary where it cannot be proven by a desktop test.
 *
 * ### Why `filesDir/canonical/` and not `noBackupFilesDir`
 *
 * `noBackupFilesDir` is excluded from Auto Backup by the platform, which sounds
 * stronger. It is weaker for this purpose: the exclusion would then be implicit
 * in a platform convention rather than declared in the two rule files the
 * package ships, and the declared rules are what
 * `ContinuityDurabilityContractV1` section 14.1 requires and what the built
 * package is verified against. Everything canonical therefore sits under a path
 * that appears verbatim in `data_extraction_rules.xml` and `backup_rules.xml`.
 *
 * The subdirectory names are the same ones those rule files exclude at top
 * level, so the layout is covered twice over: once by the `canonical/` prefix
 * and once by its own name.
 */
public class AndroidPersistenceLocations(private val filesDir: File) {

    public constructor(context: Context) : this(context.filesDir)

    /** The single root under which nothing may escape a backup exclusion. */
    public val canonicalRoot: File = File(filesDir, CANONICAL)

    /** Journal and its compaction staging file. */
    public val journalDirectory: File = File(canonicalRoot, JOURNAL)

    /** Checkpoint and its staging file. */
    public val checkpointDirectory: File = File(canonicalRoot, CHECKPOINTS)

    /** Wrapped key state, its staging file, and the quarantine marker. */
    public val keyDirectory: File = File(canonicalRoot, KEYS)

    /** Local staging for cold recovery packages. Never the authority for one. */
    public val recoveryDirectory: File = File(canonicalRoot, RECOVERY)

    /** Identity epoch and lease state. Never organism content. */
    public val identityDirectory: File = File(canonicalRoot, IDENTITY)

    public val all: List<File> = listOf(
        journalDirectory,
        checkpointDirectory,
        keyDirectory,
        recoveryDirectory,
        identityDirectory,
    )

    /**
     * Creates the layout.
     *
     * Failure throws rather than falling back to a different location. A silent
     * fallback to external storage, a cache directory or a temporary path is how
     * canonical state ends up somewhere a backup transport can read it.
     */
    public fun create(): AndroidPersistenceLocations {
        for (directory in all) {
            if (!directory.isDirectory && !directory.mkdirs()) {
                throw IllegalStateException("cannot create ${directory.absolutePath}")
            }
        }
        return this
    }

    /**
     * The path of each directory relative to `filesDir`, which is the form the
     * backup rules express exclusions in. Used by the verification that the
     * declared rules actually cover the layout the code uses, rather than
     * covering paths that were true when the rules were written.
     */
    public fun relativePaths(): List<String> =
        all.map { it.absolutePath.removePrefix(filesDir.absolutePath).trimStart('/') }

    public companion object {
        public const val CANONICAL: String = "canonical"
        public const val JOURNAL: String = "journal"
        public const val CHECKPOINTS: String = "checkpoints"
        public const val KEYS: String = "keys"
        public const val RECOVERY: String = "recovery"
        public const val IDENTITY: String = "identity"

        /**
         * The exclusion prefixes the shipped rule files must contain for this
         * layout to be covered. Kept next to the layout so the two cannot drift
         * apart without a test noticing.
         */
        public val REQUIRED_EXCLUSION_PREFIXES: List<String> = listOf(
            "$CANONICAL/",
            "$JOURNAL/",
            "$CHECKPOINTS/",
            "$IDENTITY/",
            "$KEYS/",
            "$RECOVERY/",
        )
    }
}
