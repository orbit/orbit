/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.visitor

import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.ast.DataDeclaration
import cloud.orbit.dsl.ast.DataField
import cloud.orbit.dsl.ast.ParseContext
import cloud.orbit.dsl.ast.Type
import cloud.orbit.dsl.ast.TypeOccurrenceContext
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DataDeclarationVisitorTest {
    private val visitor = DataDeclarationVisitor(
        TypeVisitor(TypeOccurrenceContext.DATA_FIELD, FakeParseContextProvider),
        FakeParseContextProvider)

    @Test
    fun buildsDataDeclaration() {
        Assertions.assertEquals(
            DataDeclaration(
                "data1",
                fields = listOf(
                    DataField(
                        "field1",
                        type = Type("int32"),
                        index = 2
                    ),
                    DataField(
                        "field2",
                        type = Type("string"),
                        index = 5
                    )
                )
            ),
            visitor.parse(
                """
                data data1 {
                    int32 field1 = 2;
                    string field2 = 5;
                }
                """,
                OrbitDslParser::dataDeclaration
            )
        )
    }

    @Test
    fun annotatesDataDeclarationWithParseContext() {
        val dataDeclaration = visitor.parse("data data1 {}", OrbitDslParser::dataDeclaration)

        Assertions.assertEquals(
            FakeParseContextProvider.fakeParseContext,
            dataDeclaration.getAnnotation<ParseContext>()
        )
    }

    @Test
    fun annotatesDataFieldWithParseContext() {
        val dataDeclaration = visitor.parse(
            """
                data data1 {
                    int32 field = 1;
                }
                """,
            OrbitDslParser::dataDeclaration
        )

        Assertions.assertEquals(
            FakeParseContextProvider.fakeParseContext,
            dataDeclaration.fields[0].getAnnotation<ParseContext>()
        )
    }
}
