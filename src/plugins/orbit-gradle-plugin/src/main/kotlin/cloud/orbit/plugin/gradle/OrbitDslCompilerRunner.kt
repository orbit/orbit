/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.plugin.gradle

import cloud.orbit.dsl.OrbitDslFileParser
import cloud.orbit.dsl.OrbitDslParseInput
import cloud.orbit.dsl.OrbitDslParsingException
import cloud.orbit.dsl.java.OrbitDslJavaCompiler
import java.io.File

class OrbitDslCompilerRunner {
    fun run(spec: OrbitDslSpec) {
        val inputDirectory = spec.orbitFiles.associateWith { file ->
            spec.inputDirectories.first { it.contains(file) }
        }

        try {
            val parsedOrbitFiles = OrbitDslFileParser().parse(
                spec.orbitFiles.map {
                    OrbitDslParseInput(
                        it.readText(),
                        computePackageNameFromFile(inputDirectory.getValue(it), it),
                        it.relativeTo(spec.projectDirectory).path
                    )
                }
            )

            OrbitDslJavaCompiler()
                .compile(parsedOrbitFiles)
                .forEach {
                    it.writeToDirectory(spec.outputDirectory)
                }
        } catch(e: OrbitDslParsingException) {
            error(e.syntaxErrors.joinToString(System.lineSeparator()) { it.errorMessage })
        }
    }

    private fun File.contains(file: File): Boolean {
        var parent = file.parentFile

        while (parent != null) {
            if (this == parent) {
                return true
            }

            parent = parent.parentFile
        }

        return false
    }

    private fun computePackageNameFromFile(inputDirectory: File, f: File): String {
        return f.relativeTo(inputDirectory).parentFile.toPath().joinToString(".")
    }
}
