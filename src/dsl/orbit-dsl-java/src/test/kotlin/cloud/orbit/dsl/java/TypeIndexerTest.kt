/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.java

import cloud.orbit.dsl.ast.ActorDeclaration
import cloud.orbit.dsl.ast.CompilationUnit
import cloud.orbit.dsl.ast.DataDeclaration
import cloud.orbit.dsl.ast.EnumDeclaration
import cloud.orbit.dsl.ast.TypeReference
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TypeIndexerTest {
    private val expectedPredefinedTypes = mapOf(
        TypeReference("boolean") to TypeName.BOOLEAN,
        TypeReference("double") to TypeName.DOUBLE,
        TypeReference("float") to TypeName.FLOAT,
        TypeReference("int32") to TypeName.INT,
        TypeReference("int64") to TypeName.LONG,
        TypeReference("string") to ClassName.get(String::class.java),
        TypeReference("list") to ClassName.get(java.util.List::class.java),
        TypeReference("map") to ClassName.get(java.util.Map::class.java)
    )

    @Test
    fun indexDefaultTypes() {
        val actual = TypeIndexer().visitCompilationUnits(emptyList())
        Assertions.assertEquals(expectedPredefinedTypes, actual)
    }

    @Test
    fun indexEnum() {
        val actual = TypeIndexer().visitCompilationUnits(
            listOf(
                CompilationUnit("foo", enums = listOf(EnumDeclaration("bar")))
            )
        )

        val expectedTypes = mutableMapOf<TypeReference, TypeName>(TypeReference("bar") to ClassName.get("foo", "bar"))
        expectedTypes.putAll(expectedPredefinedTypes)

        Assertions.assertEquals(expectedTypes, actual)
    }

    @Test
    fun indexData() {
        val actual = TypeIndexer().visitCompilationUnits(
            listOf(
                CompilationUnit("foo", data = listOf(DataDeclaration("bar")))
            )
        )

        val expectedTypes = mutableMapOf<TypeReference, TypeName>(TypeReference("bar") to ClassName.get("foo", "bar"))
        expectedTypes.putAll(expectedPredefinedTypes)

        Assertions.assertEquals(expectedTypes, actual)
    }

    @Test
    fun indexActor() {
        val actual = TypeIndexer().visitCompilationUnits(
            listOf(
                CompilationUnit("foo", actors = listOf(ActorDeclaration("bar")))
            )
        )

        val expectedTypes = mutableMapOf<TypeReference, TypeName>(TypeReference("bar") to ClassName.get("foo", "bar"))
        expectedTypes.putAll(expectedPredefinedTypes)

        Assertions.assertEquals(expectedTypes, actual)
    }
}
