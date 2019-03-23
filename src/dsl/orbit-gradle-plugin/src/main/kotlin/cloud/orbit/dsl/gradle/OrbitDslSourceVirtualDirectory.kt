/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.gradle

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.file.SourceDirectorySet

interface OrbitDslSourceVirtualDirectory {
    val orbit: SourceDirectorySet

    fun orbit(configureClosure: Closure<*>): OrbitDslSourceVirtualDirectory

    fun orbit(configureAction: Action<in SourceDirectorySet>): OrbitDslSourceVirtualDirectory
}
