/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.parsing.visitor

import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.ast.AstNode
import cloud.orbit.dsl.ast.TypeReference
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class TypeReferenceVisitorTest {
    @MockK
    lateinit var astNodeContextProvider: AstNodeContextProvider

    private lateinit var visitor: TypeReferenceVisitor

    @BeforeEach
    fun beforeEach() {
        every { astNodeContextProvider.fromToken(any()) } returns AstNode.Context.NONE

        visitor = TypeReferenceVisitor(astNodeContextProvider)
    }

    @Test
    fun buildsSimpleType() {
        Assertions.assertEquals(
            TypeReference(
                "map",
                of = listOf(
                    TypeReference("string"),
                    TypeReference("list", of = listOf(TypeReference("int32")))
                )
            ),
            visitor.parse("map<string, list<int32>>", OrbitDslParser::typeReference)
        )
    }

    @Test
    fun buildsParameterizedType() {
        Assertions.assertEquals(
            TypeReference(
                "map",
                of = listOf(
                    TypeReference("string"),
                    TypeReference("list", of = listOf(TypeReference("int32")))
                )
            ),
            visitor.parse("map<string, list<int32>>", OrbitDslParser::typeReference)
        )
    }
}
