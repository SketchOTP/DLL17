package com.animusmachinae.dll17.research.aliveness

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Research isolation.
 *
 * A000 is disposable research code and the canonical plan permits it exactly one
 * production dependency: the frozen R001 fixed-point numeric library. Everything
 * else — production organism state, R002 continuity, persistence, lifecycle,
 * schemas, the renderer, the Android host — is prohibited.
 *
 * A Gradle dependency declaration is not enough on its own, because `core-math`
 * publishes `core-crypto` as an `api` dependency for lookup-table verification,
 * which puts it on the compile classpath whether the spike wants it or not. The
 * enforceable boundary is therefore the import list of every spike source file,
 * which is what this test reads.
 */
class SpikeIsolationTest {

    private val root = locateResearchRoot()

    private val sources: List<File> = root.walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .toList()

    @Test
    fun `the research track has sources to check`() {
        assertTrue(sources.size >= 15, "expected the full spike source set, found ${sources.size}")
    }

    @Test
    fun `no spike source imports a prohibited production package`() {
        val prohibited = listOf(
            "com.animusmachinae.dll17.core.state",
            "com.animusmachinae.dll17.core.continuity",
            "com.animusmachinae.dll17.core.crypto",
            "com.animusmachinae.dll17.android",
            "android.",
            "androidx.",
        )
        val violations = ArrayList<String>()
        for (file in sources) {
            for (line in file.readLines()) {
                val trimmed = line.trim()
                if (!trimmed.startsWith("import ")) continue
                val imported = trimmed.removePrefix("import ").substringBefore(' ')
                for (banned in prohibited) {
                    if (imported.startsWith(banned)) {
                        violations += "${file.name}: $imported"
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(), "prohibited imports: $violations")
    }

    @Test
    fun `the only production dependency is the R001 fixed-point library`() {
        val productionImports = HashSet<String>()
        for (file in sources) {
            for (line in file.readLines()) {
                val trimmed = line.trim()
                if (!trimmed.startsWith("import com.animusmachinae.dll17.")) continue
                val imported = trimmed.removePrefix("import ").substringBefore(' ')
                if (imported.startsWith("com.animusmachinae.dll17.research.")) continue
                productionImports += imported.substringBeforeLast('.')
            }
        }
        assertEquals(
            setOf("com.animusmachinae.dll17.core.math"),
            productionImports,
            "the spike must consume only the frozen R001 numeric library",
        )
    }

    @Test
    fun `no production module depends on the research track`() {
        val repo = root.parentFile.parentFile
        val productionModules = listOf(
            "core-math", "core-crypto", "core-state", "core-continuity",
            "desktop-runner", "android-host",
        )
        for (module in productionModules) {
            val dir = File(repo, module)
            if (!dir.isDirectory) continue
            val offenders = dir.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                // Android debug sources are the explicit disposable owner
                // research host; release and main remain isolated.
                .filterNot {
                    module == "android-host" &&
                        (it.invariantSeparatorsPath.contains("/src/debug/") ||
                            it.invariantSeparatorsPath.contains("/src/test/"))
                }
                .filter { it.readText().contains("research") && it.readText().contains("aliveness") }
                .map { it.path }
                .toList()
            assertTrue(offenders.isEmpty(), "$module reaches into the research track: $offenders")
        }
        val androidBuild = File(repo, "android-host/build.gradle.kts").readText()
        assertTrue(
            !androidBuild.contains("implementation(project(\":research:aliveness-spike"),
            "the Android production classpath must not depend on the research track",
        )
    }

    @Test
    fun `the viewer cannot see cohort identity`() {
        val viewer = File(root, "realtime-viewer/src/main/kotlin")
        val renderers = viewer.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "ViewerSession.kt" }
            .toList()
        assertTrue(renderers.isNotEmpty(), "expected viewer rendering sources")
        for (file in renderers) {
            val text = file.readText()
            // ViewerMain constructs sessions and therefore names cohorts; the
            // rendering surface must not.
            if (file.name == "SwingViewer.kt") {
                // Only the rendering classes are checked. `ViewerMain` constructs
                // sessions and must name cohorts to do so; what matters is that
                // nothing which draws a frame can see which one it is drawing.
                val rendering = text.substringAfter("public class CreaturePanel")
                    .substringBefore("public object ViewerMain")
                assertTrue(
                    !rendering.contains("Cohort") && !rendering.contains("Mechanism"),
                    "${file.name} rendering code must not reference cohort or mechanism identity",
                )
            }
        }
    }

    @Test
    fun `the presentation contract has no cohort parameter`() {
        val expression = File(root, "cohorts/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/Expression.kt")
        val text = expression.readText()
        assertTrue(
            !text.contains("Cohort"),
            "SpikeExpressionContract must be blind to which controller produced a frame",
        )
    }

    private companion object {
        fun locateResearchRoot(): File {
            var dir = File(System.getProperty("user.dir")).absoluteFile
            while (dir.parentFile != null) {
                val candidate = File(dir, "research/aliveness-spike")
                if (candidate.isDirectory) return candidate
                dir = dir.parentFile
            }
            error("could not locate research/aliveness-spike from ${System.getProperty("user.dir")}")
        }
    }
}
