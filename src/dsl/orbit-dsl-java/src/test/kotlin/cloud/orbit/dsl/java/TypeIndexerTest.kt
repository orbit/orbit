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
    @Test
    fun indexDefaultTypes() {
        val expectedPredefinedTypes = mapOf(
            TypeReference("boolean") to TypeName.BOOLEAN,
            TypeReference("double") to TypeName.DOUBLE,
            TypeReference("float") to TypeName.FLOAT,
            TypeReference("guid") to ClassName.get(java.util.UUID::class.java),
            TypeReference("int32") to TypeName.INT,
            TypeReference("int64") to TypeName.LONG,
            TypeReference("list") to ClassName.get(java.util.List::class.java),
            TypeReference("map") to ClassName.get(java.util.Map::class.java),
            TypeReference("string") to ClassName.get(java.lang.String::class.java),
            TypeReference("void") to ClassName.get(java.lang.Void::class.java)
        )

        val types = TypeIndexer().visitCompilationUnits(emptyList())
        Assertions.assertEquals(expectedPredefinedTypes, types)
    }

    @Test
    fun indexEnum() {
        val types = TypeIndexer().visitCompilationUnits(
            listOf(
                CompilationUnit("foo", enums = listOf(EnumDeclaration("bar")))
            )
        )

        Assertions.assertEquals(
            ClassName.get("foo", "bar"),
            types[TypeReference("bar")]
        )
    }

    @Test
    fun indexData() {
        val types = TypeIndexer().visitCompilationUnits(
            listOf(
                CompilationUnit("foo", data = listOf(DataDeclaration("bar")))
            )
        )

        Assertions.assertEquals(
            ClassName.get("foo", "bar"),
            types[TypeReference("bar")]
        )
    }

    @Test
    fun indexActor() {
        val types = TypeIndexer().visitCompilationUnits(
            listOf(
                CompilationUnit("foo", actors = listOf(ActorDeclaration("bar")))
            )
        )

        Assertions.assertEquals(
            ClassName.get("foo", "bar"),
            types[TypeReference("bar")]
        )
    }
}
