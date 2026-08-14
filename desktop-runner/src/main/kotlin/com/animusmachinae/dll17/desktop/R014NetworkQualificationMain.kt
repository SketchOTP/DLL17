package com.animusmachinae.dll17.desktop

import java.io.File
import java.nio.file.Files

/**
 * Reference entry point for the R014 network qualification kernel.
 *
 * With no environment set it runs against the in-repository S3-compatible
 * endpoint, which is what CI does. Set `DLL17_RECOVERY_S3_ENDPOINT`,
 * `DLL17_RECOVERY_S3_BUCKET` and the credential variables and it runs the same
 * fixtures against that endpoint instead.
 *
 * The digest is checked only for the in-repository run. An external endpoint is
 * a third party: it may be slower, may page differently, and its result is
 * recorded as its own evidence rather than compared byte for byte against a
 * golden value this project controls.
 */
public object R014NetworkQualificationMain {

    @JvmStatic
    public fun main(args: Array<String>) {
        val root = File(args.firstOrNull() ?: "build/r014-network-qualification")
        root.deleteRecursively()
        root.mkdirs()
        val store = Files.getFileStore(root.toPath())
        if (store.type() == "tmpfs" || store.type() == "ramfs") {
            System.err.println(
                "REFUSED: ${root.absolutePath} is on ${store.type()}, where fsync does not reach " +
                    "a device. Durability fixtures would pass without proving anything.",
            )
            kotlin.system.exitProcess(2)
        }

        val external = R014NetworkQualificationKernel.externalEndpointConfig()
        println("R014_QUALIFICATION_ROOT=${root.absolutePath}")
        println("R014_FILESYSTEM=${store.type()} device=${store.name()}")
        if (external != null) {
            println("R014_EXTERNAL_ENDPOINT=${external.endpoint} bucket=${external.bucket} region=${external.region}")
            if (external.plaintextTransport) {
                println("R014_TRANSPORT_WARNING=endpoint is plaintext http; acceptable only for a local endpoint")
            }
        } else {
            println("R014_EXTERNAL_ENDPOINT=none (using the in-repository qualification endpoint)")
        }
        println()

        val report = R014NetworkQualificationKernel.run(root, external)
        println(report.render())
        root.deleteRecursively()

        if (report.heldCount != report.findings.size) {
            System.err.println("R014 FAILED: ${report.findings.size - report.heldCount} fixture(s) did not hold")
            kotlin.system.exitProcess(1)
        }
    }
}
