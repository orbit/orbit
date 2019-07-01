/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.parsing.visitor

import cloud.orbit.dsl.OrbitDslBaseVisitor
import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.OrbitDslVisitor
import cloud.orbit.dsl.ast.ActorDeclaration
import cloud.orbit.dsl.ast.CompilationUnit
import cloud.orbit.dsl.ast.DataDeclaration
import cloud.orbit.dsl.ast.EnumDeclaration

class CompilationUnitVisitor(
    private val packageName: String,
    private val enumDeclarationVisitor: OrbitDslVisitor<EnumDeclaration>,
    private val dataDeclarationVisitor: OrbitDslVisitor<DataDeclaration>,
    private val actorDeclarationVisitor: OrbitDslVisitor<ActorDeclaration>
) : OrbitDslBaseVisitor<CompilationUnit>() {
    override fun defaultResult() = CompilationUnit(packageName)

    override fun aggregateResult(aggregate: CompilationUnit, nextResult: CompilationUnit) =
        CompilationUnit(
            packageName,
            enums = aggregate.enums + nextResult.enums,
            data = aggregate.data + nextResult.data,
            actors = aggregate.actors + nextResult.actors
        )

    override fun visitEnumDeclaration(ctx: OrbitDslParser.EnumDeclarationContext) =
        CompilationUnit(
            packageName,
            enums = listOf(ctx.accept(enumDeclarationVisitor))
        )

    override fun visitDataDeclaration(ctx: OrbitDslParser.DataDeclarationContext)=
        CompilationUnit(
            packageName,
            data = listOf(ctx.accept(dataDeclarationVisitor))
        )

    override fun visitActorDeclaration(ctx: OrbitDslParser.ActorDeclarationContext)=
        CompilationUnit(
            packageName,
            actors = listOf(ctx.accept(actorDeclarationVisitor))
        )
}
