/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.java

import cloud.orbit.dsl.ast.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class JavaCodeGeneratorTest {
    private val PACKAGE_NAME = "cloud.orbit.test"

    @Test
    fun generateEnum_OneMember() {
        val cu = CompilationUnit(
            PACKAGE_NAME,
            enums = listOf(EnumDeclaration("enum1", listOf(EnumMember("member1", 3))))
        )

        val expectedSource = """
            public enum enum1 {
              member1
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(PACKAGE_NAME, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateEnum_MultipleMember() {
        val cu = CompilationUnit(
            PACKAGE_NAME,
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
            Assertions.assertEquals(PACKAGE_NAME, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateData_NoFields() {
        val cu = CompilationUnit(PACKAGE_NAME, data = listOf(DataDeclaration("data1")))

        val expectedSource = """
            public class data1 {
                public data1() {}
            }

        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(PACKAGE_NAME, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateData_SingleField() {
        val cu = CompilationUnit(
            PACKAGE_NAME, data = listOf(
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
            Assertions.assertEquals(PACKAGE_NAME, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateData_MultipleFields() {
        val cu = CompilationUnit(
            PACKAGE_NAME, data = listOf(
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
            Assertions.assertEquals(PACKAGE_NAME, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateGrain_Empty() {
        val cu = CompilationUnit(PACKAGE_NAME, grains = listOf(GrainDeclaration("grain1")))

        val expectedSource = "public interface grain1 extends cloud.orbit.actors.Actor { }"

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(PACKAGE_NAME, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateGrain_SingleMethod() {
        val cu = CompilationUnit(
            PACKAGE_NAME, grains = listOf(
                GrainDeclaration("grain1", listOf(GrainMethod("method1", Type("string"))))
            )
        )

        val expectedSource = """
            public interface grain1 extends cloud.orbit.actors.Actor  {
                cloud.orbit.concurrent.Task<java.lang.String> method1();
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(PACKAGE_NAME, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateGrain_MultipleMethods() {
        val cu = CompilationUnit(
            PACKAGE_NAME, grains = listOf(
                GrainDeclaration(
                    "grain1", listOf(
                        GrainMethod("method1", Type("string")),
                        GrainMethod("method2", Type("string"))
                    )
                )
            )
        )

        val expectedSource = """
            public interface grain1 extends cloud.orbit.actors.Actor  {
                cloud.orbit.concurrent.Task<java.lang.String> method1();
                cloud.orbit.concurrent.Task<java.lang.String> method2();
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(PACKAGE_NAME, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateGrain_SingleParamMethod() {
        val cu = CompilationUnit(
            PACKAGE_NAME, grains = listOf(
                GrainDeclaration(
                    "grain1", listOf(
                        GrainMethod(
                            "method1",
                            Type("string"),
                            params = listOf(MethodParameter("p1", Type("int32")))
                        )
                    )
                )
            )
        )

        val expectedSource = """
            public interface grain1 extends cloud.orbit.actors.Actor  {
                cloud.orbit.concurrent.Task<java.lang.String> method1(int p1);
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(PACKAGE_NAME, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateGrain_MultipleParamsMethod() {
        val cu = CompilationUnit(
            PACKAGE_NAME, grains = listOf(
                GrainDeclaration(
                    "grain1", listOf(
                        GrainMethod(
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
            public interface grain1 extends cloud.orbit.actors.Actor  {
                cloud.orbit.concurrent.Task<java.lang.String> method1(int p1, int p2);
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(PACKAGE_NAME, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
        }
    }

    @Test
    fun generateGrain_PrimitivesAreBoxedInReturnType() {
        val cu = CompilationUnit(
            PACKAGE_NAME, grains = listOf(
                GrainDeclaration(
                    "grain1", listOf(
                        GrainMethod(
                            "method1",
                            Type("int32"),
                            params = listOf(MethodParameter("p1", Type("int32")))
                        )
                    )
                )
            )
        )

        val expectedSource = """
            public interface grain1 extends cloud.orbit.actors.Actor  {
                cloud.orbit.concurrent.Task<java.lang.Integer> method1(int p1);
            }
        """

        assertOneElement(generateSource_minimalPipeline(cu)).run {
            Assertions.assertEquals(PACKAGE_NAME, this.packageName)
            assertSourceMatch(expectedSource, this.toString())
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
