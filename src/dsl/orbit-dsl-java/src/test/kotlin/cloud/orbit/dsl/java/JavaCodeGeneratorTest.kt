/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.java

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

class JavaCodeGeneratorTest {
    private val packageName = "cloud.orbit.test"

    @Test
    fun primitiveTypeBooleanIsJavaBoolean() {
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
            public interface actor1 extends cloud.orbit.core.actor.ActorWithNoKey {
              java.util.concurrent.CompletableFuture<java.lang.Boolean> m(boolean p);
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun primitiveTypeDoubleIsJavaDouble() {
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
            public interface actor1 extends cloud.orbit.core.actor.ActorWithNoKey {
              java.util.concurrent.CompletableFuture<java.lang.Double> m(double p);
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun primitiveTypeFloatIsJavaFloat() {
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
            public interface actor1 extends cloud.orbit.core.actor.ActorWithNoKey {
              java.util.concurrent.CompletableFuture<java.lang.Float> m(float p);
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
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
            public interface actor1 extends cloud.orbit.core.actor.ActorWithNoKey {
              java.util.concurrent.CompletableFuture<java.util.UUID> m(java.util.UUID p);
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun primitiveTypeInt32IsJavaInteger() {
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
            public interface actor1 extends cloud.orbit.core.actor.ActorWithNoKey {
              java.util.concurrent.CompletableFuture<java.lang.Integer> m(int p);
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun primitiveTypeInt64IsJavaLong() {
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
            public interface actor1 extends cloud.orbit.core.actor.ActorWithNoKey {
              java.util.concurrent.CompletableFuture<java.lang.Long> m(long p);
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun primitiveTypeListIsJavaList() {
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
            public interface actor1 extends cloud.orbit.core.actor.ActorWithNoKey {
              java.util.concurrent.CompletableFuture<java.util.List> m(java.util.List p);
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun primitiveTypeMapIsJavaMap() {
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
            public interface actor1 extends cloud.orbit.core.actor.ActorWithNoKey {
              java.util.concurrent.CompletableFuture<java.util.Map> m(java.util.Map p);
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun primitiveTypeStringIsJavaString() {
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
            public interface actor1 extends cloud.orbit.core.actor.ActorWithNoKey {
              java.util.concurrent.CompletableFuture<java.lang.String> m(java.lang.String p);
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun primitiveTypeVoidIsJavaVoid() {
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
            public interface actor1 extends cloud.orbit.core.actor.ActorWithNoKey {
              java.util.concurrent.CompletableFuture<java.lang.Void> m(java.lang.Void p);
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateEnum_OneMember() {
        val cu = CompilationUnit(
            packageName,
            enums = listOf(EnumDeclaration("enum1", listOf(EnumMember("member1", 3))))
        )

        val expectedSource = """
            public enum enum1 {
              member1
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateEnum_MultipleMember() {
        val cu = CompilationUnit(
            packageName,
            enums = listOf(
                EnumDeclaration(
                    "enum1", listOf(
                        EnumMember("member1", 3),
                        EnumMember("member2", 5)
                    )
                )
            )
        )

        val expectedSource = """
            public enum enum1 {
              member1,
              member2
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateData_NoFields() {
        val cu = CompilationUnit(packageName, data = listOf(DataDeclaration("data1")))

        val expectedSource = """
            public class data1 {
                public data1() {}
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateData_SingleField() {
        val cu = CompilationUnit(
            packageName, data = listOf(
                DataDeclaration("data1", listOf(DataField("Field1", TypeReference("string"), 3)))
            )
        )

        val expectedSource = """
            public class data1 {
                private final java.lang.String field1;

                public data1(java.lang.String field1) {
                    this.field1 = field1;
                }

                public java.lang.String getField1() { return field1; }
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateData_MultipleFields() {
        val cu = CompilationUnit(
            packageName, data = listOf(
                DataDeclaration(
                    "data1", listOf(
                        DataField("Field1", TypeReference("string"), 3),
                        DataField("Field2", TypeReference("int32"), 5)
                    )
                )
            )
        )

        val expectedSource = """
            public class data1 {
                private final java.lang.String field1;
                private final int field2;

                public data1(java.lang.String field1, int field2) {
                    this.field1 = field1;
                    this.field2 = field2;
                }

                public java.lang.String getField1() { return field1; }
                public int getField2() { return field2; }
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateData_GenericField_PrimitiveTypes() {
        val cu = CompilationUnit(
            packageName, data = listOf(
                DataDeclaration(
                    "data1", listOf(
                        DataField(
                            "field1", TypeReference(
                                "list", of = listOf(
                                    TypeReference(
                                        "map", of = listOf(
                                            TypeReference("string"),
                                            TypeReference("int32")
                                        )
                                    )
                                )
                            ),
                            3
                        )
                    )
                )
            )
        )

        val expectedSource = """
            public class data1 {
                private final java.util.List<java.util.Map<java.lang.String, java.lang.Integer>> field1;

                public data1(java.util.List<java.util.Map<java.lang.String, java.lang.Integer>> field1) {
                    this.field1 = field1;
                }

                public java.util.List<java.util.Map<java.lang.String, java.lang.Integer>> getField1() {
                    return field1;
                }
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateData_GenericField_CustomTypes() {
        val cu = CompilationUnit(
            packageName,
            enums = listOf(
                EnumDeclaration(
                    "enum1",
                    members = listOf(
                        EnumMember("A", 1),
                        EnumMember(
                            "B", 2
                        )
                    )
                )
            ),
            data = listOf(
                DataDeclaration(
                    "data1", fields = listOf(DataField("field1", TypeReference("string"), 1))
                ),
                DataDeclaration(
                    "data2", fields = listOf(
                        DataField("field1", TypeReference("list", of = listOf(TypeReference("enum1"))), 1),
                        DataField("field2", TypeReference("list", of = listOf(TypeReference("data1"))), 2)
                    )
                )
            )
        )

        val expectedSources = listOf(
            """
            public enum enum1 {
                A,
                B
            }
            """,
            """
            public class data1 {
                private final java.lang.String field1;

                public data1(java.lang.String field1) {
                    this.field1 = field1;
                }

                public java.lang.String getField1() {
                    return field1;
                }
            }
            """,
            """
            public class data2 {
                private final java.util.List<$packageName.enum1> field1;
                private final java.util.List<$packageName.data1> field2;

                public data2(java.util.List<$packageName.enum1> field1, java.util.List<$packageName.data1> field2) {
                    this.field1 = field1;
                    this.field2 = field2;
                }

                public java.util.List<$packageName.enum1> getField1() {
                    return field1;
                }

                public java.util.List<$packageName.data1> getField2() {
                    return field2;
                }
            }
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

        val expectedSource = "public interface actor1 extends cloud.orbit.core.actor.ActorWithNoKey { }"

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
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

        val expectedSource = "public interface actor1 extends cloud.orbit.core.actor.ActorWithNoKey { }"

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
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

        val expectedSource = "public interface actor1 extends cloud.orbit.core.actor.ActorWithStringKey { }"

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
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

        val expectedSource = "public interface actor1 extends cloud.orbit.core.actor.ActorWithInt32Key { }"

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
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

        val expectedSource = "public interface actor1 extends cloud.orbit.core.actor.ActorWithInt64Key { }"

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
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

        val expectedSource = "public interface actor1 extends cloud.orbit.core.actor.ActorWithGuidKey { }"

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateActor_SingleMethod() {
        val cu = CompilationUnit(
            packageName, actors = listOf(
                ActorDeclaration("actor1", methods = listOf(ActorMethod("method1", TypeReference("string"))))
            )
        )

        val expectedSource = """
            public interface actor1 extends cloud.orbit.core.actor.ActorWithNoKey  {
                java.util.concurrent.CompletableFuture<java.lang.String> method1();
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateActor_MultipleMethods() {
        val cu = CompilationUnit(
            packageName, actors = listOf(
                ActorDeclaration(
                    "actor1",
                    methods = listOf(
                        ActorMethod("method1", TypeReference("string")),
                        ActorMethod("method2", TypeReference("string"))
                    )
                )
            )
        )

        val expectedSource = """
            public interface actor1 extends cloud.orbit.core.actor.ActorWithNoKey  {
                java.util.concurrent.CompletableFuture<java.lang.String> method1();
                java.util.concurrent.CompletableFuture<java.lang.String> method2();
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
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
                            "method1",
                            TypeReference("string"),
                            params = listOf(MethodParameter("p1", TypeReference("int32")))
                        )
                    )
                )
            )
        )

        val expectedSource = """
            public interface actor1 extends cloud.orbit.core.actor.ActorWithNoKey  {
                java.util.concurrent.CompletableFuture<java.lang.String> method1(int p1);
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
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
                            "method1",
                            TypeReference("string"),
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
            public interface actor1 extends cloud.orbit.core.actor.ActorWithNoKey  {
                java.util.concurrent.CompletableFuture<java.lang.String> method1(int p1, int p2);
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateActor_PrimitivesAreBoxedInReturnType() {
        val cu = CompilationUnit(
            packageName, actors = listOf(
                ActorDeclaration(
                    "actor1",
                    methods = listOf(
                        ActorMethod(
                            "method1",
                            TypeReference("int32"),
                            params = listOf(MethodParameter("p1", TypeReference("int32")))
                        )
                    )
                )
            )
        )

        val expectedSource = """
            public interface actor1 extends cloud.orbit.core.actor.ActorWithNoKey  {
                java.util.concurrent.CompletableFuture<java.lang.Integer> method1(int p1);
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateActor_GenericReturnType_PrimitiveDataTypes() {
        val cu = CompilationUnit(
            packageName,
            actors = listOf(
                ActorDeclaration(
                    "actor1",
                    methods = listOf(
                        ActorMethod(
                            "method1",
                            TypeReference(
                                "map", of = listOf(
                                    TypeReference("string"),
                                    TypeReference(
                                        "list", of = listOf(
                                            TypeReference("list", of = listOf(TypeReference("int64")))
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
            public interface actor1 extends cloud.orbit.core.actor.ActorWithNoKey  {
                java.util.concurrent.CompletableFuture<
                    java.util.Map<java.lang.String, java.util.List<java.util.List<java.lang.Long>>>> method1();
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateActor_GenericReturnType_CustomDataTypes() {
        val cu = CompilationUnit(
            packageName,
            enums = listOf(
                EnumDeclaration(
                    "enum1",
                    members = listOf(
                        EnumMember("A", 1),
                        EnumMember("B", 2)
                    )
                )
            ),
            data = listOf(
                DataDeclaration(
                    "data1",
                    fields = listOf(
                        DataField(
                            "field1", TypeReference("string"), 1
                        )
                    )
                )
            ),
            actors = listOf(
                ActorDeclaration(
                    "actor1",
                    methods = listOf(
                        ActorMethod(
                            "method1",
                            TypeReference("map", of = listOf(TypeReference("enum1"), TypeReference("data1"))),
                            params = emptyList()
                        )
                    )
                )
            )
        )

        val expectedSources = listOf(
            """
            public enum enum1 {
                A,
                B
            }
            """,
            """
            public class data1 {
                private final java.lang.String field1;

                public data1(java.lang.String field1) {
                    this.field1 = field1;
                }

                public java.lang.String getField1() {
                    return field1;
                }
            }
            """,
            """
            public interface actor1 extends cloud.orbit.core.actor.ActorWithNoKey  {
                java.util.concurrent.CompletableFuture<java.util.Map<$packageName.enum1, $packageName.data1>> method1();
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
                            "method1",
                            TypeReference("int32"),
                            params = listOf(
                                MethodParameter(
                                    "arg1", type = TypeReference(
                                        "map", of = listOf(
                                            TypeReference("string"),
                                            TypeReference(
                                                "list", of = listOf(
                                                    TypeReference("list", of = listOf(TypeReference("int64")))
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
            public interface actor1 extends cloud.orbit.core.actor.ActorWithNoKey  {
                java.util.concurrent.CompletableFuture<java.lang.Integer> method1(java.util.Map<java.lang.String, java.util.List<java.util.List<java.lang.Long>>> arg1);
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateActor_GenericParameterType_CustomDataTypes() {
        val cu = CompilationUnit(
            packageName,
            enums = listOf(
                EnumDeclaration(
                    "enum1",
                    members = listOf(
                        EnumMember("A", 1),
                        EnumMember("B", 2)
                    )
                )
            ),
            data = listOf(
                DataDeclaration(
                    "data1",
                    fields = listOf(
                        DataField(
                            "field1", TypeReference("string"), 1
                        )
                    )
                )
            ),
            actors = listOf(
                ActorDeclaration(
                    "actor1",
                    methods = listOf(
                        ActorMethod(
                            "method1",
                            TypeReference("int32"),
                            params = listOf(
                                MethodParameter(
                                    "arg1", type = TypeReference(
                                        "map", of =
                                        listOf(TypeReference("enum1"), TypeReference("data1"))
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
            public enum enum1 {
                A,
                B
            }
            """,
            """
            public class data1 {
                private final java.lang.String field1;

                public data1(java.lang.String field1) {
                    this.field1 = field1;
                }

                public java.lang.String getField1() {
                    return field1;
                }
            }
            """,
            """
            public interface actor1 extends cloud.orbit.core.actor.ActorWithNoKey  {
                java.util.concurrent.CompletableFuture<java.lang.Integer> method1(java.util.Map<$packageName.enum1, $packageName.data1> arg1);
            }
            """
        )

        generateSource_minimalPipeline(cu).forEachIndexed { index, compiledType ->
            Assertions.assertEquals(packageName, compiledType.packageName)
            assertSourceMatch(expectedSources[index], compiledType.toString())
        }
    }

    private fun generateSource_minimalPipeline(vararg cus: CompilationUnit) = with(cus.toList()) {
        JavaCodeGenerator(
            TypeIndexer()
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
