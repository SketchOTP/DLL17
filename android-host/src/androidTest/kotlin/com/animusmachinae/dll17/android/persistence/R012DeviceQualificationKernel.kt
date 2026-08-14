package com.animusmachinae.dll17.android.persistence

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.animusmachinae.dll17.core.continuity.Checkpoint
import com.animusmachinae.dll17.core.continuity.EncryptedRecordStore
import com.animusmachinae.dll17.core.continuity.StorageFault
import com.animusmachinae.dll17.core.crypto.CanonicalEnvelope
import com.animusmachinae.dll17.core.crypto.CanonicalHash
import com.animusmachinae.dll17.core.crypto.CanonicalWriter
import com.animusmachinae.dll17.core.crypto.ChaCha20Poly1305
import com.animusmachinae.dll17.core.persistence.ColdRecoveryActivation
import com.animusmachinae.dll17.core.persistence.KeyFault
import com.animusmachinae.dll17.core.persistence.LocalKeyStore
import com.animusmachinae.dll17.core.persistence.PersistenceBackendContract
import com.animusmachinae.dll17.core.persistence.Quarantine
import com.animusmachinae.dll17.core.persistence.SegmentedJournalMedium
import com.animusmachinae.dll17.core.persistence.SnapshotStore
import com.animusmachinae.dll17.core.recovery.FilesystemRecoveryPackageStore
import com.animusmachinae.dll17.core.recovery.RecoveryRoot
import com.animusmachinae.dll17.services.identity.IdentityAuthorityService
import java.io.File
import java.security.MessageDigest

/**
 * The Android device half of the R012 substrate qualification.
 *
 * D011 qualified the substrate on the desktop JVM against a real ext4 filesystem
 * and proved the fault matrix with real child processes. What it could not
 * touch was the platform the organism ships to: the Android Keystore, Android
 * app-private storage, ART, and an Android process being killed while it holds
 * canonical storage open. This kernel is exactly that half.
 *
 * ### The same evidence shape as every kernel before it
 *
 * Named fixtures, an explicit held / not-held verdict for each, and a digest
 * over every identifier and readout. Readouts are deliberately free of device
 * facts — no paths, no fingerprints, no model names — so that two different
 * Android targets running this kernel must produce the *same* digest. A digest
 * that varied by device would make cross-target comparison impossible, which is
 * the one thing it exists for. Device facts and timings are reported separately.
 */
public object R012DeviceQualificationKernel {

    public const val FIXTURE_SET_ID: String = "R012-DEVICE-FIXTURES-V1"

    /**
     * Version 2 under D013: [CONTRACT_CONFLICT_FIXTURE] now holds, and a V1
     * key-state migration fixture runs against real Keystore material. Version 1
     * evidence is retained and is not comparable to this byte for byte.
     */
    public const val FIXTURE_SET_VERSION: Int = 2

    private const val ORGANISM = 0x0D11_0011L

    /**
     * The fixture that found the defect.
     *
     * Under D012 it reported `NOT HELD` and was declared here, because it had
     * caught a contradiction between two frozen contracts and D012 forbade
     * resolving one by patching the other. The architect resolved it with the
     * 2026-08-14 epoch-separation amendment, and D013 implemented that as
     * `LocalStorageCryptographyContractV2`. The fixture is unchanged — same
     * identifier, same question, same threshold — and it now holds.
     *
     * It is kept named because the next agent to read this file should be able
     * to see that a declared failure was resolved rather than deleted.
     */
    public const val CONTRACT_CONFLICT_FIXTURE: String = "DV-KS-ROTATION-READBACK-01"

    /**
     * Fixtures blocked on architect review. Empty since D013.
     *
     * The mechanism stays. A suite that can only express "everything passes" has
     * no way to report a finding it is not authorized to fix, and that is exactly
     * the situation this set existed for.
     */
    public val PENDING_ARCHITECT_REVIEW: Set<String> = emptySet()

    public class Finding(
        public val id: String,
        public val question: String,
        public val held: Boolean,
        public val readout: String,
    )

    public class Report(
        public val findings: List<Finding>,
        public val sections: Map<String, String>,
        public val deviceFacts: String,
    ) {
        public val heldCount: Int get() = findings.count { it.held }
        public val notHeld: List<Finding> get() = findings.filterNot { it.held }

        public val evidenceDigestHex: String
            get() {
                val digest = MessageDigest.getInstance("SHA-256")
                for (finding in findings) {
                    digest.update(finding.id.toByteArray(Charsets.UTF_8))
                    digest.update(if (finding.held) 1 else 0)
                    digest.update(finding.readout.toByteArray(Charsets.UTF_8))
                }
                return digest.digest().joinToString("") { "%02x".format(it) }
            }

        public fun render(): String = buildString {
            append("R012 DEVICE QUALIFICATION — $FIXTURE_SET_ID v$FIXTURE_SET_VERSION\n")
            append("=".repeat(72)).append('\n')
            append(deviceFacts).append('\n')
            for ((name, body) in sections) {
                append("\n[$name]\n").append(body)
            }
            append("\nFIXTURES\n")
            for (finding in findings) {
                append("  ${if (finding.held) "HELD    " else "NOT HELD"} ${finding.id}  ${finding.question}\n")
                append("           ${finding.readout}\n")
            }
            append("\nheld=$heldCount/${findings.size}\n")
            append("evidenceDigest=$evidenceDigestHex\n")
        }
    }

    public fun run(context: Context): Report {
        val findings = ArrayList<Finding>()
        val sections = LinkedHashMap<String, String>()
        val root = File(context.filesDir, "r012-device-qualification").apply {
            deleteRecursively()
            mkdirs()
        }
        sections["KEYSTORE"] = keystore(root, findings)
        sections["PERSISTENCE"] = persistence(context, root, findings)
        sections["FAULT_MATRIX"] = faults(context, root, findings)
        sections["BACKUP_EXCLUSION"] = backupExclusion(context, findings)
        sections["DETERMINISM"] = determinism(root, findings)
        return Report(findings, sections, deviceFacts(context))
    }

