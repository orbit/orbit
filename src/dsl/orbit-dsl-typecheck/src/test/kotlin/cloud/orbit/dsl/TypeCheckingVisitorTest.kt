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
import cloud.orbit.dsl.ast.Type
import cloud.orbit.dsl.ast.error.ErrorReporter
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeCheckingVisitorTest {
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
                            DataField("f", Type("t"), index = 1)
                        )
                    )
                )
            )
        )

        assertTrue(
            collectingTypeCheck.typesChecked.contains(
                TypeCheckInvocation(Type("t"), TypeCheck.Context.DATA_FIELD)
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
                                returnType = Type("t")
                            )
                        )
                    )
                )
            )
        )

        assertTrue(
            collectingTypeCheck.typesChecked.contains(
                TypeCheckInvocation(Type("t"), TypeCheck.Context.METHOD_RETURN)
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
                                returnType = Type("r"),
                                params = listOf(
                                    MethodParameter(
                                        "p",
                                        type = Type("t")
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
                TypeCheckInvocation(Type("t"), TypeCheck.Context.METHOD_PARAMETER)
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
                                type = Type(
                                    "t1", of = listOf(
                                        Type(
                                            "t2", of = listOf(
                                                Type("t3")
                                            )
                                        ),
                                        Type("t4")
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
                    Type(
                        "t1", of = listOf(
                            Type(
                                "t2", of = listOf(
                                    Type("t3")
                                )
                            ),
                            Type("t4")
                        )
                    ),
                    TypeCheck.Context.DATA_FIELD
                )
            )
        )
        assertTrue(
            collectingTypeCheck.typesChecked.contains(
                TypeCheckInvocation(
                    Type(
                        "t2", of = listOf(
                            Type("t3")
                        )
                    ),
                    TypeCheck.Context.TYPE_PARAMETER
                )
            )
        )
        assertTrue(
            collectingTypeCheck.typesChecked.contains(
                TypeCheckInvocation(
                    Type("t3"),
                    TypeCheck.Context.TYPE_PARAMETER
                )
            )
        )
        assertTrue(
            collectingTypeCheck.typesChecked.contains(
                TypeCheckInvocation(
                    Type("t4"),
                    TypeCheck.Context.TYPE_PARAMETER
                )
            )
        )
    }

    private data class TypeCheckInvocation(val type: Type, val context: TypeCheck.Context)

    private class CollectingTypeCheck : TypeCheck {
        val typesChecked = mutableListOf<TypeCheckInvocation>()

        override fun check(type: Type, context: TypeCheck.Context, errorReporter: ErrorReporter) {
            typesChecked.add(TypeCheckInvocation(type, context))
        }
    }
}
