/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.parsing

import cloud.orbit.dsl.ast.ActorDeclaration
import cloud.orbit.dsl.ast.ActorMethod
import cloud.orbit.dsl.ast.AstNode
import cloud.orbit.dsl.ast.CompilationUnit
import cloud.orbit.dsl.ast.DataDeclaration
import cloud.orbit.dsl.ast.DataField
import cloud.orbit.dsl.ast.EnumDeclaration
import cloud.orbit.dsl.ast.EnumMember
import cloud.orbit.dsl.ast.MethodParameter
import cloud.orbit.dsl.ast.ParseContext
import cloud.orbit.dsl.ast.TypeReference
import cloud.orbit.dsl.error.OrbitDslCompilationException
import cloud.orbit.dsl.type.PrimitiveType
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
                        context = AstNode.Context(
                            ParseContext(
                                "/path/to/file1.orbit",
                                line = 1,
                                column = 5
                            )
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
                        context = AstNode.Context(
                            ParseContext(
                                "/path/to/file2.orbit",
                                line = 1,
                                column = 5
                            )
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
                        context = AstNode.Context(
                            ParseContext(
                                "/path/to/file3.orbit",
                                line = 1,
                                column = 6
                            )
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

            actor MyKeyedActor<KeyType> { }
        """.trimIndent()

        val expectedCompilationUnit = CompilationUnit(
            testPackageName,
            enums = listOf(
                EnumDeclaration(
                    context = AstNode.Context(
                        ParseContext(
                            filePath = testFilePath,
                            line = 2,
                            column = 5
                        )
                    ),
                    name = "RGB",
                    members = listOf(
                        EnumMember(
                            context = AstNode.Context(
                                ParseContext(
                                    filePath = testFilePath,
                                    line = 4,
                                    column = 4
                                )
                            ),
                            name = "R",
                            index = 1
                        ),
                        EnumMember(
                            context = AstNode.Context(
                                ParseContext(
                                    filePath = testFilePath,
                                    line = 5,
                                    column = 4
                                )
                            ),
                            name = "G",
                            index = 2
                        ),
                        EnumMember(
                            context = AstNode.Context(
                                ParseContext(
                                    filePath = testFilePath,
                                    line = 6,
                                    column = 4
                                )
                            ),
                            name = "B",
                            index = 3
                        )
                    )
                )
            ),
            data = listOf(
                DataDeclaration(
                    context = AstNode.Context(
                        ParseContext(
                            filePath = testFilePath,
                            line = 9,
                            column = 5
                        )
                    ),
                    name = "Payload",
                    fields = listOf(
                        DataField(
                            context = AstNode.Context(
                                ParseContext(
                                    filePath = testFilePath,
                                    line = 10,
                                    column = 8
                                )
                            ),
                            name = "field1",
                            type = TypeReference(
                                context = AstNode.Context(
                                    ParseContext(
                                        filePath = testFilePath,
                                        line = 10,
                                        column = 4
                                    )
                                ),
                                name = "int"
                            ),
                            index = 1
                        ),
                        DataField(
                            context = AstNode.Context(
                                ParseContext(
                                    filePath = testFilePath,
                                    line = 12,
                                    column = 8
                                )
                            ),
                            name = "field2",
                            type = TypeReference(
                                context = AstNode.Context(
                                    ParseContext(
                                        filePath = testFilePath,
                                        line = 12,
                                        column = 4
                                    )
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
                    context = AstNode.Context(
                        ParseContext(
                            filePath = testFilePath,
                            line = 19,
                            column = 6
                        )
                    ),
                    name = "MyActor",
                    keyType = TypeReference(PrimitiveType.VOID),
                    methods = listOf(
                        ActorMethod(
                            context = AstNode.Context(
                                ParseContext(
                                    filePath = testFilePath,
                                    line = 21,
                                    column = 8
                                )
                            ),
                            name = "no_args",
                            returnType = TypeReference(
                                context = AstNode.Context(
                                    ParseContext(
                                        filePath = testFilePath,
                                        line = 21,
                                        column = 4
                                    )
                                ),
                                name = "int"
                            )
                        ),
                        ActorMethod(
                            context = AstNode.Context(
                                ParseContext(
                                    filePath = testFilePath,
                                    line = 22,
                                    column = 9
                                )
                            ),
                            name = "one_arg",
                            returnType = TypeReference(
                                context = AstNode.Context(
                                    ParseContext(
                                        filePath = testFilePath,
                                        line = 22,
                                        column = 4
                                    )
                                ),
                                name = "void"
                            ),
                            params = listOf(
                                MethodParameter(
                                    context = AstNode.Context(
                                        ParseContext(
                                            filePath = testFilePath,
                                            line = 22,
                                            column = 21
                                        )
                                    ),
                                    name = "a",
                                    type = TypeReference(
                                        context = AstNode.Context(
                                            ParseContext(
                                                filePath = testFilePath,
                                                line = 22,
                                                column = 17
                                            )
                                        ),
                                        name = "RGB"
                                    )
                                )
                            )
                        ),
                        ActorMethod(
                            context = AstNode.Context(
                                ParseContext(
                                    filePath = testFilePath,
                                    line = 23,
                                    column = 8
                                )
                            ),
                            name = "multiple_args",
                            returnType = TypeReference(
                                context = AstNode.Context(
                                    ParseContext(
                                        filePath = testFilePath,
                                        line = 23,
                                        column = 4
                                    )
                                ),
                                name = "RGB"
                            ),
                            params = listOf(
                                MethodParameter(
                                    context = AstNode.Context(
                                        ParseContext(
                                            filePath = testFilePath,
                                            line = 23,
                                            column = 26
                                        )
                                    ),
                                    name = "arg1",
                                    type = TypeReference(
                                        context = AstNode.Context(
                                            ParseContext(
                                                filePath = testFilePath,
                                                line = 23,
                                                column = 22
                                            )
                                        ),
                                        name = "RGB"
                                    )
                                ),
                                MethodParameter(
                                    context = AstNode.Context(
                                        ParseContext(
                                            filePath = testFilePath,
                                            line = 23,
                                            column = 36
                                        )
                                    ),
                                    name = "arg2",
                                    type = TypeReference(
                                        context = AstNode.Context(
                                            ParseContext(
                                                filePath = testFilePath,
                                                line = 23,
                                                column = 32
                                            )
                                        ),
                                        name = "RGB"
                                    )
                                )
                            )
                        ),
                        ActorMethod(
                            context = AstNode.Context(
                                ParseContext(
                                    filePath = testFilePath,
                                    line = 25,
                                    column = 17
                                )
                            ),
                            name = "generic_return",
                            returnType = TypeReference(
                                context = AstNode.Context(
                                    ParseContext(
                                        filePath = testFilePath,
                                        line = 25,
                                        column = 4
                                    )
                                ),
                                name = "list",
                                of = listOf(
                                    TypeReference(
                                        context = AstNode.Context(
                                            ParseContext(
                                                filePath = testFilePath,
                                                line = 25,
                                                column = 9
                                            )
                                        ),
                                        name = "string"
                                    )
                                )
                            )
                        ),
                        ActorMethod(
                            context = AstNode.Context(
                                ParseContext(
                                    filePath = testFilePath,
                                    line = 26,
                                    column = 9
                                )
                            ),
                            name = "generic_arg",
                            returnType = TypeReference(
                                context = AstNode.Context(
                                    ParseContext(
                                        filePath = testFilePath,
                                        line = 26,
                                        column = 4
                                    )
                                ),
                                name = "void"
                            ),
                            params = listOf(
                                MethodParameter(
                                    context = AstNode.Context(
                                        ParseContext(
                                            filePath = testFilePath,
                                            line = 26,
                                            column = 31
                                        )
                                    ),
                                    name = "arg1",
                                    type = TypeReference(
                                        context = AstNode.Context(
                                            ParseContext(
                                                filePath = testFilePath,
                                                line = 26,
                                                column = 21
                                            )
                                        ),
                                        name = "list", of = listOf(
                                            TypeReference(
                                                context = AstNode.Context(
                                                    ParseContext(
                                                        filePath = testFilePath,
                                                        line = 26,
                                                        column = 26
                                                    )
                                                ),
                                                name = "int"
                                            )
                                        )
                                    )
                                )
                            )
                        ),
                        ActorMethod(
                            context = AstNode.Context(
                                ParseContext(
                                    filePath = testFilePath,
                                    line = 27,
                                    column = 21
                                )
                            ),
                            name = "generic_multi",
                            returnType = TypeReference(
                                context = AstNode.Context(
                                    ParseContext(
                                        filePath = testFilePath,
                                        line = 27,
                                        column = 4
                                    )
                                ),
                                name = "map",
                                of = listOf(
                                    TypeReference(
                                        context = AstNode.Context(
                                            ParseContext(
                                                filePath = testFilePath,
                                                line = 27,
                                                column = 8
                                            )
                                        ),
                                        name = "RGB"
                                    ),
                                    TypeReference(
                                        context = AstNode.Context(
                                            ParseContext(
                                                filePath = testFilePath,
                                                line = 27,
                                                column = 13
                                            )
                                        ),
                                        name = "string"
                                    )
                                )
                            ),
                            params = listOf(
                                MethodParameter(
                                    context = AstNode.Context(
                                        ParseContext(
                                            filePath = testFilePath,
                                            line = 27,
                                            column = 56
                                        )
                                    ),
                                    name = "arg1",
                                    type = TypeReference(
                                        context = AstNode.Context(
                                            ParseContext(
                                                filePath = testFilePath,
                                                line = 27,
                                                column = 35
                                            )
                                        ),
                                        name = "map",
                                        of = listOf(
                                            TypeReference(
                                                context = AstNode.Context(
                                                    ParseContext(
                                                        filePath = testFilePath,
                                                        line = 27,
                                                        column = 39
                                                    )
                                                ),
                                                name = "string"
                                            ),
                                            TypeReference(
                                                context = AstNode.Context(
                                                    ParseContext(
                                                        filePath = testFilePath,
                                                        line = 27,
                                                        column = 47
                                                    )
                                                ),
                                                name = "Payload"
                                            )
                                        )
                                    )
                                )
                            )
                        ),
                        ActorMethod(
                            context = AstNode.Context(
                                ParseContext(
                                    filePath = testFilePath,
                                    line = 28,
                                    column = 27
                                )
                            ),
                            name = "generics_nested",
                            returnType = TypeReference(
                                context = AstNode.Context(
                                    ParseContext(
                                        filePath = testFilePath,
                                        line = 28,
                                        column = 4
                                    )
                                ),
                                name = "list",
                                of = listOf(
                                    TypeReference(
                                        context = AstNode.Context(
                                            ParseContext(
                                                filePath = testFilePath,
                                                line = 28,
                                                column = 9
                                            )
                                        ),
                                        name = "map",
                                        of = listOf(
                                            TypeReference(
                                                context = AstNode.Context(
                                                    ParseContext(
                                                        filePath = testFilePath,
                                                        line = 28,
                                                        column = 13
                                                    )
                                                ),
                                                name = "string"
                                            ),
                                            TypeReference(
                                                context = AstNode.Context(
                                                    ParseContext(
                                                        filePath = testFilePath,
                                                        line = 28,
                                                        column = 21
                                                    )
                                                ),
                                                name = "RGB"
                                            )
                                        )
                                    )
                                )
                            ),
                            params = listOf(
                                MethodParameter(
                                    context = AstNode.Context(
                                        ParseContext(
                                            filePath = testFilePath,
                                            line = 28,
                                            column = 76
                                        )
                                    ),
                                    name = "arg1",
                                    type = TypeReference(
                                        context = AstNode.Context(
                                            ParseContext(
                                                filePath = testFilePath,
                                                line = 28,
                                                column = 43
                                            )
                                        ),
                                        name = "map",
                                        of = listOf(
                                            TypeReference(
                                                context = AstNode.Context(
                                                    ParseContext(
                                                        filePath = testFilePath,
                                                        line = 28,
                                                        column = 47
                                                    )
                                                ),
                                                name = "string"
                                            ),
                                            TypeReference(
                                                context = AstNode.Context(
                                                    ParseContext(
                                                        filePath = testFilePath,
                                                        line = 28,
                                                        column = 55
                                                    )
                                                ),
                                                name = "list",
                                                of = listOf(
                                                    TypeReference(
                                                        context = AstNode.Context(
                                                            ParseContext(
                                                                filePath = testFilePath,
                                                                line = 28,
                                                                column = 60
                                                            )
                                                        ),
                                                        name = "list",
                                                        of = listOf(
                                                            TypeReference(
                                                                context = AstNode.Context(
                                                                    ParseContext(
                                                                        filePath = testFilePath,
                                                                        line = 28,
                                                                        column = 65
                                                                    )
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
                    context = AstNode.Context(
                        ParseContext(
                            filePath = testFilePath,
                            line = 31,
                            column = 6
                        )
                    ),
                    name = "MyKeylessActor",
                    keyType = TypeReference(PrimitiveType.VOID)
                ),
                ActorDeclaration(
                    context = AstNode.Context(
                        ParseContext(
                            filePath = testFilePath,
                            line = 33,
                            column = 6
                        )
                    ),
                    name = "MyKeyedActor",
                    keyType = TypeReference(
                        name = "KeyType",
                        context = AstNode.Context(
                            ParseContext(
                                filePath = testFilePath,
                                line = 33,
                                column = 19
                            )
                        ))
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
