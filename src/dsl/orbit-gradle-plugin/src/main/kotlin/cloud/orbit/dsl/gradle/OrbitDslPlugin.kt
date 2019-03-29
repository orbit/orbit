/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.internal.tasks.DefaultSourceSet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import java.io.File
import javax.inject.Inject

class OrbitDslPlugin : Plugin<Project> {
    private val objectFactory: ObjectFactory

    @Inject
    constructor(objectFactory: ObjectFactory) {
        this.objectFactory = objectFactory
    }

    override fun apply(project: Project) {
        project.pluginManager.apply(JavaPlugin::class.java)

        project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.all { sourceSet ->
            // Add an 'orbit' virtual directory mapping for each source set
            val orbitDirectoryDelegate = OrbitDslSourceVirtualDirectoryImpl(
                (sourceSet as DefaultSourceSet).displayName, objectFactory
            )
            DslObject(sourceSet).convention.plugins["orbit"] = orbitDirectoryDelegate
            orbitDirectoryDelegate.orbit.srcDir("src/${sourceSet.name}/orbit")
            sourceSet.allSource.source(orbitDirectoryDelegate.orbit)

            // Create a code generation task that writes to the output directory
            val taskName = sourceSet.getTaskName("generate", "OrbitSource")
            val outputDirectory = File("${project.buildDir}/generated-src/orbit/${sourceSet.name}")

            // Make the output of Orbit code generation an input to javac
            sourceSet.java.srcDir(outputDirectory)

            // Register the code generation task
            project.tasks.register(taskName, OrbitDslTask::class.java) {
                it.description = "Processes the ${sourceSet.name} Orbit DSL definitions."
                it.source = orbitDirectoryDelegate.orbit
                it.outputDirectory = outputDirectory
            }

            // Run task before compiling Java
            project.tasks.named(sourceSet.compileJavaTaskName) {
                it.dependsOn(taskName)
            }
        }
    }
}
