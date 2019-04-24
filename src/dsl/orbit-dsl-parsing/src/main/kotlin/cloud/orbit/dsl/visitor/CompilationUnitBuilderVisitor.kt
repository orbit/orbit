/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.visitor

import cloud.orbit.dsl.OrbitDslBaseVisitor
import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.OrbitDslVisitor
import cloud.orbit.dsl.ast.ActorDeclaration
import cloud.orbit.dsl.ast.CompilationUnit
import cloud.orbit.dsl.ast.DataDeclaration
import cloud.orbit.dsl.ast.EnumDeclaration

class CompilationUnitBuilderVisitor(
    private val packageName: String,
    private val enumDeclarationVisitor: OrbitDslVisitor<EnumDeclaration>,
    private val dataDeclarationVisitor: OrbitDslVisitor<DataDeclaration>,
    private val actorDeclarationVisitor: OrbitDslVisitor<ActorDeclaration>
) : OrbitDslBaseVisitor<CompilationUnit>() {

    override fun visitFile(ctx: OrbitDslParser.FileContext) =
        CompilationUnit(
            packageName,
            enums = ctx.accept(DeclarationCollectorVisitor(enumDeclarationVisitor)).toList(),
            data = ctx.accept(DeclarationCollectorVisitor(dataDeclarationVisitor)).toList(),
            actors = ctx.accept(DeclarationCollectorVisitor(actorDeclarationVisitor)).toList()
        )
}
