/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.ast

data class MethodParameter(
    val name: String,
    val type: TypeReference,
    override val context: AstNode.Context = AstNode.Context.NONE
) : AstNode
