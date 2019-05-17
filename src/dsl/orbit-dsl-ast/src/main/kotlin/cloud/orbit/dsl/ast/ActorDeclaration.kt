/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.ast

import cloud.orbit.dsl.type.PrimitiveType

data class ActorDeclaration(
    override val name: String,
    val keyType: TypeReference = TypeReference(PrimitiveType.VOID),
    val methods: List<ActorMethod> = emptyList(),
    override val context: AstNode.Context = AstNode.Context.NONE
) : Declaration
