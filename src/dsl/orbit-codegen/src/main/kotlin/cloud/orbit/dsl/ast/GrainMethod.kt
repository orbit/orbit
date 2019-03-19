/*
 Copyright (C) 2015 - 2018 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.ast

data class GrainMethod(
    val name: String,
    val returnType: Type,
    val params: List<MethodParameter> = emptyList()
) : AstNode
