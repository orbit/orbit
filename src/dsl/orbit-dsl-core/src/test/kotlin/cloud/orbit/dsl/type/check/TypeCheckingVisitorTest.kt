/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.type.check

import cloud.orbit.dsl.ast.ActorDeclaration
import cloud.orbit.dsl.ast.ActorMethod
import cloud.orbit.dsl.ast.CompilationUnit
import cloud.orbit.dsl.ast.DataDeclaration
import cloud.orbit.dsl.ast.DataField
import cloud.orbit.dsl.ast.MethodParameter
import cloud.orbit.dsl.ast.TypeReference
import io.mockk.confirmVerified
import io.mockk.excludeRecords
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import io.mockk.verifyAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class TypeCheckingVisitorTest {
    @MockK(relaxUnitFun = true)
    lateinit var typeCheck: TypeCheck

    lateinit var typeCheckingVisitor: TypeCheckingVisitor

    @BeforeEach
    fun beforeEach() {
        typeCheckingVisitor = TypeCheckingVisitor(listOf(typeCheck))
    }

    @Test
    fun checksActorKeyType() {
        typeCheckingVisitor.visitCompilationUnit(
            CompilationUnit(
                "cloud.orbit.test",
                actors = listOf(
                    ActorDeclaration(
                        "a",
                        keyType = TypeReference("t")
                    )
                )
            )
        )

        verify { typeCheck.check(TypeReference("t"), TypeCheck.Context.ACTOR_KEY, typeCheckingVisitor) }
        confirmVerified(typeCheck)
    }

    @Test
    fun checksDataFieldType() {
        typeCheckingVisitor.visitCompilationUnit(
            CompilationUnit(
                "cloud.orbit.test",
                data = listOf(
                    DataDeclaration(
                        "d",
                        fields = listOf(
                            DataField("f", TypeReference("t"), index = 1)
                        )
                    )
                )
            )
        )

        verify { typeCheck.check(TypeReference("t"), TypeCheck.Context.DATA_FIELD, typeCheckingVisitor) }
        confirmVerified(typeCheck)
    }

    @Test
    fun checksMethodReturnType() {
        excludeRecords { typeCheck.check(any(), not(TypeCheck.Context.METHOD_RETURN), any()) }

        typeCheckingVisitor.visitCompilationUnit(
            CompilationUnit(
                "cloud.orbit.test",
                actors = listOf(
                    ActorDeclaration(
                        "a",
                        methods = listOf(
                            ActorMethod(
                                "m",
                                returnType = TypeReference("t")
                            )
                        )
                    )
                )
            )
        )

        verify { typeCheck.check(TypeReference("t"), TypeCheck.Context.METHOD_RETURN, typeCheckingVisitor) }
        confirmVerified(typeCheck)
    }

    @Test
    fun checksMethodParameterType() {
        excludeRecords { typeCheck.check(any(), not(TypeCheck.Context.METHOD_PARAMETER), any()) }

        typeCheckingVisitor.visitCompilationUnit(
            CompilationUnit(
                "cloud.orbit.test",
                actors = listOf(
                    ActorDeclaration(
                        "a",
                        methods = listOf(
                            ActorMethod(
                                "m",
                                returnType = TypeReference("r"),
                                params = listOf(
                                    MethodParameter(
                                        "p",
                                        type = TypeReference("t")
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        verify { typeCheck.check(TypeReference("t"), TypeCheck.Context.METHOD_PARAMETER, typeCheckingVisitor) }
        confirmVerified(typeCheck)
    }

    @Test
    fun checksTypeParameters() {
        excludeRecords { typeCheck.check(any(), not(TypeCheck.Context.TYPE_PARAMETER), any()) }

        typeCheckingVisitor.visitCompilationUnit(
            CompilationUnit(
                "cloud.orbit.test",
                data = listOf(
                    DataDeclaration(
                        "d",
                        fields = listOf(
                            DataField(
                                "f",
                                type = TypeReference(
                                    "t1", of = listOf(
                                        TypeReference(
                                            "t2", of = listOf(
                                                TypeReference("t3")
                                            )
                                        ),
                                        TypeReference("t4")
                                    )
                                ),
                                index = 1
                            )
                        )
                    )
                )
            )
        )

        verifyAll {
            typeCheck.check(
                TypeReference(
                    "t2", of = listOf(
                        TypeReference("t3")
                    )
                ),
                TypeCheck.Context.TYPE_PARAMETER,
                typeCheckingVisitor
            )

            typeCheck.check(
                TypeReference("t3"),
                TypeCheck.Context.TYPE_PARAMETER,
                typeCheckingVisitor
            )

            typeCheck.check(
                TypeReference("t4"),
                TypeCheck.Context.TYPE_PARAMETER,
                typeCheckingVisitor
            )
        }
    }
}
