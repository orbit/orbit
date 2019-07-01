/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.parsing.visitor

import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.ast.AstNode
import cloud.orbit.dsl.ast.EnumDeclaration
import cloud.orbit.dsl.ast.EnumMember
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class EnumDeclarationVisitorTest {
    @MockK
    lateinit var astNodeContextProvider: AstNodeContextProvider

    private lateinit var visitor: EnumDeclarationVisitor

    @BeforeEach
    fun beforeEach() {
        every { astNodeContextProvider.fromToken(any()) } returns AstNode.Context.NONE

        visitor = EnumDeclarationVisitor(astNodeContextProvider)
    }

    @Test
    fun buildsEnumDeclaration() {
        Assertions.assertEquals(
            EnumDeclaration(
                "enum1",
                members = listOf(
                    EnumMember(
                        "A_MEMBER",
                        index = 3
                    ),
                    EnumMember(
                        "ANOTHER_MEMBER",
                        index = 8
                    )
                )
            ),
            visitor.parse(
                """
                enum enum1 {
                    A_MEMBER = 3;
                    ANOTHER_MEMBER = 8;
                }
                """,
                OrbitDslParser::enumDeclaration
            )
        )
    }
}