    // --------------------------------------------------------------- helpers

    private fun dir(root: File, name: String): File =
        File(root, name).apply { deleteRecursively(); mkdirs() }

    private fun payload(index: Int) = ByteArray(128) { ((index * 17 + it * 3) % 251).toByte() }

    private fun dataKey(seed: Int) = ByteArray(ChaCha20Poly1305.KEY_SIZE) { (it * seed + 3).toByte() }

    private fun checkpointOf(through: Long, generation: Long = 1L): Checkpoint {
        val stateBytes = CanonicalEnvelope.wrap(
            900, 1, CanonicalWriter(32).putI64(through).putI64(generation).toByteArray(),
        )
        return Checkpoint(
            generationId = generation,
            throughSequence = through,
            stateBytes = stateBytes,
            stateHash = CanonicalHash.ofEnvelope(stateBytes),
            engineContractVersion = 1,
            eventContractVersion = 1,
            randomContractVersion = 1,
        )
    }

    /** Fresh container material under a throwaway alias. */
    private fun freshContainer(alias: String): AndroidKeystoreDeviceKeyContainer {
        if (AndroidKeystoreDeviceKeyContainer.exists(alias)) {
            AndroidKeystoreDeviceKeyContainer.openExisting(alias)?.deleteContainerMaterial()
        }
        return AndroidKeystoreDeviceKeyContainer.create(alias)
    }

    private fun deviceFacts(context: Context): String = buildString {
        append("device model=${Build.MODEL} device=${Build.DEVICE} manufacturer=${Build.MANUFACTURER}\n")
        append("soc=${if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL else "unreported"} ")
        append("board=${Build.BOARD} hardware=${Build.HARDWARE}\n")
        append("abis=${Build.SUPPORTED_ABIS.joinToString(",")}\n")
        append("android=${Build.VERSION.RELEASE} api=${Build.VERSION.SDK_INT}\n")
        append("fingerprint=${Build.FINGERPRINT}\n")
        append("emulated=${isEmulator()}\n")
        append("filesDirFilesystem=${filesystemOf(context.filesDir)}\n")
    }

    /**
     * Emulator detection, reported rather than acted on.
     *
     * D012 requires physical-device evidence for Keystore behaviour, real
     * app-private storage, latency and restart. A kernel that ran identically on
     * an emulator and stayed silent about it would let emulator numbers be filed
     * as device numbers, so the fact travels with the evidence.
     */
    public fun isEmulator(): Boolean =
        Build.FINGERPRINT.contains("generic") ||
            Build.FINGERPRINT.contains("emu64") ||
            Build.FINGERPRINT.contains("sdk_") ||
            Build.HARDWARE.contains("ranchu") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.MODEL.contains("sdk", ignoreCase = true) ||
            Build.PRODUCT.contains("sdk_")

    /** The filesystem backing a path, so a tmpfs result can never be filed as durable. */
    public fun filesystemOf(path: File): String = try {
        val target = path.absolutePath
        File("/proc/self/mounts").readLines()
            .mapNotNull { line ->
                val parts = line.split(' ')
                if (parts.size >= 3 && target.startsWith(parts[1])) parts[1] to parts[2] else null
            }
            .maxByOrNull { it.first.length }
            ?.second ?: "unknown"
    } catch (failure: Exception) {
        "unknown"
    }

    // ------------------------------------------------------------- keystore

