/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.type.check

import cloud.orbit.dsl.ast.ActorDeclaration
import cloud.orbit.dsl.ast.CompilationUnit
import cloud.orbit.dsl.ast.DataDeclaration
import cloud.orbit.dsl.ast.DataField
import cloud.orbit.dsl.ast.TypeReference
import cloud.orbit.dsl.error.OrbitDslCompilationException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OrbitDslTypeCheckerTest {
    @Test
    fun checksActorKeyType() {
        val compilationUnit = CompilationUnit(
            "cloud.orbit.test",
            actors = listOf(
                ActorDeclaration(
                    name = "a1",
                    keyType = TypeReference("foo")
                )
            )
        )

        assertThrows<OrbitDslCompilationException> {
            OrbitDslTypeChecker.checkTypes(listOf(compilationUnit))
        }
    }

    @Test
    fun checksTypeArity() {
        val compilationUnit = CompilationUnit(
            "cloud.orbit.test",
            data = listOf(
                DataDeclaration(
                    name = "d1",
                    fields = listOf(
                        DataField(
                            name = "f1",
                            type = TypeReference(
                                name = "map",
                                of = listOf(TypeReference("string")) // wrong arity
                            ),
                            index = 1
                        )
                    )
                )
            )
        )

        assertThrows<OrbitDslCompilationException> {
            OrbitDslTypeChecker.checkTypes(listOf(compilationUnit))
        }
    }

    @Test
    fun checksVoidUsage() {
        val compilationUnit = CompilationUnit(
            "cloud.orbit.test",
            data = listOf(
                DataDeclaration(
                    name = "d1",
                    fields = listOf(
                        DataField(
                            name = "f1",
                            type = TypeReference("void"),
                            index = 1
                        )
                    )
                )
            )
        )

        assertThrows<OrbitDslCompilationException> {
            OrbitDslTypeChecker.checkTypes(listOf(compilationUnit))
        }
    }
}
