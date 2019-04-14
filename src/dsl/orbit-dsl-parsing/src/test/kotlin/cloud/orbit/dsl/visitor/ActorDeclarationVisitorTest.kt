/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.visitor

import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.ast.ActorDeclaration
import cloud.orbit.dsl.ast.ActorKeyType
import cloud.orbit.dsl.ast.ActorMethod
import cloud.orbit.dsl.ast.MethodParameter
import cloud.orbit.dsl.ast.Type
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ActorDeclarationVisitorTest {
    private val visitor = ActorDeclarationVisitor(TypeVisitor())

    @Test
    fun buildsKeylessActorDeclaration() {
        Assertions.assertEquals(
            ActorDeclaration("actor1", keyType = ActorKeyType.NO_KEY),
            visitor.parse("actor actor1 { }", OrbitDslParser::actorDeclaration)
        )
    }

    @Test
    fun buildsStringKeyedActorDeclaration() {
        Assertions.assertEquals(
            ActorDeclaration("actor1", keyType = ActorKeyType.STRING),
            visitor.parse("actor actor1<string> {}", OrbitDslParser::actorDeclaration)
        )
    }

    @Test
    fun buildsInt32KeyedActorDeclaration() {
        Assertions.assertEquals(
            ActorDeclaration("actor1", keyType = ActorKeyType.INT32),
            visitor.parse("actor actor1<int32> {}", OrbitDslParser::actorDeclaration)
        )
    }

    @Test
    fun buildsInt64KeyedActorDeclaration() {
        Assertions.assertEquals(
            ActorDeclaration("actor1", keyType = ActorKeyType.INT64),
            visitor.parse("actor actor1<int64> {}", OrbitDslParser::actorDeclaration)
        )
    }

    @Test
    fun buildsFuidKeyedActorDeclaration() {
        Assertions.assertEquals(
            ActorDeclaration("actor1", keyType = ActorKeyType.GUID),
            visitor.parse("actor actor1<guid> {}", OrbitDslParser::actorDeclaration)
        )
    }

    @Test
    fun throwsOnInvalidKeyType() {
        assertThrows<UnsupportedActorKeyTypeException>() {
            visitor.parse("actor actor1<float> {}", OrbitDslParser::actorDeclaration)
        }
    }


    @Test
    fun buildsActorDeclaration() {
        Assertions.assertEquals(
            ActorDeclaration(
                "actor1",
                keyType = ActorKeyType.NO_KEY,
                methods = listOf(
                    ActorMethod(
                        "method1",
                        returnType = Type("void"),
                        params = listOf(
                            MethodParameter(
                                "n",
                                type = Type("int32")
                            )
                        )
                    ),
                    ActorMethod(
                        "method2",
                        returnType = Type("int64"),
                        params = listOf(
                            MethodParameter(
                                "s",
                                type = Type("string")
                            )
                        )
                    )
                )
            ),
            visitor.parse(
                """
                    actor actor1 {
                        void method1(int32 n);
                        int64 method2(string s);
                    }
                """,
                OrbitDslParser::actorDeclaration
            )
        )
    }
}
