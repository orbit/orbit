/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.ast

data class TypeReference(
    val name: String,
    val of: List<TypeReference> = emptyList(),
    override val context: AstNode.Context = AstNode.Context.NONE
) : AstNode {
    val isGeneric = of.isNotEmpty()
}
