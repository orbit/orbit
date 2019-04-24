/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OrbitDslFileParserTest {
    private val testPackageName = "cloud.orbit.test"
    private val testFilePath = "cloud/orbit/test/hello.orbit"

    @Test
    fun throwsOnSyntaxError() {
        val exception = assertThrows<OrbitDslParsingException> {
            OrbitDslFileParser().parse("actor a {", testPackageName, testFilePath)
        }

        assertEquals(1, exception.syntaxErrors.size)
    }

    @Test
    fun convertsUnsupportedActorKeyTypeExceptionToSyntaxError() {
        val exception = assertThrows<OrbitDslParsingException> {
            OrbitDslFileParser().parse("actor a<boolean> { }", testPackageName, testFilePath)
        }

        assertEquals(1, exception.syntaxErrors.size)
        assertEquals("cloud/orbit/test/hello.orbit", exception.syntaxErrors.first().filePath)
        assertEquals(1, exception.syntaxErrors.first().line)
        assertEquals(8, exception.syntaxErrors.first().column)
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

            actor MyKeylessActor { }

            actor MyStringKeyedActor<string> { }

            actor MyInt32KeyedActor<int32> { }

            actor MyInt64KeyedActor<int64> { }

            actor MyGuidKeyedActor<guid> { }
        """.trimIndent()

        val expectedCompilationUnit = CompilationUnit(
            testPackageName,
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
                    ActorKeyType.NO_KEY,
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
                ),
                ActorDeclaration(
                    "MyKeylessActor",
                    ActorKeyType.NO_KEY
                ),
                ActorDeclaration(
                    "MyStringKeyedActor",
                    ActorKeyType.STRING
                ),
                ActorDeclaration(
                    "MyInt32KeyedActor",
                    ActorKeyType.INT32
                ),
                ActorDeclaration(
                    "MyInt64KeyedActor",
                    ActorKeyType.INT64
                ),
                ActorDeclaration(
                    "MyGuidKeyedActor",
                    ActorKeyType.GUID
                )
            )
        )

        val actualCompilationUnit = OrbitDslFileParser().parse(text, testPackageName, testFilePath)

        assertEquals(expectedCompilationUnit, actualCompilationUnit)
    }
}
