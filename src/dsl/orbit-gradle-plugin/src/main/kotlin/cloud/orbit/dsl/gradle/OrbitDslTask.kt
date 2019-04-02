/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.gradle

import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GFileUtils
import java.io.File

open class OrbitDslTask : SourceTask() {
    @OutputDirectory
    var outputDirectory: File? = null

    private var sourceDirectorySet: SourceDirectorySet? = null

    @TaskAction
    fun execute() {
        val orbitFiles = mutableSetOf<File>()
        val sourceFiles = source.files

        // Always clear the output directory and re-generate all sources
        GFileUtils.cleanDirectory(outputDirectory!!)
        orbitFiles.addAll(sourceFiles)

        val spec = OrbitDslSpec(project.projectDir, orbitFiles, sourceDirectorySet!!.srcDirs, outputDirectory!!)
        OrbitDslCompilerRunner().run(spec)
    }

    override fun setSource(source: Any) {
        super.setSource(source)

        if (source is SourceDirectorySet) {
            sourceDirectorySet = source
        }
    }
}
