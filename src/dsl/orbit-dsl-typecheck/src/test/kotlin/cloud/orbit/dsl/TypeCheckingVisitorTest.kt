/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

import cloud.orbit.dsl.ast.ActorDeclaration
import cloud.orbit.dsl.ast.ActorMethod
import cloud.orbit.dsl.ast.CompilationUnit
import cloud.orbit.dsl.ast.DataDeclaration
import cloud.orbit.dsl.ast.DataField
import cloud.orbit.dsl.ast.MethodParameter
import cloud.orbit.dsl.ast.TypeReference
import cloud.orbit.dsl.ast.error.ErrorReporter
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeCheckingVisitorTest {
    @Test
    fun checksActorKeyType() {
        val collectingTypeCheck = CollectingTypeCheck()
        val visitor = TypeCheckingVisitor(listOf(collectingTypeCheck))

        visitor.visitCompilationUnit(
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

        assertTrue(
            collectingTypeCheck.typesChecked.contains(
                TypeCheckInvocation(TypeReference("t"), TypeCheck.Context.ACTOR_KEY)
            )
        )
    }

    @Test
    fun checksDataFieldType() {
        val collectingTypeCheck = CollectingTypeCheck()
        val visitor = TypeCheckingVisitor(listOf(collectingTypeCheck))

        visitor.visitCompilationUnit(
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

        assertTrue(
            collectingTypeCheck.typesChecked.contains(
                TypeCheckInvocation(TypeReference("t"), TypeCheck.Context.DATA_FIELD)
            )
        )
    }

    @Test
    fun checksMethodReturnType() {
        val collectingTypeCheck = CollectingTypeCheck()
        val visitor = TypeCheckingVisitor(listOf(collectingTypeCheck))

        visitor.visitCompilationUnit(
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

        assertTrue(
            collectingTypeCheck.typesChecked.contains(
                TypeCheckInvocation(TypeReference("t"), TypeCheck.Context.METHOD_RETURN)
            )
        )
    }

    @Test
    fun checksMethodParameterType() {
        val collectingTypeCheck = CollectingTypeCheck()
        val visitor = TypeCheckingVisitor(listOf(collectingTypeCheck))

        visitor.visitCompilationUnit(
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

        assertTrue(
            collectingTypeCheck.typesChecked.contains(
                TypeCheckInvocation(TypeReference("t"), TypeCheck.Context.METHOD_PARAMETER)
            )
        )
    }

    @Test
    fun checksTypeParameters() {
        val collectingTypeCheck = CollectingTypeCheck()
        val visitor = TypeCheckingVisitor(listOf(collectingTypeCheck))

        visitor.visitCompilationUnit(
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

        assertTrue(
            collectingTypeCheck.typesChecked.contains(
                TypeCheckInvocation(
                    TypeReference(
                        "t1", of = listOf(
                            TypeReference(
                                "t2", of = listOf(
                                    TypeReference("t3")
                                )
                            ),
                            TypeReference("t4")
                        )
                    ),
                    TypeCheck.Context.DATA_FIELD
                )
            )
        )
        assertTrue(
            collectingTypeCheck.typesChecked.contains(
                TypeCheckInvocation(
                    TypeReference(
                        "t2", of = listOf(
                            TypeReference("t3")
                        )
                    ),
                    TypeCheck.Context.TYPE_PARAMETER
                )
            )
        )
        assertTrue(
            collectingTypeCheck.typesChecked.contains(
                TypeCheckInvocation(
                    TypeReference("t3"),
                    TypeCheck.Context.TYPE_PARAMETER
                )
            )
        )
        assertTrue(
            collectingTypeCheck.typesChecked.contains(
                TypeCheckInvocation(
                    TypeReference("t4"),
                    TypeCheck.Context.TYPE_PARAMETER
                )
            )
        )
    }

    private data class TypeCheckInvocation(val type: TypeReference, val context: TypeCheck.Context)

    private class CollectingTypeCheck : TypeCheck {
        val typesChecked = mutableListOf<TypeCheckInvocation>()

        override fun check(typeReference: TypeReference, context: TypeCheck.Context, errorReporter: ErrorReporter) {
            typesChecked.add(TypeCheckInvocation(typeReference, context))
        }
    }
}
