/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.parsing.visitor

import cloud.orbit.dsl.OrbitDslBaseVisitor
import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.ast.TypeReference

class TypeReferenceVisitor(
    private val contextProvider: AstNodeContextProvider
) : OrbitDslBaseVisitor<TypeReference>() {
    override fun visitTypeReference(ctx: OrbitDslParser.TypeReferenceContext) =
        TypeReference(
            name = ctx.name.text,
            of = ctx.children
                .filterIsInstance(OrbitDslParser.TypeReferenceContext::class.java)
                .map { it.accept(this) }
                .toList(),
            context = contextProvider.fromToken(ctx.name)
        )
}
