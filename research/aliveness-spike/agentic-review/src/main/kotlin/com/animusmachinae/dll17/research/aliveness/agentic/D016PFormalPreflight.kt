package com.animusmachinae.dll17.research.aliveness.agentic

import java.nio.file.Paths

/** Offline P0 check for the versioned D016-P input namespace. */
public object D016PFormalPreflight {
    @JvmStatic
    public fun main(args: Array<String>) {
        val root = argument(args, "root")?.let(Paths::get)?.toAbsolutePath()?.normalize() ?: Paths.get(".").toAbsolutePath().normalize()
        val inputRoot = argument(args, "input-root")?.let(Paths::get)?.toAbsolutePath()?.normalize()
            ?: error("--input-root is required")
        val outputRoot = root.resolve("research/aliveness-spike/evidence/a001-v2/d016-p")
        val runner = D016PExecutionRunner(root, inputRoot, outputRoot) { _, _ ->
            error("D016-P preflight must not execute a model")
        }
        print(runner.preflight())
    }

    private fun argument(args: Array<String>, name: String): String? =
        args.firstOrNull { it.startsWith("--$name=") }?.removePrefix("--$name=")
}
