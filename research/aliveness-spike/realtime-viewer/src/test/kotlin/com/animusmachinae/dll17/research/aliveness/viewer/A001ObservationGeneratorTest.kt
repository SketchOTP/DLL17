package com.animusmachinae.dll17.research.aliveness.viewer

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class A001ObservationGeneratorTest {
    @Test
    fun `generator creates exactly twelve matched cases per comparison`() {
        val root = Files.createTempDirectory("a001-observations")
        val cases = A001EvaluatorObservationGeneratorV1.generateAll(root)

        assertEquals(24, cases.size)
        assertEquals(12, Files.list(root.resolve("research/aliveness-spike/evidence/a001-v2/formal-input/calibration")).use { it.count() })
        assertEquals(12, Files.list(root.resolve("research/aliveness-spike/evidence/a001-v2/formal-input/qualification")).use { it.count() })
    }

    @Test
    fun `rendered bundles contain only neutral outward data`() {
        val root = Files.createTempDirectory("a001-observations")
        A001EvaluatorObservationGeneratorV1.generateAll(root)
        val files = Files.walk(root).use { it.filter { path -> path.fileName.toString().endsWith(".txt") }.toList() }
        val forbidden = listOf("FULL", "ScriptedPetBaselineV1", "DegradedScriptedControlV1", "learning", "mechanism", "cohort")
        for (file in files) {
            val text = Files.readString(file)
            assertTrue(text.contains("CREATURE_A\n"))
            assertTrue(text.contains("CREATURE_B\n"))
            assertFalse(forbidden.any { text.contains(it, ignoreCase = true) }, file.toString())
        }
    }

    @Test
    fun `same seed and protocol regenerate byte-identically`() {
        val first = Files.createTempDirectory("a001-observations")
        val second = Files.createTempDirectory("a001-observations")
        A001EvaluatorObservationGeneratorV1.generateAll(first)
        A001EvaluatorObservationGeneratorV1.generateAll(second)
        val firstFiles = Files.walk(first).use { it.filter { p -> p.fileName.toString().endsWith(".txt") }.sorted().toList() }
        val secondFiles = Files.walk(second).use { it.filter { p -> p.fileName.toString().endsWith(".txt") }.sorted().toList() }
        assertEquals(firstFiles.map { it.fileName.toString() }, secondFiles.map { it.fileName.toString() })
        firstFiles.zip(secondFiles).forEach { (a, b) -> assertEquals(Files.readAllBytes(a).toList(), Files.readAllBytes(b).toList()) }
    }
}
