/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.java

import cloud.orbit.dsl.ast.*
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TypeIndexerTest {
    private val expectedPredefinedTypes = mapOf(
        Type("boolean") to TypeName.BOOLEAN,
        Type("double") to TypeName.DOUBLE,
        Type("float") to TypeName.FLOAT,
        Type("int32") to TypeName.INT,
        Type("int64") to TypeName.LONG,
        Type("string") to ClassName.get(String::class.java)
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

        val expectedTypes = mutableMapOf<Type, TypeName>(Type("bar") to ClassName.get("foo", "bar"))
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

        val expectedTypes = mutableMapOf<Type, TypeName>(Type("bar") to ClassName.get("foo", "bar"))
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

        val expectedTypes = mutableMapOf<Type, TypeName>(Type("bar") to ClassName.get("foo", "bar"))
        expectedTypes.putAll(expectedPredefinedTypes)

        Assertions.assertEquals(expectedTypes, actual)
    }
}
