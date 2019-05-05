/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.ast

data class ActorDeclaration(
    override val name: String,
    val keyType: ActorKeyType = ActorKeyType.NO_KEY,
    val methods: List<ActorMethod> = emptyList(),
    override val context: AstNode.Context = AstNode.Context.NONE
) : Declaration
