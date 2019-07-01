/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.plugin.gradle

import cloud.orbit.dsl.error.OrbitDslCompilationException
import cloud.orbit.dsl.java.OrbitDslJavaCompiler
import cloud.orbit.dsl.kotlin.OrbitDslKotlinCompiler
import cloud.orbit.dsl.parsing.OrbitDslFileParser
import cloud.orbit.dsl.parsing.OrbitDslParseInput
import cloud.orbit.dsl.type.check.OrbitDslTypeChecker
import java.io.File

class OrbitDslCompilerRunner {
    fun run(spec: OrbitDslSpec) {
        val inputDirectory = spec.orbitFiles.associateWith { file ->
            spec.inputDirectories.first { it.contains(file) }
        }

        try {
            val compilationUnits = OrbitDslFileParser().parse(
                spec.orbitFiles.map {
                    OrbitDslParseInput(
                        it.readText(),
                        computePackageNameFromFile(inputDirectory.getValue(it), it),
                        it.relativeTo(spec.projectDirectory).path
                    )
                }
            )

            OrbitDslTypeChecker.checkTypes(compilationUnits)

            OrbitDslJavaCompiler()
                .compile(compilationUnits)
                .forEach {
                    it.writeToDirectory(spec.outputDirectory)
                }

            OrbitDslKotlinCompiler()
                .compile(compilationUnits.map {
                    it.copy(packageName = it.packageName + ".kotlin")
                })
                .forEach {
                    it.writeToDirectory(spec.outputDirectory)
                }
        } catch (e: OrbitDslCompilationException) {
            error(e.errors.joinToString(System.lineSeparator()) {
                "error: ${it.filePath}:${it.line}:${it.column}: ${it.message}"
            })
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
