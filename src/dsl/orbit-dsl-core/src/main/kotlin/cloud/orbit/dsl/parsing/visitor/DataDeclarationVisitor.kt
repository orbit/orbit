/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.parsing.visitor

import cloud.orbit.dsl.OrbitDslBaseVisitor
import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.ast.DataDeclaration
import cloud.orbit.dsl.ast.DataField

class DataDeclarationVisitor(
    private val typeReferenceVisitor: TypeReferenceVisitor,
    private val contextProvider: AstNodeContextProvider
) : OrbitDslBaseVisitor<DataDeclaration>() {
    override fun visitDataDeclaration(ctx: OrbitDslParser.DataDeclarationContext) =
        DataDeclaration(
            name = ctx.name.text,
            fields = ctx.children
                .filterIsInstance(OrbitDslParser.DataFieldContext::class.java)
                .map {
                    DataField(
                        name = it.name.text,
                        type = it.typeReference().accept(typeReferenceVisitor),
                        index = it.index.text.toInt(),
                        context = contextProvider.fromToken(it.name)
                    )
                }
                .toList(),
            context = contextProvider.fromToken(ctx.name))
}
