/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.gradle

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.reflect.HasPublicType
import org.gradle.api.reflect.TypeOf
import org.gradle.api.reflect.TypeOf.typeOf
import org.gradle.util.ConfigureUtil

class OrbitDslSourceVirtualDirectoryImpl(
    parentDisplayName: String,
    objectFactory: ObjectFactory
) : OrbitDslSourceVirtualDirectory, HasPublicType {
    override val orbit: SourceDirectorySet =
        objectFactory.sourceDirectorySet("$parentDisplayName.orbit", "$parentDisplayName Orbit source")

    init {
        orbit.filter.include("**/*.orbit")
    }

    override fun orbit(configureClosure: Closure<*>) = this.also {
        ConfigureUtil.configure(configureClosure, orbit)
    }

    override fun orbit(configureAction: Action<in SourceDirectorySet>) = this.also {
        configureAction.execute(orbit)
    }

    override fun getPublicType(): TypeOf<*> = typeOf(OrbitDslSourceVirtualDirectory::class.java)
}
