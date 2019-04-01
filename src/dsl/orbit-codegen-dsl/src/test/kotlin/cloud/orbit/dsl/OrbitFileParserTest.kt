/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

import cloud.orbit.dsl.ast.ActorDeclaration
import cloud.orbit.dsl.ast.ActorMethod
import cloud.orbit.dsl.ast.CompilationUnit
import cloud.orbit.dsl.ast.DataDeclaration
import cloud.orbit.dsl.ast.DataField
import cloud.orbit.dsl.ast.EnumDeclaration
import cloud.orbit.dsl.ast.EnumMember
import cloud.orbit.dsl.ast.MethodParameter
import cloud.orbit.dsl.ast.Type
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OrbitFileParserTest {
    private val packageName = "cloud.orbit.test"

    @Test
    fun throwsOnSyntaxError() {
        val text = "actor a {"

        assertThrows<ParseCancellationException> {
            OrbitFileParser().parse(text, packageName)
        }
    }

    @Test
    fun parseEnum_Empty() {
        val text = "enum foo{}"

        val expectedCu = CompilationUnit(
            packageName,
            enums = listOf(EnumDeclaration("foo"))
        )

        val actualCu = OrbitFileParser().parse(text, packageName)

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
            packageName,
            enums = listOf(
                EnumDeclaration(
                    "foo",
                    members = listOf(EnumMember("val1", 3))
                )
            )
        )

        val actualCu = OrbitFileParser().parse(text, packageName)

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
            packageName,
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

        val actualCu = OrbitFileParser().parse(text, packageName)

        Assertions.assertEquals(expectedCu, actualCu)
    }

    @Test
    fun parseData_Empty() {
        val text = "data foo{}"

        val expectedCu = CompilationUnit(
            packageName,
            data = listOf(DataDeclaration("foo"))
        )

        val actualCu = OrbitFileParser().parse(text, packageName)

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
            packageName,
            data = listOf(
                DataDeclaration(
                    "foo",
                    fields = listOf(
                        DataField("f1", Type("int"), 3)
                    )
                )
            )
        )

        val actualCu = OrbitFileParser().parse(text, packageName)

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
            packageName,
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

        val actualCu = OrbitFileParser().parse(text, packageName)

        Assertions.assertEquals(expectedCu, actualCu)
    }

    @Test
    fun parseData_SimpleGenericField_SingleType() {
        val text = """
            data foo {
                list<int32> f1 = 3;
            }
        """.trimIndent()

        val expectedCu = CompilationUnit(
            packageName,
            data = listOf(
                DataDeclaration(
                    "foo",
                    fields = listOf(
                        DataField("f1", Type("list", listOf(Type("int32"))), 3)
                    )
                )
            )
        )

        val actualCu = OrbitFileParser().parse(text, packageName)

        Assertions.assertEquals(expectedCu, actualCu)
    }

    @Test
    fun parseData_ComplexGenericField_SingleType() {
        val text = """
            data foo {
                list<list<string>> f1 = 3;
            }
        """.trimIndent()

        val expectedCu = CompilationUnit(
            packageName,
            data = listOf(
                DataDeclaration(
                    "foo",
                    fields = listOf(
                        DataField(
                            "f1",
                            Type(
                                "list", listOf(
                                    Type(
                                        "list", listOf(
                                            Type("string")
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

        val actualCu = OrbitFileParser().parse(text, packageName)

        Assertions.assertEquals(expectedCu, actualCu)
    }

    @Test
    fun parseData_SimpleGenericField_MultipleTypes() {
        val text = """
            data foo {
                map<string, int32> f1 = 3;
            }
        """.trimIndent()

        val expectedCu = CompilationUnit(
            packageName,
            data = listOf(
                DataDeclaration(
                    "foo",
                    fields = listOf(
                        DataField(
                            "f1",
                            Type(
                                "map", listOf(
                                    Type("string"), Type("int32")
                                )
                            ),
                            3
                        )
                    )
                )
            )
        )

        val actualCu = OrbitFileParser().parse(text, packageName)

        Assertions.assertEquals(expectedCu, actualCu)
    }

    @Test
    fun parseData_ComplexGenericField_MultipleTypes() {
        val text = """
            data foo {
                map<string, list<int32>> f1 = 3;
            }
        """.trimIndent()

        val expectedCu = CompilationUnit(
            packageName,
            data = listOf(
                DataDeclaration(
                    "foo",
                    fields = listOf(
                        DataField(
                            "f1",
                            Type(
                                "map", listOf(
                                    Type("string"), Type("list", listOf(Type("int32")))
                                )
                            ),
                            3
                        )
                    )
                )
            )
        )

        val actualCu = OrbitFileParser().parse(text, packageName)

        Assertions.assertEquals(expectedCu, actualCu)
    }

    @Test
    fun parseActor_Empty() {
        val text = "actor foo{}"

        val expectedCu = CompilationUnit(
            packageName,
            actors = listOf(ActorDeclaration("foo"))
        )

        val actualCu = OrbitFileParser().parse(text, packageName)

        Assertions.assertEquals(expectedCu, actualCu)
    }

    @Test
    fun parseActor_SingleMethod() {
        val text = """
            actor foo {
                void bar();
            }
        """.trimIndent()

        val expectedCu = CompilationUnit(
            packageName,
            actors = listOf(
                ActorDeclaration(
                    "foo",
                    methods = listOf(ActorMethod("bar", Type("void")))
                )
            )
        )

        val actualCu = OrbitFileParser().parse(text, packageName)

        Assertions.assertEquals(expectedCu, actualCu)
    }

    @Test
    fun parseActor_MultipleMethods() {
        val text = """
            actor foo {
                void bar();
                void baz();
            }
        """.trimIndent()

        val expectedCu = CompilationUnit(
            packageName,
            actors = listOf(
                ActorDeclaration(
                    "foo",
                    methods = listOf(
                        ActorMethod("bar", Type("void")),
                        ActorMethod("baz", Type("void"))
                    )
                )
            )
        )

        val actualCu = OrbitFileParser().parse(text, packageName)

        Assertions.assertEquals(expectedCu, actualCu)
    }

    @Test
    fun parseActor_MethodWithOneParam() {
        val text = """
            actor foo {
                void bar(int k);
            }
        """.trimIndent()

        val expectedCu = CompilationUnit(
            packageName,
            actors = listOf(
                ActorDeclaration(
                    "foo",
                    methods = listOf(
                        ActorMethod(
                            "bar", Type("void"),
                            params = listOf(MethodParameter("k", Type("int")))
                        )
                    )
                )
            )
        )

        val actualCu = OrbitFileParser().parse(text, packageName)

        Assertions.assertEquals(expectedCu, actualCu)
    }

    @Test
    fun parseActor_MethodWithMultipleParams() {
        val text = """
            actor foo {
                void bar(int arg1, RGB arg2);
            }
        """.trimIndent()

        val expectedCu = CompilationUnit(
            packageName,
            actors = listOf(
                ActorDeclaration(
                    "foo",
                    methods = listOf(
                        ActorMethod(
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

        val actualCu = OrbitFileParser().parse(text, packageName)

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
            actor MyActor{
                // Comment
                int no_args();
                void one_arg(RGB a); // Comment
                RGB multiple_args(RGB arg1, RGB arg2);
                // RGB multiple_args(RGB arg1, RGB arg2); This should be ignored
                list<string> generic_return();
                void generic_arg(list<int> arg1);
                map<RGB, string> generic_multi(map<string, Payload> arg1);
                list<map<string, RGB>> generics_nested(map<string, list<list<Payload>>> arg1);
            }
        """.trimIndent()

        val expectedCu = CompilationUnit(
            packageName,
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
            actors = listOf(
                ActorDeclaration(
                    "MyActor",
                    methods = listOf(
                        ActorMethod("no_args", Type("int")),
                        ActorMethod(
                            "one_arg",
                            Type("void"),
                            params = listOf(
                                MethodParameter("a", Type("RGB"))
                            )
                        ),
                        ActorMethod(
                            "multiple_args",
                            Type("RGB"),
                            params = listOf(
                                MethodParameter("arg1", Type("RGB")),
                                MethodParameter("arg2", Type("RGB"))
                            )
                        ),
                        ActorMethod("generic_return", Type("list", of = listOf(Type("string")))),
                        ActorMethod(
                            "generic_arg",
                            Type("void"),
                            params = listOf(
                                MethodParameter("arg1", Type("list", of = listOf(Type("int"))))
                            )
                        ),
                        ActorMethod(
                            "generic_multi",
                            Type("map", of = listOf(Type("RGB"), Type("string"))),
                            params = listOf(
                                MethodParameter(
                                    "arg1",
                                    Type("map", of = listOf(Type("string"), Type("Payload")))
                                )
                            )
                        ),
                        ActorMethod(
                            "generics_nested",
                            Type(
                                "list", of = listOf(
                                    Type(
                                        "map", of = listOf(
                                            Type("string"),
                                            Type("RGB")
                                        )
                                    )
                                )
                            ),
                            params = listOf(
                                MethodParameter(
                                    "arg1",
                                    Type(
                                        "map", of = listOf(
                                            Type("string"),
                                            Type(
                                                "list", of = listOf(
                                                    Type(
                                                        "list", of = listOf(
                                                            Type("Payload")
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
        )

        val actualCu = OrbitFileParser().parse(text, packageName)

        Assertions.assertEquals(expectedCu, actualCu)
    }
}
