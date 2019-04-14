/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.visitor

import cloud.orbit.dsl.OrbitDslBaseVisitor
import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.ast.ActorDeclaration
import cloud.orbit.dsl.ast.DataDeclaration
import cloud.orbit.dsl.ast.EnumDeclaration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DeclarationCollectorVisitorTest {
    private val input =
        """
        enum e1 {}

        actor a1 {}

        data d1 {}

        actor a2 {}

        data d2 {}

        enum e2 {}
        """

    @Test
    fun collectsEnumDeclarations() {
        val visitor = DeclarationCollectorVisitor(object : OrbitDslBaseVisitor<EnumDeclaration>() {
            override fun visitEnumDeclaration(ctx: OrbitDslParser.EnumDeclarationContext) =
                EnumDeclaration(ctx.name.text)
        })

        Assertions.assertEquals(
            listOf(
                EnumDeclaration("e1"),
                EnumDeclaration("e2")
            ),
            visitor.parse(input, OrbitDslParser::file)
        )
    }

    @Test
    fun collectsDataDeclarations() {
        val visitor = DeclarationCollectorVisitor(object : OrbitDslBaseVisitor<DataDeclaration>() {
            override fun visitDataDeclaration(ctx: OrbitDslParser.DataDeclarationContext) =
                DataDeclaration(ctx.name.text)
        })

        Assertions.assertEquals(
            listOf(
                DataDeclaration("d1"),
                DataDeclaration("d2")
            ),
            visitor.parse(input, OrbitDslParser::file)
        )
    }

    @Test
    fun collectsActorDeclarations() {
        val visitor = DeclarationCollectorVisitor(object : OrbitDslBaseVisitor<ActorDeclaration>() {
            override fun visitActorDeclaration(ctx: OrbitDslParser.ActorDeclarationContext) =
                ActorDeclaration(ctx.name.text)
        })

        Assertions.assertEquals(
            listOf(
                ActorDeclaration("a1"),
                ActorDeclaration("a2")
            ),
            visitor.parse(input, OrbitDslParser::file)
        )
    }
}
