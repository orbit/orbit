/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.visitor

import cloud.orbit.dsl.OrbitDslBaseVisitor
import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.ast.EnumDeclaration
import cloud.orbit.dsl.ast.EnumMember
import cloud.orbit.dsl.ast.annotated

class EnumDeclarationVisitor(
    private val parseContextProvider: ParseContextProvider
) : OrbitDslBaseVisitor<EnumDeclaration>() {
    override fun visitEnumDeclaration(ctx: OrbitDslParser.EnumDeclarationContext?) =
        EnumDeclaration(
            ctx!!.name.text,
            ctx.children
                .filterIsInstance(OrbitDslParser.EnumMemberContext::class.java)
                .map {
                    EnumMember(it.name.text, it.index.text.toInt())
                        .annotated(parseContextProvider.fromToken(it.name))
                }
                .toList())
            .annotated(parseContextProvider.fromToken(ctx.name))
}
