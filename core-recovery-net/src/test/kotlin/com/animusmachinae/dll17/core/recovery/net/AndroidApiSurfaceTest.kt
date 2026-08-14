package com.animusmachinae.dll17.core.recovery.net

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The module's own class files, read back, to check what it depends on.
 *
 * ### Why this test exists
 *
 * The whole justification for building an S3 adapter instead of adopting one is
 * that the adopted one could not run where this has to: the destination device
 * during a cold recovery. A justification that is never checked decays — someone
 * reaches for `java.net.http.HttpClient` because it is nicer, the module still
 * builds and tests green on the desktop, and the reason the dependency was
 * refused quietly stops being true.
 *
 * So the claim is enforced instead of documented. Every `java.*` and `javax.*`
 * package this module references is compared against packages present on Android
 * API 29, and anything else fails here rather than on a user's phone.
 *
 * ### What this test does *not* prove
 *
 * That the module runs on Android. That needs an instrumented run on hardware,
 * which is `BLOCKED_DEVICE_UNAVAILABLE` under D012 and is not claimed anywhere in
 * D014. This proves the narrower, still useful thing: nothing here references an
 * API Android does not have.
 */
class AndroidApiSurfaceTest {

    private companion object {
        /**
         * JDK packages present on Android API 29.
         *
         * Deliberately a short list. Adding to it is a decision about what runs
         * on the device, and it should be as visible as one.
         */
        val PERMITTED_PACKAGES = setOf(
            "java.lang",
            "java.lang.invoke",
            "java.io",
            "java.net",
            "java.nio",
            "java.nio.charset",
            "java.security",
            "java.text",
            // `java.time` arrived on Android at API 26 and minSdk is 29, so it
            // is available without desugaring.
            "java.time",
            "java.time.format",
            "java.time.temporal",
            "java.util",
            "java.util.concurrent",
            "java.util.function",
            "javax.net.ssl",
        )

        /**
         * Packages that must never appear, with the reason.
         *
         * `java.net.http` is the one that matters: it does not exist on Android
         * at any API level, and it is the natural thing to reach for.
         */
        val FORBIDDEN_PACKAGES = mapOf(
            "java.net.http" to "does not exist on Android at any API level",
            "com.sun" to "internal JDK API, absent from Android",
            "sun" to "internal JDK API, absent from Android",
            "javax.xml" to "entity resolution is a request-forgery primitive on untrusted documents",
            "java.awt" to "absent from Android",
        )
    }

    @Test
    fun `the module references no API missing from Android API 29`() {
        val classesRoot = classesDirectory()
        val classFiles = classesRoot.walkTopDown().filter { it.isFile && it.extension == "class" }.toList()
        assertTrue(classFiles.isNotEmpty(), "found no compiled classes under $classesRoot")

        val offences = ArrayList<String>()
        for (classFile in classFiles) {
            for (reference in referencedTypes(classFile.readBytes())) {
                val binary = reference.replace('/', '.')
                FORBIDDEN_PACKAGES.entries
                    .firstOrNull { binary == it.key || binary.startsWith(it.key + ".") }
                    ?.let { offences += "${classFile.name}: $binary — ${it.value}" }
                if (binary.startsWith("java.") || binary.startsWith("javax.")) {
                    val packageName = binary.substringBeforeLast('.')
                    if (packageName !in PERMITTED_PACKAGES) {
                        offences += "${classFile.name}: $binary is not in the Android API 29 allowlist"
                    }
                }
            }
        }
        if (offences.isNotEmpty()) {
            fail("the recovery network module reached outside the Android API surface:\n" + offences.joinToString("\n"))
        }
    }

    private fun classesDirectory(): File {
        val marker = S3RecoveryPackageStore::class.java
        val source = File(marker.protectionDomain.codeSource.location.toURI())
        return if (source.isDirectory) source else source.parentFile
    }

    /**
     * Class-name references from a class file's constant pool.
     *
     * Reads `CONSTANT_Class` entries, which is enough: every type a class file
     * names in a field, a method signature, an instantiation or a call has a
     * `CONSTANT_Class` or appears inside a descriptor, and both are covered by
     * also scanning `CONSTANT_Utf8` entries for descriptor syntax.
     */
    private fun referencedTypes(bytes: ByteArray): Set<String> {
        val types = LinkedHashSet<String>()
        var offset = 8 // magic (4) + minor (2) + major (2)
        val constantCount = readU16(bytes, offset)
        offset += 2
        val utf8 = HashMap<Int, String>()
        val classNameIndex = ArrayList<Int>()
        var index = 1
        while (index < constantCount) {
            when (val tag = bytes[offset].toInt() and 0xFF) {
                1 -> { // CONSTANT_Utf8
                    val length = readU16(bytes, offset + 1)
                    utf8[index] = String(bytes, offset + 3, length, Charsets.UTF_8)
                    offset += 3 + length
                }
                7 -> { // CONSTANT_Class
                    classNameIndex += readU16(bytes, offset + 1)
                    offset += 3
                }
                8, 16, 19, 20 -> offset += 3
                15 -> offset += 4
                3, 4, 9, 10, 11, 12, 17, 18 -> offset += 5
                5, 6 -> { // long and double take two constant-pool slots
                    offset += 9
                    index += 1
                }
                else -> throw IllegalStateException("unknown constant pool tag $tag at $offset")
            }
            index += 1
        }
        classNameIndex.mapNotNullTo(types) { utf8[it]?.takeIf { name -> !name.startsWith("[") } }
        // Descriptors name types the constant pool does not otherwise list.
        for (value in utf8.values) {
            if (!value.contains('L') || !value.contains(';')) continue
            var cursor = 0
            while (true) {
                val start = value.indexOf('L', cursor)
                if (start < 0) break
                val end = value.indexOf(';', start)
                if (end < 0) break
                val candidate = value.substring(start + 1, end)
                if (candidate.isNotEmpty() && candidate.all { it.isLetterOrDigit() || it == '/' || it == '_' || it == '$' }) {
                    types += candidate
                }
                cursor = end + 1
            }
        }
        return types
    }

    private fun readU16(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
}
