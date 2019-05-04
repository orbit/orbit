/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.visitor

import cloud.orbit.dsl.OrbitDslBaseVisitor
import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.ast.Type

class TypeVisitor(
    private val contextProvider: AstNodeContextProvider
) : OrbitDslBaseVisitor<Type>() {
    override fun visitType(ctx: OrbitDslParser.TypeContext) =
        Type(
            name = ctx.name.text,
            of = ctx.children
                .filterIsInstance(OrbitDslParser.TypeContext::class.java)
                .map { it.accept(this) }
                .toList(),
            context = contextProvider.fromToken(ctx.name)
        )
}
