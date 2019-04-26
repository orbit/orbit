/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.visitor

import cloud.orbit.dsl.OrbitDslBaseVisitor
import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.ast.Type
import cloud.orbit.dsl.ast.TypeOccurrenceContext
import cloud.orbit.dsl.ast.annotated

class TypeVisitor(
    private val typeOccurrenceContext: TypeOccurrenceContext,
    private val typeParameterVisitor: TypeVisitor?,
    private val parseContextProvider: ParseContextProvider
) : OrbitDslBaseVisitor<Type>() {
    constructor(typeOccurrenceContext: TypeOccurrenceContext,
                parseContextProvider: ParseContextProvider) :
        this(typeOccurrenceContext, null, parseContextProvider)

    override fun visitType(ctx: OrbitDslParser.TypeContext?) =
        Type(ctx!!.name.text, ctx.children
            .filterIsInstance(OrbitDslParser.TypeContext::class.java)
            .map { it.accept(typeParameterVisitor ?: this) }
            .toList()
        )
            .annotated(parseContextProvider.fromToken(ctx.name))
            .annotated(typeOccurrenceContext)
}
