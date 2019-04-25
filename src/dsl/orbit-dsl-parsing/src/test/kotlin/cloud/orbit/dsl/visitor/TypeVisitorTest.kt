/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.visitor

import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.ast.ParseContext
import cloud.orbit.dsl.ast.Type
import cloud.orbit.dsl.ast.TypeOccurrenceContext
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TypeVisitorTest {
    private val typeOccurrenceContext = TypeOccurrenceContext.values().random()
    private val visitor = TypeVisitor(
        typeOccurrenceContext,
        TypeVisitor(TypeOccurrenceContext.TYPE_PARAMETER, FakeParseContextProvider),
        FakeParseContextProvider
    )

    @Test
    fun buildsSimpleType() {
        Assertions.assertEquals(
            Type(
                "map",
                of = listOf(
                    Type("string"),
                    Type("list", of = listOf(Type("int32")))
                )
            ),
            visitor.parse("map<string, list<int32>>", OrbitDslParser::type)
        )
    }

    @Test
    fun buildsParameterizedType() {
        Assertions.assertEquals(
            Type(
                "map",
                of = listOf(
                    Type("string"),
                    Type("list", of = listOf(Type("int32")))
                )
            ),
            visitor.parse("map<string, list<int32>>", OrbitDslParser::type)
        )
    }

    @Test
    fun annotatesTypeWithParseContext() {
        Assertions.assertEquals(
            FakeParseContextProvider.fakeParseContext,
            visitor.parse("int32", OrbitDslParser::type).getAnnotation<ParseContext>()
        )
    }

    @Test
    fun annotatesTypeWithTypeOccurrenceContext() {
        Assertions.assertEquals(
            typeOccurrenceContext,
            visitor.parse("int32", OrbitDslParser::type).getAnnotation<TypeOccurrenceContext>()
        )
    }

    @Test
    fun annotatesTypeParameterWithTypeOccurrenceContext() {
        Assertions.assertEquals(
            TypeOccurrenceContext.TYPE_PARAMETER,
            visitor.parse("list<int32>", OrbitDslParser::type).of[0].getAnnotation<TypeOccurrenceContext>()
        )
    }
}
