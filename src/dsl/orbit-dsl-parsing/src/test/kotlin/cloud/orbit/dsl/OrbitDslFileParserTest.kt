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
import cloud.orbit.dsl.ast.ParseContext
import cloud.orbit.dsl.ast.Type
import cloud.orbit.dsl.ast.TypeOccurrenceContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OrbitDslFileParserTest {
    private val testPackageName = "cloud.orbit.test"
    private val testFilePath = "cloud/orbit/test/hello.orbit"

    @Test
    fun throwsOnSyntaxError() {
        val exception = assertThrows<OrbitDslParsingException> {
            OrbitDslFileParser().parse(
                listOf(
                    OrbitDslParseInput("actor a {", testPackageName, testFilePath)
                )
            )
        }

        assertEquals(1, exception.syntaxErrors.size)
    }

    @Test
    fun convertsUnsupportedActorKeyTypeExceptionToSyntaxError() {
        val exception = assertThrows<OrbitDslParsingException> {
            OrbitDslFileParser().parse(
                listOf(
                    OrbitDslParseInput("actor a<boolean> { }", testPackageName, testFilePath)
                )
            )
        }

        assertEquals(1, exception.syntaxErrors.size)
        assertEquals("cloud/orbit/test/hello.orbit", exception.syntaxErrors.first().filePath)
        assertEquals(1, exception.syntaxErrors.first().line)
        assertEquals(8, exception.syntaxErrors.first().column)
    }

    @Test
    fun parsesMultipleFiles() {
        val compilationUnits = OrbitDslFileParser().parse(
            listOf(
                OrbitDslParseInput("enum e {}", "package1", "/path/to/file1.orbit"),
                OrbitDslParseInput("data d {}", "package2", "/path/to/file2.orbit"),
                OrbitDslParseInput("actor a {}", "package3", "/path/to/file3.orbit")
            )
        )

        assertEquals(
            CompilationUnit(
                "package1", enums = listOf(EnumDeclaration("e"))
            ),
            compilationUnits[0]
        )
        assertEquals(
            CompilationUnit(
                "package2", data = listOf(DataDeclaration("d"))
            ),
            compilationUnits[1]
        )
        assertEquals(
            CompilationUnit(
                "package3", actors = listOf(ActorDeclaration("a"))
            ),
            compilationUnits[2]
        )
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

        val actualCompilationUnit = OrbitDslFileParser().parse(
            listOf(
                OrbitDslParseInput(text, testPackageName, testFilePath)
            )
        ).first()

        assertEquals(expectedCompilationUnit, actualCompilationUnit)
    }

    @Test
    fun astNodesAreAnnotatedWithParseContext() {
        val text = """
            enum
                AnEnum {
                    A_VALUE
                        = 1;
            }

             data
                SomeData {
                    string
                        a_field
                            = 1;
            }

            actor
                TheActor
                    <
                        string
                    >
            {
                map<
                    string,
                        list<
                            int32
                            >
                    >
                        a_method
                        (
                            int32
                                param
                        );
            }
        """.trimIndent()

        val compilationUnit = OrbitDslFileParser().parse(
            listOf(
                OrbitDslParseInput(text, testPackageName, testFilePath)
            )
        ).first()

        assertEquals(
            ParseContext(testFilePath, 2, 4),
            compilationUnit.enums[0].getAnnotation<ParseContext>()
        )
        assertEquals(
            ParseContext(testFilePath, 3, 8),
            compilationUnit.enums[0].members[0].getAnnotation<ParseContext>()
        )

        assertEquals(
            ParseContext(testFilePath, 8, 4),
            compilationUnit.data[0].getAnnotation<ParseContext>()
        )
        assertEquals(
            ParseContext(testFilePath, 9, 8),
            compilationUnit.data[0].fields[0].type.getAnnotation<ParseContext>()
        )
        assertEquals(
            ParseContext(testFilePath, 10, 12),
            compilationUnit.data[0].fields[0].getAnnotation<ParseContext>()
        )

        assertEquals(
            ParseContext(testFilePath, 15, 4),
            compilationUnit.actors[0].getAnnotation<ParseContext>()
        )
        assertEquals(
            ParseContext(testFilePath, 20, 4),
            compilationUnit.actors[0].methods[0].returnType.getAnnotation<ParseContext>()
        )
        assertEquals(
            ParseContext(testFilePath, 21, 8),
            compilationUnit.actors[0].methods[0].returnType.of[0].getAnnotation<ParseContext>()
        )
        assertEquals(
            ParseContext(testFilePath, 22, 12),
            compilationUnit.actors[0].methods[0].returnType.of[1].getAnnotation<ParseContext>()
        )
        assertEquals(
            ParseContext(testFilePath, 23, 16),
            compilationUnit.actors[0].methods[0].returnType.of[1].of[0].getAnnotation<ParseContext>()
        )
        assertEquals(
            ParseContext(testFilePath, 26, 12),
            compilationUnit.actors[0].methods[0].getAnnotation<ParseContext>()
        )
        assertEquals(
            ParseContext(testFilePath, 28, 16),
            compilationUnit.actors[0].methods[0].params[0].type.getAnnotation<ParseContext>()
        )
        assertEquals(
            ParseContext(testFilePath, 29, 20),
            compilationUnit.actors[0].methods[0].params[0].getAnnotation<ParseContext>()
        )
    }

    @Test
    fun typeNodesAreAnnotatedWithTypeOccurrenceContext() {
        val text = """
            data SomeData {
                string a_field = 1;
            }
             actor TheActor<string> {
                map<string, list<int32>> a_method(map<string, list<int32>> param);
            }
        """.trimIndent()

        val compilationUnit = OrbitDslFileParser().parse(
            listOf(
                OrbitDslParseInput(text, testPackageName, testFilePath)
            )
        ).first()

        assertEquals(
            TypeOccurrenceContext.DATA_FIELD,
            compilationUnit.data[0].fields[0].type.getAnnotation<TypeOccurrenceContext>()
        )
        assertEquals(
            TypeOccurrenceContext.METHOD_RETURN,
            compilationUnit.actors[0].methods[0].returnType.getAnnotation<TypeOccurrenceContext>()
        )
        assertEquals(
            TypeOccurrenceContext.TYPE_PARAMETER,
            compilationUnit.actors[0].methods[0].returnType.of[0].getAnnotation<TypeOccurrenceContext>()
        )
        assertEquals(
            TypeOccurrenceContext.TYPE_PARAMETER,
            compilationUnit.actors[0].methods[0].returnType.of[1].getAnnotation<TypeOccurrenceContext>()
        )
        assertEquals(
            TypeOccurrenceContext.TYPE_PARAMETER,
            compilationUnit.actors[0].methods[0].returnType.of[1].of[0].getAnnotation<TypeOccurrenceContext>()
        )
        assertEquals(
            TypeOccurrenceContext.METHOD_PARAMETER,
            compilationUnit.actors[0].methods[0].params[0].type.getAnnotation<TypeOccurrenceContext>()
        )
        assertEquals(
            TypeOccurrenceContext.TYPE_PARAMETER,
            compilationUnit.actors[0].methods[0].params[0].type.of[0].getAnnotation<TypeOccurrenceContext>()
        )
        assertEquals(
            TypeOccurrenceContext.TYPE_PARAMETER,
            compilationUnit.actors[0].methods[0].params[0].type.of[1].getAnnotation<TypeOccurrenceContext>()
        )
        assertEquals(
            TypeOccurrenceContext.TYPE_PARAMETER,
            compilationUnit.actors[0].methods[0].params[0].type.of[1].of[0].getAnnotation<TypeOccurrenceContext>()
        )
    }
}
