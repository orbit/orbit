/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.visitor

import cloud.orbit.dsl.OrbitDslBaseVisitor
import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.OrbitDslParser.TypeContext
import cloud.orbit.dsl.ast.ActorDeclaration
import cloud.orbit.dsl.ast.ActorKeyType
import cloud.orbit.dsl.ast.ActorMethod
import cloud.orbit.dsl.ast.MethodParameter
import cloud.orbit.dsl.ast.annotated

class ActorDeclarationVisitor(
    private val methodReturnTypeVisitor: TypeVisitor,
    private val methodParameterTypeVisitor: TypeVisitor,
    private val parseContextProvider: ParseContextProvider
) : OrbitDslBaseVisitor<ActorDeclaration>() {
    override fun visitActorDeclaration(ctx: OrbitDslParser.ActorDeclarationContext?) =
        ActorDeclaration(
            ctx!!.name.text,
            keyType = ctx.keyType?.toActorKeyType() ?: ActorKeyType.NO_KEY,
            methods = ctx.children
                .filterIsInstance(OrbitDslParser.ActorMethodContext::class.java)
                .map { m ->
                    ActorMethod(
                        name = m.name.text,
                        returnType = m.returnType.accept(methodReturnTypeVisitor),
                        params = m.children
                            .filterIsInstance(OrbitDslParser.MethodParamContext::class.java)
                            .map { p ->
                                MethodParameter(p.name.text, p.type().accept(methodParameterTypeVisitor))
                                    .annotated(parseContextProvider.fromToken(p.name))
                            }
                            .toList())
                        .annotated(parseContextProvider.fromToken(m.name))
                }
                .toList())
            .annotated(parseContextProvider.fromToken(ctx.name))

    private fun TypeContext.toActorKeyType() =
        when (this.text) {
            "string" -> ActorKeyType.STRING
            "int32" -> ActorKeyType.INT32
            "int64" -> ActorKeyType.INT64
            "guid" -> ActorKeyType.GUID
            else -> throw UnsupportedActorKeyTypeException(
                this.text, this.name.line, this.name.charPositionInLine
            )
        }
}
