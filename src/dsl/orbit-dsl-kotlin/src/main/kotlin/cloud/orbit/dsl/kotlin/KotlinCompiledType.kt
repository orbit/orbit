/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.kotlin

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File

data class KotlinCompiledType(
    val packageName: String,
    private val spec: TypeSpec
) {
    fun writeToDirectory(directory: File) {
        FileSpec
            .get(packageName, spec)
            .writeTo(directory)
    }

    override fun toString() = spec.toString()
}
