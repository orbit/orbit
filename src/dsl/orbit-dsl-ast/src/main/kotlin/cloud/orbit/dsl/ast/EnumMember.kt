/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.ast

data class EnumMember(
    val name: String,
    val index: Int,
    override val context: AstNode.Context = AstNode.Context.NONE
) : AstNode
