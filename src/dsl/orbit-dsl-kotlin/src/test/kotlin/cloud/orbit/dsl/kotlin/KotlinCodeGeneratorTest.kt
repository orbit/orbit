/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.kotlin

import cloud.orbit.dsl.ast.ActorDeclaration
import cloud.orbit.dsl.ast.ActorMethod
import cloud.orbit.dsl.ast.CompilationUnit
import cloud.orbit.dsl.ast.DataDeclaration
import cloud.orbit.dsl.ast.DataField
import cloud.orbit.dsl.ast.EnumDeclaration
import cloud.orbit.dsl.ast.EnumMember
import cloud.orbit.dsl.ast.MethodParameter
import cloud.orbit.dsl.ast.TypeReference
import cloud.orbit.dsl.type.PrimitiveType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class KotlinCodeGeneratorTest {
    private val packageName = "cloud.orbit.test"

    @Test
    fun primitiveTypeBooleanIsKotlinBoolean() {
        val cu = CompilationUnit(
            packageName,
            actors = listOf(
                ActorDeclaration(
                    name = "actor1",
                    methods = listOf(
                        ActorMethod(
                            name = "m",
                            returnType = TypeReference(PrimitiveType.BOOLEAN),
                            params = listOf(
                                MethodParameter(
                                    name = "p",
                                    type = TypeReference(PrimitiveType.BOOLEAN)
                                )
                            )
                        )
                    )
                )
            )
        )

        val expectedSource = """
            interface actor1 : cloud.orbit.core.actor.ActorWithNoKey {
                fun m(p: kotlin.Boolean): kotlinx.coroutines.Deferred<kotlin.Boolean>
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun primitiveTypeDoubleIsKotlinDouble() {
        val cu = CompilationUnit(
            packageName,
            actors = listOf(
                ActorDeclaration(
                    name = "actor1",
                    methods = listOf(
                        ActorMethod(
                            name = "m",
                            returnType = TypeReference(PrimitiveType.DOUBLE),
                            params = listOf(
                                MethodParameter(
                                    name = "p",
                                    type = TypeReference(PrimitiveType.DOUBLE)
                                )
                            )
                        )
                    )
                )
            )
        )

        val expectedSource = """
            interface actor1 : cloud.orbit.core.actor.ActorWithNoKey {
                 fun m(p: kotlin.Double): kotlinx.coroutines.Deferred<kotlin.Double>
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun primitiveTypeFloatIsKotlinFloat() {
        val cu = CompilationUnit(
            packageName,
            actors = listOf(
                ActorDeclaration(
                    name = "actor1",
                    methods = listOf(
                        ActorMethod(
                            name = "m",
                            returnType = TypeReference(PrimitiveType.FLOAT),
                            params = listOf(
                                MethodParameter(
                                    name = "p",
                                    type = TypeReference(PrimitiveType.FLOAT)
                                )
                            )
                        )
                    )
                )
            )
        )

        val expectedSource = """
            interface actor1 : cloud.orbit.core.actor.ActorWithNoKey {
              fun m(p: kotlin.Float): kotlinx.coroutines.Deferred<kotlin.Float>
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun primitiveTypeGuidIsJavaUuid() {
        val cu = CompilationUnit(
            packageName,
            actors = listOf(
                ActorDeclaration(
                    name = "actor1",
                    methods = listOf(
                        ActorMethod(
                            name = "m",
                            returnType = TypeReference(PrimitiveType.GUID),
                            params = listOf(
                                MethodParameter(
                                    name = "p",
                                    type = TypeReference(PrimitiveType.GUID)
                                )
                            )
                        )
                    )
                )
            )
        )

        val expectedSource = """
            interface actor1 : cloud.orbit.core.actor.ActorWithNoKey {
                fun m(p: java.util.UUID): kotlinx.coroutines.Deferred<java.util.UUID>
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun primitiveTypeInt32IsKotlinInt() {
        val cu = CompilationUnit(
            packageName,
            actors = listOf(
                ActorDeclaration(
                    name = "actor1",
                    methods = listOf(
                        ActorMethod(
                            name = "m",
                            returnType = TypeReference(PrimitiveType.INT32),
                            params = listOf(
                                MethodParameter(
                                    name = "p",
                                    type = TypeReference(PrimitiveType.INT32)
                                )
                            )
                        )
                    )
                )
            )
        )

        val expectedSource = """
            interface actor1 : cloud.orbit.core.actor.ActorWithNoKey {
                fun m(p: kotlin.Int): kotlinx.coroutines.Deferred<kotlin.Int>
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun primitiveTypeInt64IsKotlinLong() {
        val cu = CompilationUnit(
            packageName,
            actors = listOf(
                ActorDeclaration(
                    name = "actor1",
                    methods = listOf(
                        ActorMethod(
                            name = "m",
                            returnType = TypeReference(PrimitiveType.INT64),
                            params = listOf(
                                MethodParameter(
                                    name = "p",
                                    type = TypeReference(PrimitiveType.INT64)
                                )
                            )
                        )
                    )
                )
            )
        )

        val expectedSource = """
            interface actor1 : cloud.orbit.core.actor.ActorWithNoKey {
                fun m(p: kotlin.Long): kotlinx.coroutines.Deferred<kotlin.Long>
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun primitiveTypeListIsKotlinList() {
        val cu = CompilationUnit(
            packageName,
            actors = listOf(
                ActorDeclaration(
                    name = "actor1",
                    methods = listOf(
                        ActorMethod(
                            name = "m",
                            returnType = TypeReference(PrimitiveType.LIST),
                            params = listOf(
                                MethodParameter(
                                    name = "p",
                                    type = TypeReference(PrimitiveType.LIST)
                                )
                            )
                        )
                    )
                )
            )
        )

        val expectedSource = """
            interface actor1 : cloud.orbit.core.actor.ActorWithNoKey {
                fun m(p: kotlin.collections.List): kotlinx.coroutines.Deferred<kotlin.collections.List>
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun primitiveTypeMapIsKotlinMap() {
        val cu = CompilationUnit(
            packageName,
            actors = listOf(
                ActorDeclaration(
                    name = "actor1",
                    methods = listOf(
                        ActorMethod(
                            name = "m",
                            returnType = TypeReference(PrimitiveType.MAP),
                            params = listOf(
                                MethodParameter(
                                    name = "p",
                                    type = TypeReference(PrimitiveType.MAP)
                                )
                            )
                        )
                    )
                )
            )
        )

        val expectedSource = """
            interface actor1 : cloud.orbit.core.actor.ActorWithNoKey {
                fun m(p: kotlin.collections.Map): kotlinx.coroutines.Deferred<kotlin.collections.Map>
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun primitiveTypeStringIsKotlinString() {
        val cu = CompilationUnit(
            packageName,
            actors = listOf(
                ActorDeclaration(
                    name = "actor1",
                    methods = listOf(
                        ActorMethod(
                            name = "m",
                            returnType = TypeReference(PrimitiveType.STRING),
                            params = listOf(
                                MethodParameter(
                                    name = "p",
                                    type = TypeReference(PrimitiveType.STRING)
                                )
                            )
                        )
                    )
                )
            )
        )

        val expectedSource = """
            interface actor1 : cloud.orbit.core.actor.ActorWithNoKey {
                fun m(p: kotlin.String): kotlinx.coroutines.Deferred<kotlin.String>
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun primitiveTypeVoidIsKotlinUnit() {
        val cu = CompilationUnit(
            packageName,
            actors = listOf(
                ActorDeclaration(
                    name = "actor1",
                    methods = listOf(
                        ActorMethod(
                            name = "m",
                            returnType = TypeReference(PrimitiveType.VOID),
                            params = listOf(
                                MethodParameter(
                                    name = "p",
                                    type = TypeReference(PrimitiveType.VOID)
                                )
                            )
                        )
                    )
                )
            )
        )

        val expectedSource = """
            interface actor1 : cloud.orbit.core.actor.ActorWithNoKey {
              fun m(p: kotlin.Unit): kotlinx.coroutines.Deferred<kotlin.Unit>
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateEnum_OneMember() {
        val cu = CompilationUnit(
            packageName,
            enums = listOf(
                EnumDeclaration(
                    name = "enum1",
                    members = listOf(EnumMember("MEMBER_1", 3))
                )
            )
        )

        val expectedSource = """
            enum class enum1 {
                MEMBER_1
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateEnum_MultipleMember() {
        val cu = CompilationUnit(
            packageName,
            enums = listOf(
                EnumDeclaration(
                    name = "enum1",
                    members = listOf(
                        EnumMember("MEMBER_1", 3),
                        EnumMember("MEMBER_2", 5)
                    )
                )
            )
        )

        val expectedSource = """
            enum class enum1 {
                MEMBER_1,
                MEMBER_2
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateData_NoFields() {
        val cu = CompilationUnit(packageName, data = listOf(DataDeclaration("data1")))

        val expectedSource = "data class data1()"

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateData_SingleField() {
        val cu = CompilationUnit(
            packageName, data = listOf(
                DataDeclaration(
                    name = "data1",
                    fields = listOf(DataField("Field1", TypeReference("string"), 3))
                )
            )
        )

        val expectedSource = "data class data1(val field1: kotlin.String)"

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateData_MultipleFields() {
        val cu = CompilationUnit(
            packageName, data = listOf(
                DataDeclaration(
                    name = "data1",
                    fields = listOf(
                        DataField("Field1", TypeReference("string"), 3),
                        DataField("Field2", TypeReference("int32"), 5)
                    )
                )
            )
        )

        val expectedSource = "data class data1(val field1: kotlin.String, val field2: kotlin.Int)"

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateData_GenericField_PrimitiveTypes() {
        val cu = CompilationUnit(
            packageName, data = listOf(
                DataDeclaration(
                    name = "data1",
                    fields = listOf(
                        DataField(
                            name = "field1",
                            type = TypeReference(
                                name = "list",
                                of = listOf(
                                    TypeReference(
                                        name = "map",
                                        of = listOf(
                                            TypeReference("string"),
                                            TypeReference("int32")
                                        )
                                    )
                                )
                            ),
                            index = 3
                        )
                    )
                )
            )
        )

        val expectedSource = "data class data1(val field1: kotlin.collections.List<kotlin.collections.Map<kotlin.String, kotlin.Int>>)"

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateData_GenericField_CustomTypes() {
        val cu = CompilationUnit(
            packageName,
            enums = listOf(
                EnumDeclaration(
                    name = "enum1",
                    members = listOf(
                        EnumMember("A", 1),
                        EnumMember("B", 2)
                    )
                )
            ),
            data = listOf(
                DataDeclaration(
                    name = "data1",
                    fields = listOf(DataField("field1", TypeReference("string"), 1))
                ),
                DataDeclaration(
                    name = "data2",
                    fields = listOf(
                        DataField(
                            name = "field1",
                            type = TypeReference("list", of = listOf(TypeReference("enum1"))),
                            index = 1
                        ),
                        DataField(
                            name = "field2",
                            type = TypeReference("list", of = listOf(TypeReference("data1"))),
                            index = 2
                        )
                    )
                )
            )
        )

        val expectedSources = listOf(
            """
            enum class enum1 {
                A,
                B
            }
            """,
            "data class data1(val field1: kotlin.String)",
            """
            data class data2(
                val field1: kotlin.collections.List<$packageName.enum1>,
                val field2: kotlin.collections.List<$packageName.data1>)
            """
        )

        generateSource_minimalPipeline(cu).forEachIndexed { index, compiledType ->
            Assertions.assertEquals(packageName, compiledType.packageName)
            assertSourceMatch(expectedSources[index], compiledType.toString())
        }
    }

    @Test
    fun generateActor_Empty() {
        val cu = CompilationUnit(packageName, actors = listOf(ActorDeclaration("actor1")))

        val expectedSource = "interface actor1 : cloud.orbit.core.actor.ActorWithNoKey"

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generatorActor_NoKey() {
        val cu = CompilationUnit(
            packageName,
            actors = listOf(
                ActorDeclaration(
                    name = "actor1",
                    keyType = TypeReference(PrimitiveType.VOID)
                )
            )
        )

        val expectedSource = "interface actor1 : cloud.orbit.core.actor.ActorWithNoKey"

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generatorActor_StringKey() {
        val cu = CompilationUnit(
            packageName,
            actors = listOf(
                ActorDeclaration(
                    name = "actor1",
                    keyType = TypeReference(PrimitiveType.STRING)
                )
            )
        )

        val expectedSource = "interface actor1 : cloud.orbit.core.actor.ActorWithStringKey"

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generatorActor_Int32Key() {
        val cu = CompilationUnit(
            packageName,
            actors = listOf(
                ActorDeclaration(
                    name = "actor1",
                    keyType = TypeReference(PrimitiveType.INT32)
                )
            )
        )

        val expectedSource = "interface actor1 : cloud.orbit.core.actor.ActorWithInt32Key"

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generatorActor_Int64Key() {
        val cu = CompilationUnit(
            packageName,
            actors = listOf(
                ActorDeclaration(
                    name = "actor1",
                    keyType = TypeReference(PrimitiveType.INT64)
                )
            )
        )

        val expectedSource = "interface actor1 : cloud.orbit.core.actor.ActorWithInt64Key"

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generatorActor_GuidKey() {
        val cu = CompilationUnit(
            packageName,
            actors = listOf(
                ActorDeclaration(
                    name = "actor1",
                    keyType = TypeReference(PrimitiveType.GUID)
                )
            )
        )

        val expectedSource = "interface actor1 : cloud.orbit.core.actor.ActorWithGuidKey"

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateActor_SingleMethod() {
        val cu = CompilationUnit(
            packageName, actors = listOf(
                ActorDeclaration(
                    name = "actor1",
                    methods = listOf(
                        ActorMethod(
                            name = "method1",
                            returnType = TypeReference("string")
                        )
                    )
                )
            )
        )

        val expectedSource = """
            interface actor1 : cloud.orbit.core.actor.ActorWithNoKey  {
                fun method1(): kotlinx.coroutines.Deferred<kotlin.String>
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateActor_MultipleMethods() {
        val cu = CompilationUnit(
            packageName, actors = listOf(
                ActorDeclaration(
                    name = "actor1",
                    methods = listOf(
                        ActorMethod(
                            name = "method1",
                            returnType = TypeReference("string")
                        ),
                        ActorMethod(
                            name = "method2",
                            returnType = TypeReference("string")
                        )
                    )
                )
            )
        )

        val expectedSource = """
            interface actor1 : cloud.orbit.core.actor.ActorWithNoKey  {
                fun method1(): kotlinx.coroutines.Deferred<kotlin.String>
                fun method2(): kotlinx.coroutines.Deferred<kotlin.String>
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateActor_SingleParamMethod() {
        val cu = CompilationUnit(
            packageName, actors = listOf(
                ActorDeclaration(
                    "actor1",
                    methods = listOf(
                        ActorMethod(
                            name = "method1",
                            returnType = TypeReference("string"),
                            params = listOf(MethodParameter("p1", TypeReference("int32")))
                        )
                    )
                )
            )
        )

        val expectedSource = """
            interface actor1 : cloud.orbit.core.actor.ActorWithNoKey  {
                fun method1(p1: kotlin.Int): kotlinx.coroutines.Deferred<kotlin.String>
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateActor_MultipleParamsMethod() {
        val cu = CompilationUnit(
            packageName, actors = listOf(
                ActorDeclaration(
                    "actor1",
                    methods = listOf(
                        ActorMethod(
                            name = "method1",
                            returnType = TypeReference("string"),
                            params = listOf(
                                MethodParameter("p1", TypeReference("int32")),
                                MethodParameter("p2", TypeReference("int32"))
                            )
                        )
                    )
                )
            )
        )

        val expectedSource = """
            interface actor1 : cloud.orbit.core.actor.ActorWithNoKey  {
                fun method1(p1: kotlin.Int, p2: kotlin.Int): kotlinx.coroutines.Deferred<kotlin.String>
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateActor_GenericReturnType_PrimitiveDataTypes() {
        val cu = CompilationUnit(
            packageName,
            actors = listOf(
                ActorDeclaration(
                    name = "actor1",
                    methods = listOf(
                        ActorMethod(
                            name = "method1",
                            returnType = TypeReference(
                                name = "map",
                                of = listOf(
                                    TypeReference("string"),
                                    TypeReference(
                                        name = "list", of = listOf(
                                            TypeReference(
                                                name = "list", of = listOf(TypeReference("int64"))
                                            )
                                        )
                                    )
                                )
                            ),
                            params = emptyList()
                        )
                    )
                )
            )
        )

        val expectedSource = """
            interface actor1 : cloud.orbit.core.actor.ActorWithNoKey  {
                fun method1(): kotlinx.coroutines.Deferred<kotlin.collections.Map<kotlin.String, kotlin.collections.List<kotlin.collections.List<kotlin.Long>>>>
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateActor_GenericReturnType_CustomDataTypes() {
        val cu = CompilationUnit(
            packageName,
            enums = listOf(
                EnumDeclaration(
                    name = "enum1",
                    members = listOf(
                        EnumMember("A", 1),
                        EnumMember("B", 2)
                    )
                )
            ),
            data = listOf(
                DataDeclaration(
                    name = "data1",
                    fields = listOf(
                        DataField(
                            "field1", TypeReference("string"), 1
                        )
                    )
                )
            ),
            actors = listOf(
                ActorDeclaration(
                    name = "actor1",
                    methods = listOf(
                        ActorMethod(
                            name = "method1",
                            returnType = TypeReference(
                                name = "map",
                                of = listOf(TypeReference("enum1"), TypeReference("data1"))
                            ),
                            params = emptyList()
                        )
                    )
                )
            )
        )

        val expectedSources = listOf(
            """
            enum class enum1 {
                A,
                B
            }
            """,
            "data class data1(val field1: kotlin.String)",
            """
            interface actor1 : cloud.orbit.core.actor.ActorWithNoKey  {
                fun method1(): kotlinx.coroutines.Deferred<kotlin.collections.Map<$packageName.enum1, $packageName.data1>>
            }
            """
        )

        generateSource_minimalPipeline(cu).forEachIndexed { index, compiledType ->
            Assertions.assertEquals(packageName, compiledType.packageName)
            assertSourceMatch(expectedSources[index], compiledType.toString())
        }
    }

    @Test
    fun generateActor_GenericParameterType_PrimitiveDataTypes() {
        val cu = CompilationUnit(
            packageName,
            actors = listOf(
                ActorDeclaration(
                    "actor1",
                    methods = listOf(
                        ActorMethod(
                            name = "method1",
                            returnType = TypeReference("int32"),
                            params = listOf(
                                MethodParameter(
                                    name = "arg1",
                                    type = TypeReference(
                                        name = "map",
                                        of = listOf(
                                            TypeReference("string"),
                                            TypeReference(
                                                name = "list",
                                                of = listOf(
                                                    TypeReference(
                                                        name = "list",
                                                        of = listOf(TypeReference("int64"))
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        val expectedSource = """
            interface actor1 : cloud.orbit.core.actor.ActorWithNoKey  {
                fun method1(arg1: kotlin.collections.Map<kotlin.String, kotlin.collections.List<kotlin.collections.List<kotlin.Long>>>): kotlinx.coroutines.Deferred<kotlin.Int>
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@KotlinCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateActor_GenericParameterType_CustomDataTypes() {
        val cu = CompilationUnit(
            packageName,
            enums = listOf(
                EnumDeclaration(
                    name = "enum1",
                    members = listOf(
                        EnumMember("A", 1),
                        EnumMember("B", 2)
                    )
                )
            ),
            data = listOf(
                DataDeclaration(
                    name = "data1",
                    fields = listOf(
                        DataField(
                            name = "field1",
                            type = TypeReference("string"),
                            index = 1
                        )
                    )
                )
            ),
            actors = listOf(
                ActorDeclaration(
                    name = "actor1",
                    methods = listOf(
                        ActorMethod(
                            name = "method1",
                            returnType = TypeReference("int32"),
                            params = listOf(
                                MethodParameter(
                                    name = "arg1",
                                    type = TypeReference(
                                        name = "map",
                                        of = listOf(TypeReference("enum1"), TypeReference("data1"))
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        val expectedSources = listOf(
            """
            enum class enum1 {
                A,
                B
            }
            """,
            "data class data1(val field1: kotlin.String)",
            """
            interface actor1 : cloud.orbit.core.actor.ActorWithNoKey  {
                fun method1(arg1: kotlin.collections.Map<$packageName.enum1, $packageName.data1>): kotlinx.coroutines.Deferred<kotlin.Int>
            }
            """
        )

        generateSource_minimalPipeline(cu).forEachIndexed { index, compiledType ->
            Assertions.assertEquals(packageName, compiledType.packageName)
            assertSourceMatch(expectedSources[index], compiledType.toString())
        }
    }

    private fun generateSource_minimalPipeline(vararg cus: CompilationUnit) = with(cus.toList()) {
        KotlinCodeGenerator(
            KotlinTypeIndexer()
                .visitCompilationUnits(this)
        ).visitCompilationUnits(this)
    }

    private fun <T> assertOneElement(list: List<T>): T {
        Assertions.assertEquals(1, list.size)
        return list.first()
    }

    private fun assertSourceMatch(expected: String, actual: String) {
        // Basically convert the source code to a single line string with no spaces. Not ideal but it works
        // until we have strings or other concepts that require space/newline preservation
        fun normalizeSource(s: String) = s
            .replace("\\n+".toRegex(), "") // replace new lines with a single space
            .replace("\\s+".toRegex(), "") // replace multiple spaces with a single one
            .trim(' ') // Trim leading and ending spaces

        Assertions.assertEquals(
            normalizeSource(expected),
            normalizeSource(actual),
            "Expected:\n${expected}\nActual:\n${actual}"
        )
    }
}