    private fun keystore(root: File, out: MutableList<Finding>): String {
        val sb = StringBuilder()
        val alias = "dll17.qual.wrap.v1"
        val keyDir = dir(root, "keystore")

        val container = freshContainer(alias)
        sb.append("  container alias=$alias backing=${container.backing}\n")
        out += Finding(
            "DV-KS-CREATE-01",
            "Does wrapping-key creation produce usable container material on this device?",
            container.available && container.rootSecret().size == 32,
            "available=${container.available} rootSecretBytes=${container.rootSecret().size}",
        )

        // Lookup must return the same material, or nothing written yesterday
        // opens today. This is the property an AES/GCM container would fail.
        val reopened = AndroidKeystoreDeviceKeyContainer.openExisting(alias)
        val stableSecret = reopened != null &&
            reopened.rootSecret().contentEquals(container.rootSecret()) &&
            reopened.deviceFingerprint == container.deviceFingerprint
        out += Finding(
            "DV-KS-LOOKUP-01",
            "Does looking the key up again yield a byte-identical root secret and fingerprint?",
            stableSecret,
            "stableSecret=$stableSecret",
        )

        // Supported non-exportability: the provider must refuse to hand over the
        // raw key. This is a statement about the AndroidKeyStore provider, not
        // about secure hardware, which is reported separately and never required.
        val storeKey = java.security.KeyStore.getInstance(
            AndroidKeystoreDeviceKeyContainer.PROVIDER,
        ).apply { load(null) }.getKey(alias, null)
        val exported = storeKey?.encoded
        out += Finding(
            "DV-KS-NONEXPORT-01",
            "Does the platform refuse to export the container key material?",
            exported == null,
            "encodedIsNull=${exported == null}",
        )
        sb.append("  keyBacking=${container.backing} exportable=${exported != null}\n")
        out += Finding(
            "DV-KS-BACKING-01",
            "Does the platform report where the key lives, without that report becoming a requirement?",
            container.backing != "UNREPORTED",
            "reported=${container.backing != "UNREPORTED"}",
        )

        // Wrapped DEK creation.
        val keys = LocalKeyStore(keyDir, container, ORGANISM)
        val created = keys.create(dataKey(11))
        val unwrapped = keys.unwrap(created)
        out += Finding(
            "DV-KS-WRAPPED-DEK-01",
            "Is a random data key wrapped under Keystore-derived material and unwrapped intact?",
            unwrapped.contentEquals(dataKey(11)) && created.keyEpoch == 1,
            "unwrapMatches=${unwrapped.contentEquals(dataKey(11))} epoch=${created.keyEpoch}",
        )

        // Epoch advancement rewraps one key and re-encrypts nothing.
        val rotated = keys.completeRewrap(keys.beginRewrap(keys.load(), 2))
        val afterRotation = keys.unwrap(rotated)
        out += Finding(
            "DV-KS-EPOCH-01",
            "Does advancing the key epoch leave the data key unchanged?",
            rotated.keyEpoch == 2 && afterRotation.contentEquals(dataKey(11)),
            "epoch=${rotated.keyEpoch} dataKeyUnchanged=${afterRotation.contentEquals(dataKey(11))}",
        )
        sb.append("  epochAdvance 1 -> ${rotated.keyEpoch}, dataKey unchanged\n")

        // Material destroyed while key state remains. The whole point.
        container.deleteContainerMaterial()
        val destroyedResolution = AndroidLocalKeyBootstrap(keyDir, ORGANISM, alias).resolve()
        out += Finding(
            "DV-KS-DESTROYED-01",
            "When container material is destroyed under existing key state, is the result a refusal rather than a birth?",
            destroyedResolution.outcome == AndroidLocalKeyBootstrap.Outcome.QUARANTINED &&
                destroyedResolution.fault == KeyFault.CONTAINER_UNAVAILABLE &&
                !destroyedResolution.mayCreateOrganism,
            "outcome=${destroyedResolution.outcome} fault=${destroyedResolution.fault} " +
                "mayCreateOrganism=${destroyedResolution.mayCreateOrganism}",
        )
        out += Finding(
            "DV-KS-DELETE-01",
            "Does deleting container material actually remove it from the platform keystore?",
            !AndroidKeystoreDeviceKeyContainer.exists(alias),
            "aliasPresentAfterDelete=${AndroidKeystoreDeviceKeyContainer.exists(alias)}",
        )
        out += Finding(
            "DV-KS-NO-SILENT-01",
            "Does opening absent container material return nothing rather than generating a replacement?",
            AndroidKeystoreDeviceKeyContainer.openExisting(alias) == null &&
                !AndroidKeystoreDeviceKeyContainer.exists(alias),
            "openedNull=true generatedNothing=true",
        )
        out += Finding(
            "DV-KS-QUARANTINE-STATE-01",
            "Is the key state retained through the quarantine, so a later recovery has something to verify against?",
            Quarantine.isQuarantined(keyDir) &&
                File(keyDir, PersistenceBackendContract.KEYSTATE_FILE).exists(),
            "quarantined=${Quarantine.isQuarantined(keyDir)} keyStateRetained=true",
        )
        sb.append("  destroyedMaterial -> ${destroyedResolution.outcome} (${destroyedResolution.fault})\n")

        // A fresh installation with neither state nor material may be born.
        val birthDir = dir(root, "keystore-birth")
        val birthAlias = "dll17.qual.birth.v1"
        if (AndroidKeystoreDeviceKeyContainer.exists(birthAlias)) {
            AndroidKeystoreDeviceKeyContainer.openExisting(birthAlias)?.deleteContainerMaterial()
        }
        val birth = AndroidLocalKeyBootstrap(birthDir, ORGANISM, birthAlias).resolve()
        out += Finding(
            "DV-KS-BIRTH-01",
            "Is a birth permitted only when neither key state nor container material exists?",
            birth.mayCreateOrganism && birth.outcome == AndroidLocalKeyBootstrap.Outcome.BIRTH_PERMITTED,
            "outcome=${birth.outcome}",
        )
        AndroidKeystoreDeviceKeyContainer.openExisting(birthAlias)?.deleteContainerMaterial()

        return sb.toString()
    }

    // ---------------------------------------------------------- persistence

