/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.parsing.visitor

import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.ast.ActorDeclaration
import cloud.orbit.dsl.ast.ActorMethod
import cloud.orbit.dsl.ast.AstNode
import cloud.orbit.dsl.ast.MethodParameter
import cloud.orbit.dsl.ast.TypeReference
import cloud.orbit.dsl.type.PrimitiveType
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class ActorDeclarationVisitorTest {
    @MockK
    lateinit var typeReferenceVisitor: TypeReferenceVisitor

    @MockK
    lateinit var astNodeContextProvider: AstNodeContextProvider

    private lateinit var visitor: ActorDeclarationVisitor

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

        visitor = ActorDeclarationVisitor(typeReferenceVisitor, astNodeContextProvider)
    }

    @Test
    fun buildsKeylessActorDeclaration() {
        Assertions.assertEquals(
            ActorDeclaration("actor1", keyType = TypeReference(PrimitiveType.VOID)),
            visitor.parse("actor actor1 { }", OrbitDslParser::actorDeclaration)
        )
    }

    @Test
    fun buildsKeyedActorDeclaration() {
        Assertions.assertEquals(
            ActorDeclaration("actor1", keyType = TypeReference("anything")),
            visitor.parse("actor actor1<anything> {}", OrbitDslParser::actorDeclaration)
        )
    }

    @Test
    fun buildsActorDeclarationWithMethods() {
        Assertions.assertEquals(
            ActorDeclaration(
                "actor1",
                keyType = TypeReference(PrimitiveType.VOID),
                methods = listOf(
                    ActorMethod(
                        "method1",
                        returnType = TypeReference("void"),
                        params = listOf(
                            MethodParameter(
                                "n",
                                type = TypeReference("int32")
                            )
                        )
                    ),
                    ActorMethod(
                        "method2",
                        returnType = TypeReference("int64"),
                        params = listOf(
                            MethodParameter(
                                "s",
                                type = TypeReference("string")
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
