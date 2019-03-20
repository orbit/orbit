/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

import cloud.orbit.dsl.ast.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class OrbitFileParserTest {
    private val FAKE_PACKAGE_NAME = "cloud.orbit.test.some.package"

    @Test
    fun parseEnum_Empty() {
        val text = "enum foo{}"

        val expectedCu = CompilationUnit(
            FAKE_PACKAGE_NAME,
            enums = listOf(EnumDeclaration("foo"))
        )

        val actualCu = OrbitFileParser().parse(text, FAKE_PACKAGE_NAME)

        Assertions.assertEquals(expectedCu, actualCu)
    }

    @Test
    fun parseEnum_SingleMember() {
        val text = """
            enum foo {
                val1 = 3;
            }
        """.trimIndent()

        val expectedCu = CompilationUnit(
            FAKE_PACKAGE_NAME,
            enums = listOf(
                EnumDeclaration(
                    "foo",
                    members = listOf(EnumMember("val1", 3))
                )
            )
        )

        val actualCu = OrbitFileParser().parse(text, FAKE_PACKAGE_NAME)

        Assertions.assertEquals(expectedCu, actualCu)
    }

    @Test
    fun parseEnum_MultipleMembers() {
        val text = """
            enum foo {
                val1 = 3;
                val2 = 5;
            }
        """.trimIndent()

        val expectedCu = CompilationUnit(
            FAKE_PACKAGE_NAME,
            enums = listOf(
                EnumDeclaration(
                    "foo",
                    members = listOf(
                        EnumMember("val1", 3),
                        EnumMember("val2", 5)
                    )
                )
            )
        )

        val actualCu = OrbitFileParser().parse(text, FAKE_PACKAGE_NAME)

        Assertions.assertEquals(expectedCu, actualCu)
    }

    @Test
    fun parseData_Empty() {
        val text = "data foo{}"

        val expectedCu = CompilationUnit(
            FAKE_PACKAGE_NAME,
            data = listOf(DataDeclaration("foo"))
        )

        val actualCu = OrbitFileParser().parse(text, FAKE_PACKAGE_NAME)

        Assertions.assertEquals(expectedCu, actualCu)
    }

    @Test
    fun parseData_SingleField() {
        val text = """
            data foo {
                int f1 = 3;
            }
        """.trimIndent()

        val expectedCu = CompilationUnit(
            FAKE_PACKAGE_NAME,
            data = listOf(
                DataDeclaration(
                    "foo",
                    fields = listOf(
                        DataField("f1", Type("int"), 3)
                    )
                )
            )
        )

        val actualCu = OrbitFileParser().parse(text, FAKE_PACKAGE_NAME)

        Assertions.assertEquals(expectedCu, actualCu)
    }

    @Test
    fun parseData_MultipleFields() {
        val text = """
            data foo {
                int f1 = 3;
                string f2 = 5;
            }
        """.trimIndent()

        val expectedCu = CompilationUnit(
            FAKE_PACKAGE_NAME,
            data = listOf(
                DataDeclaration(
                    "foo",
                    fields = listOf(
                        DataField("f1", Type("int"), 3),
                        DataField("f2", Type("string"), 5)
                    )
                )
            )
        )

        val actualCu = OrbitFileParser().parse(text, FAKE_PACKAGE_NAME)

        Assertions.assertEquals(expectedCu, actualCu)
    }

    @Test
    fun parseGrain_Empty() {
        val text = "grain foo{}"

        val expectedCu = CompilationUnit(
            FAKE_PACKAGE_NAME,
            grains = listOf(GrainDeclaration("foo"))
        )

        val actualCu = OrbitFileParser().parse(text, FAKE_PACKAGE_NAME)

        Assertions.assertEquals(expectedCu, actualCu)
    }

    @Test
    fun parseGrain_SingleMethod() {
        val text = """
            grain foo {
                void bar();
            }
        """.trimIndent()

        val expectedCu = CompilationUnit(
            FAKE_PACKAGE_NAME,
            grains = listOf(
                GrainDeclaration(
                    "foo",
                    methods = listOf(GrainMethod("bar", Type("void")))
                )
            )
        )

        val actualCu = OrbitFileParser().parse(text, FAKE_PACKAGE_NAME)

        Assertions.assertEquals(expectedCu, actualCu)
    }

    @Test
    fun parseGrain_MultipleMethods() {
        val text = """
            grain foo {
                void bar();
                void baz();
            }
        """.trimIndent()

        val expectedCu = CompilationUnit(
            FAKE_PACKAGE_NAME,
            grains = listOf(
                GrainDeclaration(
                    "foo",
                    methods = listOf(
                        GrainMethod("bar", Type("void")),
                        GrainMethod("baz", Type("void"))
                    )
                )
            )
        )

        val actualCu = OrbitFileParser().parse(text, FAKE_PACKAGE_NAME)

        Assertions.assertEquals(expectedCu, actualCu)
    }

    @Test
    fun parseGrain_MethodWithOneParam() {
        val text = """
            grain foo {
                void bar(int k);
            }
        """.trimIndent()

        val expectedCu = CompilationUnit(
            FAKE_PACKAGE_NAME,
            grains = listOf(
                GrainDeclaration(
                    "foo",
                    methods = listOf(
                        GrainMethod(
                            "bar", Type("void"),
                            params = listOf(MethodParameter("k", Type("int")))
                        )
                    )
                )
            )
        )

        val actualCu = OrbitFileParser().parse(text, FAKE_PACKAGE_NAME)

        Assertions.assertEquals(expectedCu, actualCu)
    }

    @Test
    fun parseGrain_MethodWithMultipleParams() {
        val text = """
            grain foo {
                void bar(int arg1, RGB arg2);
            }
        """.trimIndent()

        val expectedCu = CompilationUnit(
            FAKE_PACKAGE_NAME,
            grains = listOf(
                GrainDeclaration(
                    "foo",
                    methods = listOf(
                        GrainMethod(
                            "bar", Type("void"),
                            params = listOf(
                                MethodParameter("arg1", Type("int")),
                                MethodParameter("arg2", Type("RGB"))
                            )
                        )
                    )
                )
            )
        )

        val actualCu = OrbitFileParser().parse(text, FAKE_PACKAGE_NAME)

        Assertions.assertEquals(expectedCu, actualCu)
    }

    @Test
    fun parseFile_allConcepts() {
        val text = """
            // Comments
            enum RGB {
                // Comment
                R = 1;
                G = 2; // Inline comment X = 3
                B = 3;
            }

            data Payload {
                int field1 = 1;
                // Comment
                RGB field2 = 2; // Comment
            } // Comment

            // Lots of comments //
            //Lots of comments

            // Comment
            grain MyGrain{
                // Comment
                int no_args();
                void one_arg(RGB a); // Comment
                RGB multiple_args(RGB arg1, RGB arg2);
                // RGB multiple_args(RGB arg1, RGB arg2); This should be ignored
            }
        """.trimIndent()

        val expectedCu = CompilationUnit(
            FAKE_PACKAGE_NAME,
            enums = listOf(
                EnumDeclaration(
                    "RGB",
                    members = listOf(
                        EnumMember("R", 1),
                        EnumMember("G", 2),
                        EnumMember("B", 3)
                    )
                )
            ),

            data = listOf(
                DataDeclaration(
                    "Payload",
                    fields = listOf(
                        DataField("field1", Type("int"), 1),
                        DataField("field2", Type("RGB"), 2)
                    )
                )
            ),
            grains = listOf(
                GrainDeclaration(
                    "MyGrain",
                    methods = listOf(
                        GrainMethod("no_args", Type("int")),
                        GrainMethod(
                            "one_arg", Type("void"),
                            params = listOf(
                                MethodParameter("a", Type("RGB"))
                            )
                        ),
                        GrainMethod(
                            "multiple_args", Type("RGB"),
                            params = listOf(
                                MethodParameter("arg1", Type("RGB")),
                                MethodParameter("arg2", Type("RGB"))
                            )
                        )
                    )
                )
            )
        )

        val actualCu = OrbitFileParser().parse(text, FAKE_PACKAGE_NAME)

        Assertions.assertEquals(expectedCu, actualCu)
    }
}
