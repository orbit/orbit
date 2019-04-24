/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.visitor

import cloud.orbit.dsl.OrbitDslBaseVisitor
import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.ast.DataDeclaration
import cloud.orbit.dsl.ast.DataField

class DataDeclarationVisitor(
    private val dataFieldTypeVisitor: TypeVisitor
) : OrbitDslBaseVisitor<DataDeclaration>() {
    override fun visitDataDeclaration(ctx: OrbitDslParser.DataDeclarationContext?) =
        DataDeclaration(
            ctx!!.name.text,
            ctx.children
                .filterIsInstance(OrbitDslParser.DataFieldContext::class.java)
                .map {
                    DataField(
                        it.name.text,
                        it.type().accept(dataFieldTypeVisitor),
                        it.index.text.toInt()
                    )
                }
                .toList())
}
