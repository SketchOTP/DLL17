package com.animusmachinae.dll17.desktop

import com.animusmachinae.dll17.core.persistence.R012PerformanceHarness
import java.io.File
import java.nio.file.Files

/** Reference entry point for the R012 measured-performance harness. */
public object R012PerformanceMain {

    @JvmStatic
    public fun main(args: Array<String>) {
        val root = File(args.firstOrNull() ?: "build/r012-performance")
        root.deleteRecursively()
        root.mkdirs()
        val store = Files.getFileStore(root.toPath())
        if (store.type() == "tmpfs" || store.type() == "ramfs") {
            System.err.println(
                "REFUSED: ${root.absolutePath} is on ${store.type()}. An fsync that never " +
                    "reaches a device measures nothing.",
            )
            kotlin.system.exitProcess(2)
        }
        println(R012PerformanceHarness.run(root))
        root.deleteRecursively()
    }
}
