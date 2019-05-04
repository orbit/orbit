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
import cloud.orbit.dsl.error.OrbitDslCompilationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OrbitDslFileParserTest {
    private val testPackageName = "cloud.orbit.test"
    private val testFilePath = "cloud/orbit/test/hello.orbit"

    @Test
    fun throwsOnSyntaxError() {
        val exception = assertThrows<OrbitDslCompilationException> {
            OrbitDslFileParser().parse(
                listOf(
                    OrbitDslParseInput("actor a {", testPackageName, testFilePath)
                )
            )
        }

        assertEquals(1, exception.errors.size)
    }

    @Test
    fun convertsUnsupportedActorKeyTypeExceptionToSyntaxError() {
        val exception = assertThrows<OrbitDslCompilationException> {
            OrbitDslFileParser().parse(
                listOf(
                    OrbitDslParseInput("actor a<boolean> { }", testPackageName, testFilePath)
                )
            )
        }

        assertEquals(1, exception.errors.size)
        assertEquals("cloud/orbit/test/hello.orbit", exception.errors.first().filePath)
        assertEquals(1, exception.errors.first().line)
        assertEquals(8, exception.errors.first().column)
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
                "package1", enums = listOf(
                    EnumDeclaration(
                        parseContext = ParseContext(
                            "/path/to/file1.orbit",
                            line = 1,
                            column = 5
                        ),
                        name = "e"
                    )
                )
            ),
            compilationUnits[0]
        )

        assertEquals(
            CompilationUnit(
                "package2", data = listOf(
                    DataDeclaration(
                        parseContext = ParseContext(
                            "/path/to/file2.orbit",
                            line = 1,
                            column = 5
                        ),
                        name = "d"
                    )
                )
            ),
            compilationUnits[1]
        )

        assertEquals(
            CompilationUnit(
                "package3", actors = listOf(
                    ActorDeclaration(
                        parseContext = ParseContext(
                            "/path/to/file3.orbit",
                            line = 1,
                            column = 6
                        ),
                        name = "a"
                    )
                )
            ),
            compilationUnits[2]
        )
    }

    @Test
    fun parsesAllConcepts() {
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
                    parseContext = ParseContext(
                        filePath = testFilePath,
                        line = 2,
                        column = 5
                    ),
                    name = "RGB",
                    members = listOf(
                        EnumMember(
                            parseContext = ParseContext(
                                filePath = testFilePath,
                                line = 4,
                                column = 4
                            ),
                            name = "R",
                            index = 1
                        ),
                        EnumMember(
                            parseContext = ParseContext(
                                filePath = testFilePath,
                                line = 5,
                                column = 4
                            ),
                            name = "G",
                            index = 2
                        ),
                        EnumMember(
                            parseContext = ParseContext(
                                filePath = testFilePath,
                                line = 6,
                                column = 4
                            ),
                            name = "B",
                            index = 3
                        )
                    )
                )
            ),
            data = listOf(
                DataDeclaration(
                    parseContext = ParseContext(
                        filePath = testFilePath,
                        line = 9,
                        column = 5
                    ),
                    name = "Payload",
                    fields = listOf(
                        DataField(
                            parseContext = ParseContext(
                                filePath = testFilePath,
                                line = 10,
                                column = 8
                            ),
                            name = "field1",
                            type = Type(
                                parseContext = ParseContext(
                                    filePath = testFilePath,
                                    line = 10,
                                    column = 4
                                ),
                                name = "int"
                            ),
                            index = 1
                        ),
                        DataField(
                            parseContext = ParseContext(
                                filePath = testFilePath,
                                line = 12,
                                column = 8
                            ),
                            name = "field2",
                            type = Type(
                                parseContext = ParseContext(
                                    filePath = testFilePath,
                                    line = 12,
                                    column = 4
                                ),
                                name = "RGB"
                            ),
                            index = 2
                        )
                    )
                )
            ),
            actors = listOf(
                ActorDeclaration(
                    parseContext = ParseContext(
                        filePath = testFilePath,
                        line = 19,
                        column = 6
                    ),
                    name = "MyActor",
                    keyType = ActorKeyType.NO_KEY,
                    methods = listOf(
                        ActorMethod(
                            parseContext = ParseContext(
                                filePath = testFilePath,
                                line = 21,
                                column = 8
                            ),
                            name = "no_args",
                            returnType = Type(
                                parseContext = ParseContext(
                                    filePath = testFilePath,
                                    line = 21,
                                    column = 4
                                ),
                                name = "int"
                            )
                        ),
                        ActorMethod(
                            parseContext = ParseContext(
                                filePath = testFilePath,
                                line = 22,
                                column = 9
                            ),
                            name = "one_arg",
                            returnType = Type(
                                parseContext = ParseContext(
                                    filePath = testFilePath,
                                    line = 22,
                                    column = 4
                                ),
                                name = "void"
                            ),
                            params = listOf(
                                MethodParameter(
                                    parseContext = ParseContext(
                                        filePath = testFilePath,
                                        line = 22,
                                        column = 21
                                    ),
                                    name = "a",
                                    type = Type(
                                        parseContext = ParseContext(
                                            filePath = testFilePath,
                                            line = 22,
                                            column = 17
                                        ),
                                        name = "RGB"
                                    )
                                )
                            )
                        ),
                        ActorMethod(
                            parseContext = ParseContext(
                                filePath = testFilePath,
                                line = 23,
                                column = 8
                            ),
                            name = "multiple_args",
                            returnType = Type(
                                parseContext = ParseContext(
                                    filePath = testFilePath,
                                    line = 23,
                                    column = 4
                                ),
                                name = "RGB"
                            ),
                            params = listOf(
                                MethodParameter(
                                    parseContext = ParseContext(
                                        filePath = testFilePath,
                                        line = 23,
                                        column = 26
                                    ),
                                    name = "arg1",
                                    type = Type(
                                        parseContext = ParseContext(
                                            filePath = testFilePath,
                                            line = 23,
                                            column = 22
                                        ),
                                        name = "RGB"
                                    )
                                ),
                                MethodParameter(
                                    parseContext = ParseContext(
                                        filePath = testFilePath,
                                        line = 23,
                                        column = 36
                                    ),
                                    name = "arg2",
                                    type = Type(
                                        parseContext = ParseContext(
                                            filePath = testFilePath,
                                            line = 23,
                                            column = 32
                                        ),
                                        name = "RGB"
                                    )
                                )
                            )
                        ),
                        ActorMethod(
                            parseContext = ParseContext(
                                filePath = testFilePath,
                                line = 25,
                                column = 17
                            ),
                            name = "generic_return",
                            returnType = Type(
                                parseContext = ParseContext(
                                    filePath = testFilePath,
                                    line = 25,
                                    column = 4
                                ),
                                name = "list",
                                of = listOf(
                                    Type(
                                        parseContext = ParseContext(
                                            filePath = testFilePath,
                                            line = 25,
                                            column = 9
                                        ),
                                        name = "string"
                                    )
                                )
                            )
                        ),
                        ActorMethod(
                            parseContext = ParseContext(
                                filePath = testFilePath,
                                line = 26,
                                column = 9
                            ),
                            name = "generic_arg",
                            returnType = Type(
                                parseContext = ParseContext(
                                    filePath = testFilePath,
                                    line = 26,
                                    column = 4
                                ),
                                name = "void"
                            ),
                            params = listOf(
                                MethodParameter(
                                    parseContext = ParseContext(
                                        filePath = testFilePath,
                                        line = 26,
                                        column = 31
                                    ),
                                    name = "arg1",
                                    type = Type(
                                        parseContext = ParseContext(
                                            filePath = testFilePath,
                                            line = 26,
                                            column = 21
                                        ),
                                        name = "list", of = listOf(
                                            Type(
                                                parseContext = ParseContext(
                                                    filePath = testFilePath,
                                                    line = 26,
                                                    column = 26
                                                ),
                                                name = "int"
                                            )
                                        )
                                    )
                                )
                            )
                        ),
                        ActorMethod(
                            parseContext = ParseContext(
                                filePath = testFilePath,
                                line = 27,
                                column = 21
                            ),
                            name = "generic_multi",
                            returnType = Type(
                                parseContext = ParseContext(
                                    filePath = testFilePath,
                                    line = 27,
                                    column = 4
                                ),
                                name = "map",
                                of = listOf(
                                    Type(
                                        parseContext = ParseContext(
                                            filePath = testFilePath,
                                            line = 27,
                                            column = 8
                                        ),
                                        name = "RGB"
                                    ),
                                    Type(
                                        parseContext = ParseContext(
                                            filePath = testFilePath,
                                            line = 27,
                                            column = 13
                                        ),
                                        name = "string"
                                    )
                                )
                            ),
                            params = listOf(
                                MethodParameter(
                                    parseContext = ParseContext(
                                        filePath = testFilePath,
                                        line = 27,
                                        column = 56
                                    ),
                                    name = "arg1",
                                    type = Type(
                                        parseContext = ParseContext(
                                            filePath = testFilePath,
                                            line = 27,
                                            column = 35
                                        ),
                                        name = "map",
                                        of = listOf(
                                            Type(
                                                parseContext = ParseContext(
                                                    filePath = testFilePath,
                                                    line = 27,
                                                    column = 39
                                                ),
                                                name = "string"
                                            ),
                                            Type(
                                                parseContext = ParseContext(
                                                    filePath = testFilePath,
                                                    line = 27,
                                                    column = 47
                                                ),
                                                name = "Payload"
                                            )
                                        )
                                    )
                                )
                            )
                        ),
                        ActorMethod(
                            parseContext = ParseContext(
                                filePath = testFilePath,
                                line = 28,
                                column = 27
                            ),
                            name = "generics_nested",
                            returnType = Type(
                                parseContext = ParseContext(
                                    filePath = testFilePath,
                                    line = 28,
                                    column = 4
                                ),
                                name = "list",
                                of = listOf(
                                    Type(
                                        parseContext = ParseContext(
                                            filePath = testFilePath,
                                            line = 28,
                                            column = 9
                                        ),
                                        name = "map",
                                        of = listOf(
                                            Type(
                                                parseContext = ParseContext(
                                                    filePath = testFilePath,
                                                    line = 28,
                                                    column = 13
                                                ),
                                                name = "string"
                                            ),
                                            Type(
                                                parseContext = ParseContext(
                                                    filePath = testFilePath,
                                                    line = 28,
                                                    column = 21
                                                ),
                                                name = "RGB"
                                            )
                                        )
                                    )
                                )
                            ),
                            params = listOf(
                                MethodParameter(
                                    parseContext = ParseContext(
                                        filePath = testFilePath,
                                        line = 28,
                                        column = 76
                                    ),
                                    name = "arg1",
                                    type = Type(
                                        parseContext = ParseContext(
                                            filePath = testFilePath,
                                            line = 28,
                                            column = 43
                                        ),
                                        name = "map",
                                        of = listOf(
                                            Type(
                                                parseContext = ParseContext(
                                                    filePath = testFilePath,
                                                    line = 28,
                                                    column = 47
                                                ),
                                                name = "string"
                                            ),
                                            Type(
                                                parseContext = ParseContext(
                                                    filePath = testFilePath,
                                                    line = 28,
                                                    column = 55
                                                ),
                                                name = "list",
                                                of = listOf(
                                                    Type(
                                                        parseContext = ParseContext(
                                                            filePath = testFilePath,
                                                            line = 28,
                                                            column = 60
                                                        ),
                                                        name = "list",
                                                        of = listOf(
                                                            Type(
                                                                parseContext = ParseContext(
                                                                    filePath = testFilePath,
                                                                    line = 28,
                                                                    column = 65
                                                                ),
                                                                name = "Payload"
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
                ),
                ActorDeclaration(
                    parseContext = ParseContext(
                        filePath = testFilePath,
                        line = 31,
                        column = 6
                    ),
                    name = "MyKeylessActor",
                    keyType = ActorKeyType.NO_KEY
                ),
                ActorDeclaration(
                    parseContext = ParseContext(
                        filePath = testFilePath,
                        line = 33,
                        column = 6
                    ),
                    name = "MyStringKeyedActor",
                    keyType = ActorKeyType.STRING
                ),
                ActorDeclaration(
                    parseContext = ParseContext(
                        filePath = testFilePath,
                        line = 35,
                        column = 6
                    ),
                    name = "MyInt32KeyedActor",
                    keyType = ActorKeyType.INT32
                ),
                ActorDeclaration(
                    parseContext = ParseContext(
                        filePath = testFilePath,
                        line = 37,
                        column = 6
                    ),
                    name = "MyInt64KeyedActor",
                    keyType = ActorKeyType.INT64
                ),
                ActorDeclaration(
                    parseContext = ParseContext(
                        filePath = testFilePath,
                        line = 39,
                        column = 6
                    ),
                    name = "MyGuidKeyedActor",
                    keyType = ActorKeyType.GUID
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
}
