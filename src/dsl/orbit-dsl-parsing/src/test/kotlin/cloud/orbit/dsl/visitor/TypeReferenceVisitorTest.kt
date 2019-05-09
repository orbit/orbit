/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.visitor

import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.ast.Type
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TypeReferenceVisitorTest {
    private val visitor = TypeReferenceVisitor(TestAstNodeContextProvider)

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
            visitor.parse("map<string, list<int32>>", OrbitDslParser::typeReference)
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
            visitor.parse("map<string, list<int32>>", OrbitDslParser::typeReference)
        )
    }
}
