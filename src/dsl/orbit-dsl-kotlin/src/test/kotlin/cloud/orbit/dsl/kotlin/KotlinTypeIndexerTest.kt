/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.kotlin

import cloud.orbit.dsl.ast.ActorDeclaration
import cloud.orbit.dsl.ast.CompilationUnit
import cloud.orbit.dsl.ast.DataDeclaration
import cloud.orbit.dsl.ast.EnumDeclaration
import cloud.orbit.dsl.ast.TypeReference
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class KotlinTypeIndexerTest {
    @Test
    fun indexDefaultTypes() {
        val expectedPredefinedTypes = mapOf(
            TypeReference("boolean") to Boolean::class.asTypeName(),
            TypeReference("double") to Double::class.asTypeName(),
            TypeReference("float") to Float::class.asTypeName(),
            TypeReference("guid") to java.util.UUID::class.asClassName(),
            TypeReference("int32") to Int::class.asTypeName(),
            TypeReference("int64") to Long::class.asTypeName(),
            TypeReference("list") to List::class.asClassName(),
            TypeReference("map") to Map::class.asClassName(),
            TypeReference("string") to String::class.asTypeName(),
            TypeReference("void") to Unit::class.asTypeName()
        )

        val types = KotlinTypeIndexer().visitCompilationUnits(emptyList())
        Assertions.assertEquals(expectedPredefinedTypes, types)
    }

    @Test
    fun indexEnum() {
        val types = KotlinTypeIndexer().visitCompilationUnits(
            listOf(
                CompilationUnit("foo", enums = listOf(EnumDeclaration("bar")))
            )
        )

        Assertions.assertEquals(
            ClassName("foo", "bar"),
            types[TypeReference("bar")]
        )
    }

    @Test
    fun indexData() {
        val types = KotlinTypeIndexer().visitCompilationUnits(
            listOf(
                CompilationUnit("foo", data = listOf(DataDeclaration("bar")))
            )
        )

        Assertions.assertEquals(
            ClassName("foo", "bar"),
            types[TypeReference("bar")]
        )
    }

    @Test
    fun indexActor() {
        val types = KotlinTypeIndexer().visitCompilationUnits(
            listOf(
                CompilationUnit("foo", actors = listOf(ActorDeclaration("bar")))
            )
        )

        Assertions.assertEquals(
            ClassName("foo", "bar"),
            types[TypeReference("bar")]
        )
    }
}
