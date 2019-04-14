/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.visitor

import cloud.orbit.dsl.OrbitDslBaseVisitor
import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.ast.Type
import cloud.orbit.dsl.ast.annotated

class TypeVisitor : OrbitDslBaseVisitor<Type>() {
    override fun visitType(ctx: OrbitDslParser.TypeContext?) =
        Type(ctx!!.name.text, ctx.children
            .filterIsInstance(OrbitDslParser.TypeContext::class.java)
            .map { it.accept(TypeVisitor()) }
            .toList()
        )
}
