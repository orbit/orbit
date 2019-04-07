/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.java

import cloud.orbit.dsl.ast.ActorDeclaration
import cloud.orbit.dsl.ast.ActorKeyType
import cloud.orbit.dsl.ast.ActorMethod
import cloud.orbit.dsl.ast.CompilationUnit
import cloud.orbit.dsl.ast.DataDeclaration
import cloud.orbit.dsl.ast.DataField
import cloud.orbit.dsl.ast.EnumDeclaration
import cloud.orbit.dsl.ast.EnumMember
import cloud.orbit.dsl.ast.MethodParameter
import cloud.orbit.dsl.ast.Type
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class JavaCodeGeneratorTest {
    private val packageName = "cloud.orbit.test"

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
                DataDeclaration("data1", listOf(DataField("Field1", Type("string"), 3)))
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
                        DataField("Field1", Type("string"), 3),
                        DataField("Field2", Type("int32"), 5)
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
                            "field1", Type(
                                "list", of = listOf(
                                    Type(
                                        "map", of = listOf(
                                            Type("string"),
                                            Type("int32")
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
                    "data1", fields = listOf(DataField("field1", Type("string"), 1))
                ),
                DataDeclaration(
                    "data2", fields = listOf(
                        DataField("field1", Type("list", of = listOf(Type("enum1"))), 1),
                        DataField("field2", Type("list", of = listOf(Type("data1"))), 2)
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
    fun generatorActor_NoKeyType() {
        val cu = CompilationUnit(packageName, actors = listOf(ActorDeclaration("actor1")))

        val expectedSource = "public interface actor1 extends cloud.orbit.core.actor.ActorWithNoKey { }"

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generatorActor_StringKey() {
        val cu = CompilationUnit(packageName, actors = listOf(ActorDeclaration("actor1", ActorKeyType.STRING)))

        val expectedSource = "public interface actor1 extends cloud.orbit.core.actor.ActorWithStringKey { }"

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generatorActor_Int32Key() {
        val cu = CompilationUnit(packageName, actors = listOf(ActorDeclaration("actor1", ActorKeyType.INT32)))

        val expectedSource = "public interface actor1 extends cloud.orbit.core.actor.ActorWithInt32Key { }"

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generatorActor_Int64Key() {
        val cu = CompilationUnit(packageName, actors = listOf(ActorDeclaration("actor1", ActorKeyType.INT64)))

        val expectedSource = "public interface actor1 extends cloud.orbit.core.actor.ActorWithInt64Key { }"

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(this@JavaCodeGeneratorTest.packageName, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generatorActor_GuidKey() {
        val cu = CompilationUnit(packageName, actors = listOf(ActorDeclaration("actor1", ActorKeyType.GUID)))

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
                ActorDeclaration("actor1", methods = listOf(ActorMethod("method1", Type("string"))))
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
                        ActorMethod("method1", Type("string")),
                        ActorMethod("method2", Type("string"))
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
                            Type("string"),
                            params = listOf(MethodParameter("p1", Type("int32")))
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
                            Type("string"),
                            params = listOf(
                                MethodParameter("p1", Type("int32")),
                                MethodParameter("p2", Type("int32"))
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
                            Type("int32"),
                            params = listOf(MethodParameter("p1", Type("int32")))
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
                            Type(
                                "map", of = listOf(
                                    Type("string"),
                                    Type(
                                        "list", of = listOf(
                                            Type("list", of = listOf(Type("int64")))
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
                            "field1", Type("string"), 1
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
                            Type("map", of = listOf(Type("enum1"), Type("data1"))),
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
                            Type("int32"),
                            params = listOf(
                                MethodParameter(
                                    "arg1", type = Type(
                                        "map", of = listOf(
                                            Type("string"),
                                            Type(
                                                "list", of = listOf(
                                                    Type("list", of = listOf(Type("int64")))
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
                            "field1", Type("string"), 1
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
                            Type("int32"),
                            params = listOf(
                                MethodParameter(
                                    "arg1", type = Type(
                                        "map", of =
                                        listOf(Type("enum1"), Type("data1"))
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