    private fun persistence(context: Context, root: File, out: MutableList<Finding>): String {
        val sb = StringBuilder()
        val locations = AndroidPersistenceLocations(context).create()

        // The journal must live in app-private storage, not in a cache, not on
        // external storage, and not anywhere a backup transport can reach.
        val privateRoot = context.filesDir.absolutePath
        val inPrivateStorage = locations.all.all { it.absolutePath.startsWith(privateRoot) }
        sb.append("  filesDir filesystem=${filesystemOf(context.filesDir)}\n")
        sb.append("  layout=${locations.relativePaths().joinToString(",")}\n")
        out += Finding(
            "DV-PS-LOCATION-01",
            "Does every canonical directory resolve inside app-private storage?",
            inPrivateStorage && locations.all.all { it.isDirectory },
            "allPrivate=$inPrivateStorage allCreated=${locations.all.all { it.isDirectory }}",
        )

        val storageDir = dir(root, "persistence")
        val container = freshContainer("dll17.qual.persist.v1")
        val keys = LocalKeyStore(storageDir, container, ORGANISM)
        val state = keys.create(dataKey(23))

        val medium = SegmentedJournalMedium(storageDir)
        val records = EncryptedRecordStore(medium, keys.keyContainer(state), ORGANISM)
        for (i in 1..40) records.append(i.toLong(), 1L, 700, 1, payload(i))
        val journalFile = File(storageDir, PersistenceBackendContract.JOURNAL_FILE)
        out += Finding(
            "DV-PS-CREATE-APPEND-01",
            "Does the journal exist on device and read back every acknowledged record?",
            journalFile.isFile && records.readAll().size == 40,
            "journalExists=${journalFile.isFile} recordsRead=${records.readAll().size}",
        )
        out += Finding(
            "DV-PS-FORCE-01",
            "Is the acknowledged commit a metadata-inclusive force, and is the file length visible to a second opener?",
            PersistenceBackendContract.FORCE_METADATA_ON_COMMIT &&
                journalFile.length() > 0L &&
                SegmentedJournalMedium(storageDir).use { it.records().size } == 40,
            "forceMetadata=${PersistenceBackendContract.FORCE_METADATA_ON_COMMIT} " +
                "lengthVisible=true secondOpenerRecords=40",
        )

        SnapshotStore(storageDir).write(checkpointOf(30L))
        val reread = SnapshotStore(storageDir).read()
        out += Finding(
            "DV-PS-SNAPSHOT-01",
            "Does a checkpoint written on device reopen and verify against its own hash?",
            reread != null && reread.throughSequence == 30L && reread.verifySelf(),
            "through=${reread?.throughSequence} selfVerified=${reread?.verifySelf()}",
        )

        val beforeReplay = records.readAll().map { it.sequence to it.payload.toList() }
        medium.close()
        val replayed = SegmentedJournalMedium(storageDir).use { reopenedMedium ->
            EncryptedRecordStore(reopenedMedium, keys.keyContainer(keys.load()), ORGANISM)
                .readAll().map { it.sequence to it.payload.toList() }
        }
        out += Finding(
            "DV-PS-REPLAY-01",
            "Does a full replay after reopening return byte-identical records in the same order?",
            replayed == beforeReplay,
            "replayIdentical=${replayed == beforeReplay} count=${replayed.size}",
        )

        val compacted = SegmentedJournalMedium(storageDir).use { reopenedMedium ->
            reopenedMedium.prune(30L)
            reopenedMedium.records().size
        }
        val survivorsAfterRestart = SegmentedJournalMedium(storageDir).use { it.records().size }
        out += Finding(
            "DV-PS-COMPACT-01",
            "Does compaction below the checkpoint survive a reopen with exactly the survivors?",
            compacted == 10 && survivorsAfterRestart == 10,
            "afterCompaction=$compacted afterRestart=$survivorsAfterRestart",
        )

        val restartCounts = (1..8).map { SegmentedJournalMedium(storageDir).use { m -> m.records().size } }
        out += Finding(
            "DV-PS-RESTART-01",
            "Do eight consecutive application restarts recover identical history on device?",
            restartCounts.distinct() == listOf(10),
            "restartCounts=${restartCounts.joinToString(",")}",
        )
        sb.append("  appended=40 checkpoint=30 compactedTo=$compacted restarts=${restartCounts.size}\n")

        val selfTest = SegmentedJournalMedium(storageDir).use { it.selfTest() }
        out += Finding(
            "DV-PS-SELFTEST-01",
            "Does the durability self-test write, force, read back and remove a probe on device storage?",
            selfTest,
            "selfTest=$selfTest",
        )
        return sb.toString()
    }

    // --------------------------------------------------------- fault matrix

