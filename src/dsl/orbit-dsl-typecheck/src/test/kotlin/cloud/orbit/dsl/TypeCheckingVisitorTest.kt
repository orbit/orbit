/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

import cloud.orbit.dsl.ast.ActorDeclaration
import cloud.orbit.dsl.ast.ActorKeyType
import cloud.orbit.dsl.ast.ActorMethod
import cloud.orbit.dsl.ast.CompilationUnit
import cloud.orbit.dsl.ast.DataDeclaration
import cloud.orbit.dsl.ast.DataField
import cloud.orbit.dsl.ast.MethodParameter
import cloud.orbit.dsl.ast.Type
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeCheckingVisitorTest {
    @Test
    fun checksDataFieldTypes() {
        val collectingTypeCheck = CollectingTypeCheck()
        val visitor = TypeCheckingVisitor(listOf(collectingTypeCheck))
        visitor.visitCompilationUnit(
            CompilationUnit(
                "cloud.orbit.test",
                data = listOf(
                    DataDeclaration(
                        "data1",
                        fields = listOf(
                            DataField("field1", Type("ft1"), index = 1),
                            DataField("field2", Type("ft2"), index = 2)
                        )
                    )
                )
            )
        )

        assertTrue(collectingTypeCheck.typesChecked.contains(Type("ft1")))
        assertTrue(collectingTypeCheck.typesChecked.contains(Type("ft2")))
    }

    @Test
    fun checksMethodReturnTypes() {
        val collectingTypeCheck = CollectingTypeCheck()
        val visitor = TypeCheckingVisitor(listOf(collectingTypeCheck))
        visitor.visitCompilationUnit(
            CompilationUnit(
                "cloud.orbit.test",
                actors = listOf(
                    ActorDeclaration(
                        "actor1",
                        ActorKeyType.NO_KEY,
                        methods = listOf(
                            ActorMethod(
                                "method1",
                                returnType = Type("mrt1")
                            ),
                            ActorMethod(
                                "method2",
                                returnType = Type("mrt2")
                            )
                        )
                    )
                )
            )
        )

        assertTrue(collectingTypeCheck.typesChecked.contains(Type("mrt1")))
        assertTrue(collectingTypeCheck.typesChecked.contains(Type("mrt2")))
    }

    @Test
    fun checksMethodParameterTypes() {
        val collectingTypeCheck = CollectingTypeCheck()
        val visitor = TypeCheckingVisitor(listOf(collectingTypeCheck))
        visitor.visitCompilationUnit(
            CompilationUnit(
                "cloud.orbit.test",
                actors = listOf(
                    ActorDeclaration(
                        "actor1",
                        ActorKeyType.NO_KEY,
                        methods = listOf(
                            ActorMethod(
                                "method1",
                                returnType = Type("mrt"),
                                params = listOf(
                                    MethodParameter(
                                        "p1",
                                        type = Type("mpt1")
                                    ),
                                    MethodParameter(
                                        "p2",
                                        type = Type("mpt2")
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        assertTrue(collectingTypeCheck.typesChecked.contains(Type("mpt1")))
        assertTrue(collectingTypeCheck.typesChecked.contains(Type("mpt2")))
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
                        "data1",
                        fields = listOf(
                            DataField(
                                "field1",
                                type = Type(
                                    "ft", of = listOf(
                                        Type(
                                            "tp1", of = listOf(
                                                Type("tp11")
                                            )
                                        ),
                                        Type("tp2")
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
                Type(
                    "ft", of = listOf(
                        Type(
                            "tp1", of = listOf(
                                Type("tp11")
                            )
                        ),
                        Type("tp2")
                    )
                )
            )
        )
        assertTrue(
            collectingTypeCheck.typesChecked.contains(
                Type(
                    "tp1", of = listOf(
                        Type("tp11")
                    )
                )
            )
        )
        assertTrue(collectingTypeCheck.typesChecked.contains(Type("tp11")))
        assertTrue(collectingTypeCheck.typesChecked.contains(Type("tp2")))
    }

    @Test
    fun reportsTypeCheckErrors() {
        val errorListener = TypeErrorListener()
        val compilationUnit = CompilationUnit(
            "cloud.orbit.test",
            data = listOf(
                DataDeclaration(
                    "data1",
                    fields = listOf(
                        DataField(
                            "field1",
                            Type(
                                "ft1",
                                of = listOf(
                                    Type("tp1")
                                )
                            ),
                            index = 1
                        )
                    )
                )
            )
        )

        val visitor = TypeCheckingVisitor(listOf(ErrorReportingTypeCheck()))
        visitor.addErrorListener(errorListener)
        visitor.visitCompilationUnit(compilationUnit)
        assertEquals(2, errorListener.typeErrors.size)

        visitor.removeErrorListener(errorListener)
        visitor.visitCompilationUnit(compilationUnit)
        // No more errors reported
        assertEquals(2, errorListener.typeErrors.size)
    }

    private class CollectingTypeCheck : TypeCheck() {
        val typesChecked = mutableListOf<Type>()

        override fun check(type: Type) {
            typesChecked.add(type)
        }
    }

    private class ErrorReportingTypeCheck : TypeCheck() {
        override fun check(type: Type) {
            reportError(type, "")
        }
    }
}
