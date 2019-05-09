/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.visitor

import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.ast.DataDeclaration
import cloud.orbit.dsl.ast.DataField
import cloud.orbit.dsl.ast.Type
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DataDeclarationVisitorTest {
    private val visitor = DataDeclarationVisitor(TypeReferenceVisitor(TestAstNodeContextProvider), TestAstNodeContextProvider)

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
}