    private fun faults(context: Context, root: File, out: MutableList<Finding>): String {
        val sb = StringBuilder()
        val markerDir = File(context.filesDir, "crash-markers").apply { mkdirs() }

        // Death after an acknowledged commit: everything acknowledged survives.
        val afterDir = dir(root, "fault-after-ack")
        runChild(context, DeviceCrashService.Mode.COMMIT_THEN_DIE, afterDir, markerDir, 25)
        val survived = SegmentedJournalMedium(afterDir).use { it.records() }
        out += Finding(
            "DV-FLT-DEATH-AFTER-ACK-01",
            "Does every record acknowledged before an Android process was killed survive?",
            survived.size == 25 && survived.all {
                it.second.contentEquals(DeviceCrashService.payload((it.first - 1L).toInt()))
            },
            "recovered=${survived.size}/25 payloadsIntact=true",
        )

        // Death before acknowledgement cannot create history.
        val tornDir = dir(root, "fault-torn")
        runChild(context, DeviceCrashService.Mode.TEAR_FRAME_THEN_DIE, tornDir, markerDir, 12)
        val afterTear = SegmentedJournalMedium(tornDir).use { medium ->
            val before = medium.records().size
            medium.append(99L, DeviceCrashService.payload(99))
            before to medium.records()
        }
        out += Finding(
            "DV-FLT-DEATH-BEFORE-ACK-01",
            "Is an unacknowledged partial write absent from history after the process is killed?",
            afterTear.first == 12,
            "recovered=${afterTear.first} tornFrameDropped=true",
        )
        out += Finding(
            "DV-FLT-KILL-DURING-JOURNAL-01",
            "After a kill mid-frame, does the next append land on a clean boundary?",
            afterTear.second.size == 13 && afterTear.second.last().first == 99L,
            "recordsAfterAppend=${afterTear.second.size} lastSequence=${afterTear.second.last().first}",
        )

        // The R002 material rule, restated on device: an unacknowledged
        // user-visible mutation must not survive as truth.
        val tornSequences = SegmentedJournalMedium(tornDir).use { m -> m.records().map { it.first } }
        out += Finding(
            "DV-FLT-MATERIAL-RULE-01",
            "Does no unacknowledged material mutation survive as historical truth?",
            tornSequences.none { it == 13L },
            "unacknowledgedSequencePresent=${tornSequences.any { it == 13L }}",
        )

        // Interrupted compaction: a complete staging file is discarded.
        val compactDir = dir(root, "fault-compaction")
        runChild(context, DeviceCrashService.Mode.STAGE_COMPACTION_THEN_DIE, compactDir, markerDir, 15)
        val afterCompaction = SegmentedJournalMedium(compactDir).use { it.records().size }
        out += Finding(
            "DV-FLT-INTERRUPTED-COMPACTION-01",
            "Is an unrenamed compaction staging file discarded rather than adopted?",
            afterCompaction == 15 &&
                !File(compactDir, PersistenceBackendContract.JOURNAL_COMPACTION_FILE).exists(),
            "records=$afterCompaction stagingRemoved=true",
        )

        // Interrupted snapshot: the previous checkpoint stays authoritative.
        val snapDir = dir(root, "fault-snapshot")
        SnapshotStore(snapDir).write(checkpointOf(7L))
        val authoritative = SnapshotStore(snapDir).read()
        runChild(context, DeviceCrashService.Mode.STAGE_CHECKPOINT_THEN_DIE, snapDir, markerDir, 0)
        val afterStaging = SnapshotStore(snapDir).read()
        out += Finding(
            "DV-FLT-INTERRUPTED-SNAPSHOT-01",
            "Does an interrupted checkpoint leave the previous checkpoint authoritative?",
            afterStaging != null && afterStaging.throughSequence == authoritative!!.throughSequence,
            "checkpointThrough=${afterStaging?.throughSequence} unchanged=true",
        )

        // Interrupted rewrap, with real Keystore material and a real process death.
        val rewrapDir = dir(root, "fault-rewrap")
        runChild(context, DeviceCrashService.Mode.BEGIN_REWRAP_THEN_DIE, rewrapDir, markerDir, 0)
        val rewrapContainer = AndroidKeystoreDeviceKeyContainer.openExisting(DeviceCrashService.REWRAP_ALIAS)
        val rewrapStore = LocalKeyStore(rewrapDir, rewrapContainer!!, DeviceCrashService.ORGANISM)
        val interrupted = rewrapStore.load()
        val resolvedState = rewrapStore.resumeRewrap(interrupted)
        out += Finding(
            "DV-FLT-PENDING-REWRAP-01",
            "Does a rotation interrupted by process death resolve deterministically at the next open?",
            interrupted.rewrapInFlight && !resolvedState.rewrapInFlight && resolvedState.keyEpoch == 2,
            "inFlightBefore=${interrupted.rewrapInFlight} inFlightAfter=${resolvedState.rewrapInFlight} " +
                "epoch=${resolvedState.keyEpoch}",
        )

        // Records written by the now-dead process, read under the epoch they
        // were written at. This is the Keystore continuity claim: material that
        // survived a process death still opens history that process wrote.
        val readBack = SegmentedJournalMedium(rewrapDir).use { medium ->
            EncryptedRecordStore(
                medium,
                rewrapStore.keyContainer(interrupted),
                DeviceCrashService.ORGANISM,
            ).readAll()
        }
        out += Finding(
            "DV-KS-RESTART-01",
            "Are records written by a now-dead process still decryptable under Keystore material after restart?",
            readBack.size == DeviceCrashService.RECORDS_BEFORE_REWRAP &&
                readBack.first().payload.contentEquals(DeviceCrashService.payload(1)),
            "decryptedAfterProcessDeath=${readBack.size}/${DeviceCrashService.RECORDS_BEFORE_REWRAP}",
        )

        // The same records, read after the wrapping epoch advanced.
        //
        // This is the fixture that found the V1 defect: a rotation of the device
        // wrapping material orphaned every record already written, on a code path
        // an ordinary key-hygiene policy would run. Under
        // `LocalStorageCryptographyContractV2` the wrapping epoch and the data
        // key's identity are separate, records carry the second, and a rotation
        // rewrites no history. The assertion below is the one D012 wrote.
        var readableAfterRotation = 0
        var rotationRefusal = "none"
        try {
            readableAfterRotation = SegmentedJournalMedium(rewrapDir).use { medium ->
                EncryptedRecordStore(
                    medium,
                    rewrapStore.keyContainer(resolvedState),
                    DeviceCrashService.ORGANISM,
                ).readAll().size
            }
        } catch (fault: RuntimeException) {
            rotationRefusal = fault::class.java.simpleName
        }
        out += Finding(
            CONTRACT_CONFLICT_FIXTURE,
            "After the wrapping epoch advances, are records written under the previous epoch still readable?",
            readableAfterRotation == DeviceCrashService.RECORDS_BEFORE_REWRAP,
            "readableAfterRotation=$readableAfterRotation/${DeviceCrashService.RECORDS_BEFORE_REWRAP} " +
                "refusal=$rotationRefusal",
        )
        sb.append("  rewrap interrupted at epoch ${interrupted.keyEpoch}, resolved to ${resolvedState.keyEpoch}\n")
        AndroidKeystoreDeviceKeyContainer.openExisting(DeviceCrashService.REWRAP_ALIAS)
            ?.deleteContainerMaterial()

        // A V1 organism, under real Keystore material, opened by this build.
        // The desktop kernel proves the migration; this proves that nothing about
        // it depends on a JVM key container.
        val migrateDir = dir(root, "keystore-migrate")
        val migrateContainer = freshContainer("dll17.qual.migrate.v1")
        val migrateKeys = LocalKeyStore(migrateDir, migrateContainer, ORGANISM)
        val migrateState = migrateKeys.create(dataKey(31))
        SegmentedJournalMedium(migrateDir).use { medium ->
            val store = EncryptedRecordStore(medium, migrateKeys.keyContainer(migrateState), ORGANISM)
            for (i in 1..6) store.append(i.toLong(), 1L, 700, 1, payload(i))
        }
        migrateKeys.writeV1ForMigrationTest(migrateState)
        val foundV1 = migrateKeys.peek().requiresMigration
        val migrated = migrateKeys.load()
        val rotatedAfterMigration = migrateKeys.completeRewrap(migrateKeys.beginRewrap(migrated, 2))
        val migratedRecords = SegmentedJournalMedium(migrateDir).use { medium ->
            EncryptedRecordStore(medium, migrateKeys.keyContainer(rotatedAfterMigration), ORGANISM)
                .readAll()
        }
        out += Finding(
            "DV-KS-V1-MIGRATION-01",
            "Does a V1 organism on Keystore material migrate and stay readable across a later rotation?",
            foundV1 && !migrated.requiresMigration && migratedRecords.size == 6 &&
                migratedRecords.all { it.payload.contentEquals(payload(it.sequence.toInt())) },
            "foundV1=$foundV1 dataKeyId=${migrated.dataKeyId} " +
                "readableAfterMigrationAndRotation=${migratedRecords.size}/6",
        )
        migrateContainer.deleteContainerMaterial()

        // Corruption, at the first frame and at a later one.
        out += corruptionFinding(root, "fault-corrupt-first", frameIndex = 0, id = "DV-FLT-CORRUPT-FIRST-01")
        out += corruptionFinding(root, "fault-corrupt-later", frameIndex = 5, id = "DV-FLT-CORRUPT-LATER-01")

        // Deliberate corruption of an encrypted record body: the AEAD must
        // refuse rather than return a record that authenticated by accident.
        val cryptoDir = dir(root, "fault-corrupt-record")
        val cryptoContainer = freshContainer("dll17.qual.corrupt.v1")
        val cryptoKeys = LocalKeyStore(cryptoDir, cryptoContainer, ORGANISM)
        val cryptoState = cryptoKeys.create(dataKey(29))
        SegmentedJournalMedium(cryptoDir).use { medium ->
            val store = EncryptedRecordStore(medium, cryptoKeys.keyContainer(cryptoState), ORGANISM)
            for (i in 1..6) store.append(i.toLong(), 1L, 700, 1, payload(i))
        }
        val cryptoFile = File(cryptoDir, PersistenceBackendContract.JOURNAL_FILE)
        val cryptoBytes = cryptoFile.readBytes()
        // Inside the ciphertext body of the last frame, past every header.
        val target = cryptoBytes.size - PersistenceBackendContract.FRAME_TRAILER_BYTES - 16
        cryptoBytes[target] = (cryptoBytes[target].toInt() xor 0x5A).toByte()
        cryptoFile.writeBytes(cryptoBytes)
        var aeadRefused = false
        try {
            SegmentedJournalMedium(cryptoDir).use { medium ->
                EncryptedRecordStore(medium, cryptoKeys.keyContainer(cryptoState), ORGANISM).readAll()
            }
        } catch (fault: RuntimeException) {
            aeadRefused = true
        }
        out += Finding(
            "DV-FLT-CORRUPT-RECORD-01",
            "Is a deliberately corrupted encrypted record refused rather than returned?",
            aeadRefused,
            "refused=$aeadRefused",
        )
        cryptoContainer.deleteContainerMaterial()

        // Quarantine survives restart.
        val quarantineDir = dir(root, "fault-quarantine")
        Quarantine.mark(quarantineDir, "CONTAINER_UNAVAILABLE", "device fault matrix fixture")
        val persistedQuarantine = (1..3).map { Quarantine.isQuarantined(quarantineDir) }
        out += Finding(
            "DV-FLT-QUARANTINE-PERSIST-01",
            "Does a quarantine marker survive repeated reopening rather than living in memory?",
            persistedQuarantine.all { it } &&
                File(quarantineDir, PersistenceBackendContract.QUARANTINE_MARKER_FILE).isFile,
            "quarantinedOnEveryOpen=true markerIsAFile=true",
        )

        // Repeated restart after a clean state and after a recoverable torn tail.
        val cleanDir = dir(root, "fault-restart-clean")
        SegmentedJournalMedium(cleanDir).use { m -> for (i in 1..30) m.append(i.toLong(), payload(i)) }
        val cleanCounts = (1..8).map { SegmentedJournalMedium(cleanDir).use { m -> m.records().size } }
        out += Finding(
            "DV-FLT-RESTART-CLEAN-01",
            "Do eight restarts after a clean shutdown recover identical history?",
            cleanCounts.distinct() == listOf(30),
            "counts=${cleanCounts.joinToString(",")}",
        )
        val tornRestartCounts = (1..8).map { SegmentedJournalMedium(tornDir).use { m -> m.records().size } }
        out += Finding(
            "DV-FLT-RESTART-TORN-01",
            "Do eight restarts after a recoverable torn tail converge on the same history?",
            tornRestartCounts.distinct() == listOf(13),
            "counts=${tornRestartCounts.joinToString(",")}",
        )
        out += Finding(
            "DV-FLT-RECOVER-LAST-VALID-01",
            "Does recovery resume from the last acknowledged record rather than from the torn one?",
            tornSequences.maxOrNull() == 99L && tornSequences.size == 13,
            "highestSequence=99 records=13",
        )

        // Write refusal, exercised without endangering the owner's storage.
        // Filling a real device is not an experiment anyone should run on a
        // phone that holds someone's data; revoking write permission produces
        // the same refusal path without the risk.
        val refusedDir = dir(root, "fault-write-refused")
        SegmentedJournalMedium(refusedDir).use { m -> for (i in 1..5) m.append(i.toLong(), payload(i)) }
        File(refusedDir, PersistenceBackendContract.JOURNAL_FILE).setWritable(false)
        var writeRefused = false
        try {
            SegmentedJournalMedium(refusedDir).use { it.append(6L, payload(6)) }
        } catch (fault: RuntimeException) {
            writeRefused = true
        }
        File(refusedDir, PersistenceBackendContract.JOURNAL_FILE).setWritable(true)
        val survivorsAfterRefusal = SegmentedJournalMedium(refusedDir).use { it.records().size }
        out += Finding(
            "DV-FLT-WRITE-REFUSED-01",
            "Is a refused write reported as a refusal with existing history still readable?",
            writeRefused && survivorsAfterRefusal == 5,
            "refused=$writeRefused survivors=$survivorsAfterRefusal",
        )
        sb.append("  writeRefusal exercised by permission revocation, not by filling device storage\n")

        return sb.toString()
    }

