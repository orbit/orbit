/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.parsing.visitor

import cloud.orbit.dsl.OrbitDslBaseVisitor
import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.ast.EnumDeclaration
import cloud.orbit.dsl.ast.EnumMember

class EnumDeclarationVisitor(
    private val contextProvider: AstNodeContextProvider
) : OrbitDslBaseVisitor<EnumDeclaration>() {
    override fun visitEnumDeclaration(ctx: OrbitDslParser.EnumDeclarationContext) =
        EnumDeclaration(
            name = ctx.name.text,
            members = ctx.children
                .filterIsInstance(OrbitDslParser.EnumMemberContext::class.java)
                .map {
                    EnumMember(
                        name = it.name.text,
                        index = it.index.text.toInt(),
                        context = contextProvider.fromToken(it.name)
                    )
                }
                .toList(),
            context = contextProvider.fromToken(ctx.name))
}
