package com.animusmachinae.dll17.android

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Android backup and device-transfer exclusion, per
 * `ContinuityDurabilityContractV1` section 14.1.
 *
 * Structural rather than behavioural on purpose. The rules take effect inside
 * the platform's backup transport, which cannot be exercised from a unit test —
 * but the failure mode is a missing declaration, and a missing declaration is
 * exactly what a structural check catches. Auto Backup is opt-out by default, so
 * the absence of these files is silently permissive.
 */
class BackupExclusionTest {

    private val moduleRoot = File("").absoluteFile

    private fun read(relative: String): String {
        val file = File(moduleRoot, relative)
        assertTrue("missing required file: $relative", file.isFile)
        return file.readText()
    }

    @Test
    fun theManifestDisablesBackupAndDeclaresBothRuleSets() {
        val manifest = read("src/main/AndroidManifest.xml")
        assertTrue(
            "allowBackup must be explicitly false",
            manifest.contains("android:allowBackup=\"false\""),
        )
        assertTrue(
            "API 31+ needs dataExtractionRules",
            manifest.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\""),
        )
        assertTrue(
            "API 30 and lower need fullBackupContent",
            manifest.contains("android:fullBackupContent=\"@xml/backup_rules\""),
        )
    }

    @Test
    fun bothBackupAndDeviceTransferExcludeCanonicalState() {
        // Excluding canonical state from cloud backup while leaving device
        // transfer open would copy a live organism by a different route.
        val rules = read("src/main/res/xml/data_extraction_rules.xml")
        for (section in listOf("<cloud-backup>", "<device-transfer>")) {
            assertTrue("missing $section section", rules.contains(section))
        }
        val cloud = rules.substringAfter("<cloud-backup>").substringBefore("</cloud-backup>")
        val transfer = rules.substringAfter("<device-transfer>").substringBefore("</device-transfer>")
        for (section in listOf(cloud, transfer)) {
            for (path in CANONICAL_PATHS) {
                assertTrue("canonical path $path is not excluded", section.contains(path))
            }
            assertTrue(
                "the canonical database is not excluded",
                section.contains("domain=\"database\""),
            )
        }
    }

    @Test
    fun theLegacyRulesExcludeTheSameCanonicalState() {
        val legacy = read("src/main/res/xml/backup_rules.xml")
        for (path in CANONICAL_PATHS) {
            assertTrue("canonical path $path is not excluded for API 30", legacy.contains(path))
        }
    }

    private companion object {
        val CANONICAL_PATHS = listOf(
            "canonical/",
            "journal/",
            "checkpoints/",
            "identity/",
            "keys/",
            "recovery/",
        )
    }
}