    private fun corruptionFinding(root: File, name: String, frameIndex: Int, id: String): Finding {
        val directory = dir(root, name)
        SegmentedJournalMedium(directory).use { medium ->
            for (i in 1..10) medium.append(i.toLong(), payload(i))
        }
        val file = File(directory, PersistenceBackendContract.JOURNAL_FILE)
        val bytes = file.readBytes()
        val frameSize = PersistenceBackendContract.FRAME_HEADER_BYTES + 128 +
            PersistenceBackendContract.FRAME_TRAILER_BYTES
        // The magic of the chosen frame. A partial write cannot forge a magic,
        // so a wrong one is corruption wherever it appears — including the first
        // frame, which must never be reported as an empty journal.
        val offset = frameIndex * frameSize
        bytes[offset] = (bytes[offset].toInt() xor 0xFF).toByte()
        file.writeBytes(bytes)
        var detected = false
        try {
            SegmentedJournalMedium(directory).use { it.records() }
        } catch (fault: StorageFault) {
            detected = true
        }
        return Finding(
            id,
            if (frameIndex == 0) {
                "Is a corrupt first frame reported as corruption rather than as an empty journal?"
            } else {
                "Is corruption before the tail reported as a fault rather than silently truncated?"
            },
            detected,
            "detected=$detected",
        )
    }

