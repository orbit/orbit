/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.ast

data class ActorMethod(
    val name: String,
    val returnType: Type,
    val params: List<MethodParameter> = emptyList(),
    override val parseContext: ParseContext = ParseContext.NONE
) : AstNode
