/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.plugin.gradle

import cloud.orbit.dsl.OrbitDslFileParser
import cloud.orbit.dsl.ast.CompilationUnit
import cloud.orbit.dsl.java.OrbitDslJavaCompiler
import org.antlr.v4.runtime.misc.ParseCancellationException
import java.io.File

class OrbitDslCompilerRunner {
    fun run(spec: OrbitDslSpec) {
        val inputDirectory = spec.orbitFiles.map { file ->
            file to spec.inputDirectories.first { it.contains(file) }
        }.toMap()

        val errors = mutableListOf<String>()
        val parsedOrbitFiles = mutableListOf<CompilationUnit>()

        spec.orbitFiles.forEach { file ->
            try {
                parsedOrbitFiles.add(
                    OrbitDslFileParser().parse(
                        file.readText(), getPackageNameFromFile(inputDirectory.getValue(file), file)
                    )
                )
            } catch (e: ParseCancellationException) {
                errors.add("${file.relativeTo(spec.projectDirectory)}: error: ${e.message}")
            }
        }

        if (errors.isNotEmpty()) {
            throw OrbitDslException(errors.joinToString(System.lineSeparator()))
        }

        OrbitDslJavaCompiler()
            .compile(parsedOrbitFiles)
            .forEach {
                it.writeToDirectory(spec.outputDirectory)
            }
    }

    private fun getPackageNameFromFile(inputDirectory: File, f: File): String {
        tailrec fun getPackageNameFromFile(file: File?, acc: MutableList<String>): List<String> {
            if (file == null) {
                return acc
            }

            acc.add(0, file.name)
            return getPackageNameFromFile(file.parentFile, acc)
        }

        return getPackageNameFromFile(
            f.relativeTo(inputDirectory).parentFile,
            mutableListOf()
        )
            .joinToString(separator = ".")
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
}
