/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.visitor

import cloud.orbit.dsl.OrbitDslBaseVisitor
import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.ast.ActorDeclaration
import cloud.orbit.dsl.ast.CompilationUnit
import cloud.orbit.dsl.ast.DataDeclaration
import cloud.orbit.dsl.ast.EnumDeclaration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class CompilationUnitBuilderVisitorTest {
    private val enumDeclarationVisitor = object : OrbitDslBaseVisitor<EnumDeclaration>() {
        override fun visitEnumDeclaration(ctx: OrbitDslParser.EnumDeclarationContext) =
            EnumDeclaration(ctx.name.text)
    }

    private val dataDeclarationVisitor = object : OrbitDslBaseVisitor<DataDeclaration>() {
        override fun visitDataDeclaration(ctx: OrbitDslParser.DataDeclarationContext) =
            DataDeclaration(ctx.name.text)
    }

    private val actorDeclarationVisitor = object : OrbitDslBaseVisitor<ActorDeclaration>() {
        override fun visitActorDeclaration(ctx: OrbitDslParser.ActorDeclarationContext) =
            ActorDeclaration(ctx.name.text)
    }

    private val visitor = CompilationUnitBuilderVisitor(
        "cloud.orbit.test",
        enumDeclarationVisitor,
        dataDeclarationVisitor,
        actorDeclarationVisitor
    )

    @Test
    fun buildsCompilationUnit() {
        val compilationUnit = parse(
            """
            enum e1 {
            }

            enum e2 {
            }

            data d1 {
            }

            actor a1 {
            }

            data d2 {
            }

            actor a2 {
            }
            """
        )

        Assertions.assertEquals(
            CompilationUnit(
                "cloud.orbit.test",
                enums = listOf(
                    EnumDeclaration("e1"),
                    EnumDeclaration("e2")
                ),
                data = listOf(
                    DataDeclaration("d1"),
                    DataDeclaration("d2")
                ),
                actors = listOf(
                    ActorDeclaration("a1"),
                    ActorDeclaration("a2")
                )
            ),
            compilationUnit
        )
    }

    private fun parse(input: String) = visitor.parse(input, OrbitDslParser::file)
}
