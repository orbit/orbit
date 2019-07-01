/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.parsing.visitor

import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.OrbitDslVisitor
import cloud.orbit.dsl.ast.ActorDeclaration
import cloud.orbit.dsl.ast.CompilationUnit
import cloud.orbit.dsl.ast.DataDeclaration
import cloud.orbit.dsl.ast.Declaration
import cloud.orbit.dsl.ast.EnumDeclaration
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import org.antlr.v4.runtime.ParserRuleContext
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class CompilationUnitVisitorTest {
    @MockK
    lateinit var enumDeclarationVisitor: OrbitDslVisitor<EnumDeclaration>

    @MockK
    lateinit var dataDeclarationVisitor: OrbitDslVisitor<DataDeclaration>

    @MockK
    lateinit var actorDeclarationVisitor: OrbitDslVisitor<ActorDeclaration>

    lateinit var visitor: CompilationUnitVisitor

    @BeforeEach
    fun beforeEach() {
        configureMockVisitor(enumDeclarationVisitor::visitEnumDeclaration) { EnumDeclaration(it) }
        configureMockVisitor(dataDeclarationVisitor::visitDataDeclaration) { DataDeclaration(it) }
        configureMockVisitor(actorDeclarationVisitor::visitActorDeclaration) { ActorDeclaration(it) }

        visitor = CompilationUnitVisitor(
            "cloud.orbit.test",
            enumDeclarationVisitor,
            dataDeclarationVisitor,
            actorDeclarationVisitor
        )
    }

    @Test
    fun buildsCompilationUnit() {
        val compilationUnit = parse(
            """
            enum e1 {}

            data d1 {}

            enum e2 {}

            actor a1 {}

            data d2 {}

            actor a2 {}

            data d3 {}

            actor a3 {}

            actor a4 {}
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
                    DataDeclaration("d2"),
                    DataDeclaration("d3")
                ),
                actors = listOf(
                    ActorDeclaration("a1"),
                    ActorDeclaration("a2"),
                    ActorDeclaration("a3"),
                    ActorDeclaration("a4")
                )
            ),
            compilationUnit
        )
    }

    private inline fun <reified C : ParserRuleContext, D : Declaration> configureMockVisitor(
        crossinline visit: (C) -> D,
        crossinline declarationBuilder: (String) -> D
    ) {
        val slot = slot<C>()

        every {
            visit(capture(slot))
        } answers {
            declarationBuilder(slot.captured.getChild(1).text)
        }
    }

    private fun parse(input: String) = visitor.parse(input, OrbitDslParser::file)
}
