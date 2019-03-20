/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.gradle

import cloud.orbit.dsl.OrbitFileParser
import cloud.orbit.dsl.java.OrbitJavaCompiler
import java.io.File

class OrbitDslCompilerRunner {
    fun run(spec: OrbitDslSpec) {
        val inputDirectory = spec.orbitFiles.map { file ->
            file to spec.inputDirectories.first { it.contains(file) }
        }.toMap()

        val parsedOrbitFiles = spec.orbitFiles.map {
            OrbitFileParser().parse(it.readText(), getPackageNameFromFile(inputDirectory.getValue(it), it))
        }

        OrbitJavaCompiler()
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