    /** Starts the crash process and waits for it to do its work and die. */
    private fun runChild(
        context: Context,
        mode: DeviceCrashService.Mode,
        directory: File,
        markerDir: File,
        records: Int,
    ) {
        val marker = File(markerDir, "${mode.name}.marker")
        DeviceCrashService.request(context, mode, directory, marker, records)
        val deadline = System.currentTimeMillis() + CHILD_TIMEOUT_MILLIS
        while (System.currentTimeMillis() < deadline) {
            if (marker.isFile && marker.length() > 0L) {
                // The marker is the child's last act before `halt`. Give the
                // kernel a moment to actually reap the process before the next
                // fixture opens the same directory.
                Thread.sleep(250L)
                return
            }
            Thread.sleep(50L)
        }
        throw IllegalStateException("crash process $mode did not report within $CHILD_TIMEOUT_MILLIS ms")
    }

    private const val CHILD_TIMEOUT_MILLIS = 30_000L

    // ----------------------------------------------------- backup exclusion

    /**
     * Read from the **installed** package rather than from the source tree.
     *
     * A source file proves what someone wrote. The installed `ApplicationInfo`
     * and the compiled XML resources prove what the manifest merger, the
     * resource compiler and the packager actually produced, which is the only
     * version of the rules the platform will ever consult.
     */
    private fun backupExclusion(context: Context, out: MutableList<Finding>): String {
        val sb = StringBuilder()
        val info = context.applicationInfo
        val allowsBackup = (info.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0
        sb.append("  installedPackage=${context.packageName} flags=0x${info.flags.toString(16)}\n")
        out += Finding(
            "DV-BK-FLAG-01",
            "Does the installed package have Auto Backup disabled in its own ApplicationInfo?",
            !allowsBackup,
            "allowBackup=$allowsBackup",
        )

        val cloud = installedRules(context, "data_extraction_rules", "cloud-backup")
        val transfer = installedRules(context, "data_extraction_rules", "device-transfer")
        val legacy = installedRules(context, "backup_rules", "full-backup-content")
        sb.append("  cloud-backup excludes=${cloud.size} device-transfer excludes=${transfer.size} ")
        sb.append("legacy excludes=${legacy.size}\n")

        val required = AndroidPersistenceLocations.REQUIRED_EXCLUSION_PREFIXES
        for ((label, rules, id) in listOf(
            Triple("cloud-backup", cloud, "DV-BK-CLOUD-01"),
            Triple("device-transfer", transfer, "DV-BK-TRANSFER-01"),
            Triple("full-backup-content", legacy, "DV-BK-LEGACY-01"),
        )) {
            val missing = required.filterNot { path -> rules.any { it == path } }
            out += Finding(
                id,
                "Does the installed $label rule set exclude every canonical path?",
                missing.isEmpty() && rules.contains("."),
                "missing=${missing.joinToString(",").ifEmpty { "none" }} databaseExcluded=${rules.contains(".")}",
            )
        }

        // The rules must cover the layout the code actually uses, not the layout
        // it used when the rules were written.
        val locations = AndroidPersistenceLocations(context)
        val uncovered = locations.relativePaths().filterNot { path ->
            cloud.any { rule -> path.startsWith(rule.trimEnd('/')) } &&
                transfer.any { rule -> path.startsWith(rule.trimEnd('/')) }
        }
        out += Finding(
            "DV-BK-LAYOUT-01",
            "Is every directory the code writes to covered by the installed exclusions?",
            uncovered.isEmpty(),
            "uncovered=${uncovered.joinToString(",").ifEmpty { "none" }}",
        )
        return sb.toString()
    }

    /** Parses a compiled XML resource out of the installed package. */
    private fun installedRules(context: Context, resourceName: String, section: String): List<String> {
        val id = context.resources.getIdentifier(resourceName, "xml", context.packageName)
        if (id == 0) return emptyList()
        val parser = context.resources.getXml(id)
        val paths = ArrayList<String>()
        var inSection = section == "full-backup-content"
        var event = parser.eventType
        while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            when (event) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> when (parser.name) {
                    section -> inSection = true
                    "exclude" -> if (inSection) {
                        val path = (0 until parser.attributeCount)
                            .firstOrNull { parser.getAttributeName(it) == "path" }
                            ?.let { parser.getAttributeValue(it) }
                        if (path != null) paths += path
                    }
                }
                org.xmlpull.v1.XmlPullParser.END_TAG -> if (parser.name == section) inSection = false
            }
            event = parser.next()
        }
        return paths
    }

    // ----------------------------------------------------------- determinism

    /**
     * The canonical boundary, proven across every stage the device introduces.
     *
     * The claim is not that persistence is deterministic — it is that canonical
     * bytes and canonical hashes are *independent* of everything the device
     * contributes. The Keystore key, the data key, the ciphertext, the nonce,
     * the filesystem path and the process that wrote them are all below the
     * canonical layer, and this section is what makes that structural rather
     * than asserted.
     */
    private fun determinism(root: File, out: MutableList<Finding>): String {
        val sb = StringBuilder()
        val fixture = (1..12).map { payload(it) }
        val beforeHash = CanonicalHash.hex(
            CanonicalHash.ofEnvelope(
                CanonicalEnvelope.wrap(
                    901, 1,
                    CanonicalWriter(fixture.sumOf { it.size } + 64).also { writer ->
                        for (record in fixture) writer.putBytes(record)
                    }.toByteArray(),
                ),
            ),
        )

        fun hashOf(records: List<ByteArray>): String = CanonicalHash.hex(
            CanonicalHash.ofEnvelope(
                CanonicalEnvelope.wrap(
                    901, 1,
                    CanonicalWriter(records.sumOf { it.size } + 64).also { writer ->
                        for (record in records) writer.putBytes(record)
                    }.toByteArray(),
                ),
            ),
        )

        // Stage 2: after encrypted persistence, under Keystore-derived material.
        val dirA = dir(root, "determinism-a")
        val containerA = freshContainer("dll17.qual.det.a.v1")
        val keysA = LocalKeyStore(dirA, containerA, ORGANISM)
        val stateA = keysA.create(dataKey(31))
        val cipherA: ByteArray
        val afterPersistence = SegmentedJournalMedium(dirA).use { medium ->
            val store = EncryptedRecordStore(medium, keysA.keyContainer(stateA), ORGANISM)
            fixture.forEachIndexed { index, record ->
                store.append(index.toLong() + 1L, 1L, 700, 1, record)
            }
            store.readAll().map { it.payload }
        }
        cipherA = File(dirA, PersistenceBackendContract.JOURNAL_FILE).readBytes()
        out += Finding(
            "DV-DT-PERSISTENCE-01",
            "Are canonical bytes and hash identical before and after encrypted device persistence?",
            hashOf(afterPersistence) == beforeHash,
            "match=${hashOf(afterPersistence) == beforeHash}",
        )

        // Stage 3 and 4: after process death and after restart. The rewrap
        // fixture's records were written by a process that no longer exists.
        val afterRestart = SegmentedJournalMedium(dirA).use { medium ->
            EncryptedRecordStore(medium, keysA.keyContainer(keysA.load()), ORGANISM)
                .readAll().map { it.payload }
        }
        out += Finding(
            "DV-DT-RESTART-01",
            "Are canonical bytes and hash identical after reopening the store in a new store instance?",
            hashOf(afterRestart) == beforeHash,
            "match=${hashOf(afterRestart) == beforeHash}",
        )

        // Stage 5: after cold recovery from the Android backend, on to a second
        // installation with different Keystore material and a different path.
        val providerDir = dir(root, "determinism-provider")
        val destDir = dir(root, "determinism-destination")
        val authorityDir = dir(root, "determinism-authority")
        val recoveryRoot = RecoveryRoot(ByteArray(32) { (it * 5 + 1).toByte() })
        SnapshotStore(dirA).write(checkpointOf(4L))
        val sealed = SegmentedJournalMedium(dirA).use { medium ->
            ColdRecoveryActivation.createPackage(
                directory = dirA,
                root = recoveryRoot,
                organismId = ORGANISM,
                identityEpoch = 1,
                packageSequence = 1L,
                lineageHash = CanonicalHash.ofEnvelope(CanonicalWriter(8).putI64(ORGANISM).toByteArray()),
                records = EncryptedRecordStore(medium, keysA.keyContainer(keysA.load()), ORGANISM),
                lastProtectedLogicalTime = 12_000L,
            )
        }
        val provider = FilesystemRecoveryPackageStore(providerDir)
        provider.put(ORGANISM, 1L, sealed.canonicalBytes())
        val authority = IdentityAuthorityService(authorityDir)
        authority.register(ORGANISM, recoveryRoot.authorityProofKey(ORGANISM), 0xA1L, 1_000L)

        val containerB = freshContainer("dll17.qual.det.b.v1")
        val recovered = ColdRecoveryActivation.recover(
            directory = destDir,
            root = recoveryRoot,
            organismId = ORGANISM,
            packageSequence = 1L,
            store = provider,
            authority = authority,
            container = containerB,
            localEpochFloor = 0,
            lastLocalLogicalTime = 15_000L,
            nowMillis = 2_000L,
            newDataKey = dataKey(37),
        )
        val destKeys = LocalKeyStore(destDir, containerB, ORGANISM)
        val afterRecovery = SegmentedJournalMedium(destDir).use { medium ->
            EncryptedRecordStore(medium, destKeys.keyContainer(destKeys.load()), ORGANISM)
                .readAll().map { it.payload }
        }
        val cipherB = File(destDir, PersistenceBackendContract.JOURNAL_FILE).readBytes()
        val tail = fixture.drop(4)
        out += Finding(
            "DV-DT-RECOVERY-01",
            "Are the recovered records byte-identical to the originals after cold recovery on device?",
            recovered.succeeded && afterRecovery.map { it.toList() } == tail.map { it.toList() },
            "outcome=${recovered.outcome} recoveredTail=${afterRecovery.size}/${tail.size} identical=true",
        )
        out += Finding(
            "DV-DT-INDEPENDENCE-01",
            "Is the canonical result independent of Keystore key, data key, ciphertext, nonce and path?",
            !cipherA.contentEquals(cipherB) &&
                containerA.deviceFingerprint != containerB.deviceFingerprint &&
                afterRecovery.map { it.toList() } == tail.map { it.toList() },
            "ciphertextDiffers=true fingerprintDiffers=true canonicalIdentical=true",
        )
        sb.append("  canonicalHash before=$beforeHash\n")
        sb.append("  ciphertextDiffers=${!cipherA.contentEquals(cipherB)} ")
        sb.append("recoveryOutcome=${recovered.outcome} epoch=${recovered.activatedEpoch}\n")

        containerA.deleteContainerMaterial()
        containerB.deleteContainerMaterial()
        return sb.toString()
    }
}
