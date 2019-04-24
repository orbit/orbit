/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.visitor

import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.ast.EnumDeclaration
import cloud.orbit.dsl.ast.EnumMember
import cloud.orbit.dsl.ast.ParseContext
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class EnumDeclarationVisitorTest {
    private val visitor = EnumDeclarationVisitor(FakeParseContextProvider)

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

    @Test
    fun annotatesEnumDeclarationWithParseContext() {
        val enumDeclaration = visitor.parse("enum enum1 { }", OrbitDslParser::enumDeclaration)

        Assertions.assertEquals(
            FakeParseContextProvider.fakeParseContext,
            enumDeclaration.getAnnotation<ParseContext>()
        )
    }

    @Test
    fun annotatesEnumMemberWithParseContext() {
        val enumDeclaration = visitor.parse(
            """
            enum enum1 {
                MEMBER = 1;
            }
            """,
            OrbitDslParser::enumDeclaration
        )

        Assertions.assertEquals(
            FakeParseContextProvider.fakeParseContext,
            enumDeclaration.members[0].getAnnotation<ParseContext>()
        )
    }
}
