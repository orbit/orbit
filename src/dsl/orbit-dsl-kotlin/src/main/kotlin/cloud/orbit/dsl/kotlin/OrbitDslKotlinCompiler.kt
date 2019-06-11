/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.kotlin

import cloud.orbit.dsl.ast.CompilationUnit

class OrbitDslKotlinCompiler {
    fun compile(compilationUnits: List<CompilationUnit>) =
        KotlinCodeGenerator(knownTypes = KotlinTypeIndexer().visitCompilationUnits(compilationUnits))
            .visitCompilationUnits(compilationUnits)
}
