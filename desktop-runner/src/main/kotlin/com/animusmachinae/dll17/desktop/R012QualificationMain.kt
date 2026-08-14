package com.animusmachinae.dll17.desktop

import com.animusmachinae.dll17.core.persistence.R012QualificationKernel
import com.animusmachinae.dll17.services.identity.IdentityAuthorityService
import java.io.File
import java.nio.file.Files

/**
 * Reference entry point for the R012 qualification kernel.
 *
 * It lives here, in the runner, for a structural reason: the kernel needs an
 * identity authority, `core-persistence` deliberately does not depend on the
 * authority service, and this is the one module already allowed to see
 * everything. Wiring the two together is exactly what a host does.
 */
public object R012QualificationMain {

    @JvmStatic
    public fun main(args: Array<String>) {
        // Never the system temp directory. On this machine it is tmpfs, where
        // `fsync` never reaches a device and every durability fixture would pass
        // for the wrong reason.
        val root = File(args.firstOrNull() ?: "build/r012-qualification")
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
        println("R012_QUALIFICATION_ROOT=${root.absolutePath}")
        println("R012_FILESYSTEM=${store.type()} device=${store.name()}")
        println()

        val report = R012QualificationKernel.run(root) { directory ->
            IdentityAuthorityService(directory)
        }
        println(report.render())
        root.deleteRecursively()
    }
}
