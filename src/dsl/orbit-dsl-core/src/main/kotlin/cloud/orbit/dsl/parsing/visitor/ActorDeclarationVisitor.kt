/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.parsing.visitor

import cloud.orbit.dsl.OrbitDslBaseVisitor
import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.ast.ActorDeclaration
import cloud.orbit.dsl.ast.ActorMethod
import cloud.orbit.dsl.ast.MethodParameter
import cloud.orbit.dsl.ast.TypeReference
import cloud.orbit.dsl.type.PrimitiveType

class ActorDeclarationVisitor(
    private val typeReferenceVisitor: TypeReferenceVisitor,
    private val contextProvider: AstNodeContextProvider
) : OrbitDslBaseVisitor<ActorDeclaration>() {
    override fun visitActorDeclaration(ctx: OrbitDslParser.ActorDeclarationContext) =
        ActorDeclaration(
            name = ctx.name.text,
            keyType = ctx.keyType?.accept(typeReferenceVisitor) ?: TypeReference(PrimitiveType.VOID),
            methods = ctx.children
                .filterIsInstance(OrbitDslParser.ActorMethodContext::class.java)
                .map { m ->
                    ActorMethod(
                        name = m.name.text,
                        returnType = m.returnType.accept(typeReferenceVisitor),
                        params = m.children
                            .filterIsInstance(OrbitDslParser.MethodParamContext::class.java)
                            .map { p ->
                                MethodParameter(
                                    name = p.name.text,
                                    type = p.typeReference().accept(typeReferenceVisitor),
                                    context = contextProvider.fromToken(p.name)
                                )
                            }
                            .toList(),
                        context = contextProvider.fromToken(m.name))
                }
                .toList(),
            context = contextProvider.fromToken(ctx.name))
}
