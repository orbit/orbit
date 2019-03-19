/*
 Copyright (C) 2015 - 2018 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.ast

data class CompilationUnit(
    val packageName: String,
    val enums: List<EnumDeclaration> = emptyList(),
    val data: List<DataDeclaration> = emptyList(),
    val grains: List<GrainDeclaration> = emptyList()
)
