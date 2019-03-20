/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.java

import cloud.orbit.dsl.ast.CompilationUnit

class OrbitJavaCompiler {
    fun compile(compilationUnits: List<CompilationUnit>) =
        JavaCodeGenerator(knownTypes = TypeIndexer().visitCompilationUnits(compilationUnits))
            .visitCompilationUnits(compilationUnits)
}
