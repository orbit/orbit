/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.java

import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import java.io.File

data class CompiledType(
    val packageName: String,
    private val spec: TypeSpec
) {
    fun writeToDirectory(directory: File) {
        JavaFile.builder(packageName, spec)
            .build()
            .writeTo(directory)
    }

    override fun toString(): String {
        return spec.toString()
    }
}
