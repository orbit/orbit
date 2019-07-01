/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.parsing.visitor

import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.ast.AstNode
import cloud.orbit.dsl.ast.DataDeclaration
import cloud.orbit.dsl.ast.DataField
import cloud.orbit.dsl.ast.TypeReference
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class DataDeclarationVisitorTest {
    @MockK
    lateinit var typeReferenceVisitor: TypeReferenceVisitor

    @MockK
    lateinit var astNodeContextProvider: AstNodeContextProvider

    private lateinit var visitor: DataDeclarationVisitor

    @BeforeEach
    fun beforeEach() {
        slot<OrbitDslParser.TypeReferenceContext>().let { slot ->
            every {
                typeReferenceVisitor.visitTypeReference(capture(slot))
            } answers {
                TypeReference(slot.captured.name.text)
            }
        }

        every { astNodeContextProvider.fromToken(any()) } returns AstNode.Context.NONE

        visitor = DataDeclarationVisitor(typeReferenceVisitor, astNodeContextProvider)
    }

    @Test
    fun buildsDataDeclaration() {
        Assertions.assertEquals(
            DataDeclaration(
                "data1",
                fields = listOf(
                    DataField(
                        "field1",
                        type = TypeReference("int32"),
                        index = 2
                    ),
                    DataField(
                        "field2",
                        type = TypeReference("string"),
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
