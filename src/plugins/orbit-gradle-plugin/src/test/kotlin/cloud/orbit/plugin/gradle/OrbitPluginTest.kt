/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.plugin.gradle

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OrbitPluginTest {
    private val project = ProjectBuilder.builder().build()

    @BeforeEach
    fun setup() {
        project.pluginManager.apply("cloud.orbit.plugin")
    }

    @Test
    fun applyingPluginWorks() {
        assertTrue(project.pluginManager.hasPlugin("cloud.orbit.plugin"))
    }

    @Test
    fun appliesJavaPlugin() {
        assertTrue(project.pluginManager.hasPlugin("java"))
    }

    @Test
    fun registersSourceGenerationTask() {
        assertNotNull(project.tasks.getByName("generateOrbitSource"))
    }

    @Test
    fun addsDependencyToJavaCompilationTask() {
        project.tasks.getByName("compileJava").dependsOn.contains("generateOrbitSource")
    }
}
