/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.visitor

import cloud.orbit.dsl.OrbitDslBaseVisitor
import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.ast.Type

class TypeReferenceVisitor(
    private val contextProvider: AstNodeContextProvider
) : OrbitDslBaseVisitor<Type>() {
    override fun visitTypeReference(ctx: OrbitDslParser.TypeReferenceContext) =
        Type(
            name = ctx.name.text,
            of = ctx.children
                .filterIsInstance(OrbitDslParser.TypeReferenceContext::class.java)
                .map { it.accept(this) }
                .toList(),
            context = contextProvider.fromToken(ctx.name)
        )
}
