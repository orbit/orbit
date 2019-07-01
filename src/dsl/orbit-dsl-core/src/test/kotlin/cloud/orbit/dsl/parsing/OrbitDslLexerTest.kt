/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.parsing

import cloud.orbit.dsl.OrbitDslLexer
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.Token
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class OrbitDslLexerTest {
    @TestFactory
    fun tokenizeId() = listOf(
        "a",
        "a1",
        "B",
        "B1",
        "A_b",
        "A__b",
        "ThisIsALongIdentifier_With_AllPoss1b1eCharact3rs",
        "a_",
        "Enum",
        "Data",
        "Actor"
    )
        .map { testSingleToken("Id", it, OrbitDslLexer.ID) }

    @TestFactory
    fun tokenizeInt() = listOf(
        "0",
        "1",
        "32",
        "2147483647",
        "9223372036854775807"
    )
        .map { testSingleToken("Int", it, OrbitDslLexer.INT) }

    @TestFactory
    fun tokenizeKeywords() = mapOf(
        "enum" to OrbitDslLexer.ENUM,
        "data" to OrbitDslLexer.DATA,
        "actor" to OrbitDslLexer.ACTOR
    )
        .map { testSingleToken("Keyword", it.key, it.value) }

    @Test
    fun tokenizeCompleteFile() {
        val file = """
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
            }
        """.trimIndent()

        val expectedTokens = listOf(
            // ==== Enum ====
            "enum" to OrbitDslLexer.ENUM,
            "RGB" to OrbitDslLexer.ID,
            "{" to OrbitDslLexer.LC_BRACE,

            // Enum value
            "R" to OrbitDslLexer.ID,
            "=" to OrbitDslLexer.EQUAL,
            "1" to OrbitDslLexer.INT,
            ";" to OrbitDslLexer.SEMI_COLON,

            // Enum value
            "G" to OrbitDslLexer.ID,
            "=" to OrbitDslLexer.EQUAL,
            "2" to OrbitDslLexer.INT,
            ";" to OrbitDslLexer.SEMI_COLON,

            // Enum value
            "B" to OrbitDslLexer.ID,
            "=" to OrbitDslLexer.EQUAL,
            "3" to OrbitDslLexer.INT,
            ";" to OrbitDslLexer.SEMI_COLON,

            "}" to OrbitDslLexer.RC_BRACE,

            // ==== Data ====
            "data" to OrbitDslLexer.DATA,
            "Payload" to OrbitDslLexer.ID,
            "{" to OrbitDslLexer.LC_BRACE,

            // Data field
            "int" to OrbitDslLexer.ID,
            "field1" to OrbitDslLexer.ID,
            "=" to OrbitDslLexer.EQUAL,
            "1" to OrbitDslLexer.INT,
            ";" to OrbitDslLexer.SEMI_COLON,

            // Data field
            "RGB" to OrbitDslLexer.ID,
            "field2" to OrbitDslLexer.ID,
            "=" to OrbitDslLexer.EQUAL,
            "2" to OrbitDslLexer.INT,
            ";" to OrbitDslLexer.SEMI_COLON,

            "}" to OrbitDslLexer.RC_BRACE,

            // ==== Actor ====
            "actor" to OrbitDslLexer.ACTOR,
            "MyActor" to OrbitDslLexer.ID,
            "{" to OrbitDslLexer.LC_BRACE,

            // Actor method
            "int" to OrbitDslLexer.ID,
            "no_args" to OrbitDslLexer.ID,
            "(" to OrbitDslLexer.L_PAREN,
            ")" to OrbitDslLexer.R_PAREN,
            ";" to OrbitDslLexer.SEMI_COLON,

            // Actor method
            "void" to OrbitDslLexer.ID,
            "one_arg" to OrbitDslLexer.ID,
            "(" to OrbitDslLexer.L_PAREN,
            "RGB" to OrbitDslLexer.ID,
            "a" to OrbitDslLexer.ID,
            ")" to OrbitDslLexer.R_PAREN,
            ";" to OrbitDslLexer.SEMI_COLON,

            // Actor method
            "RGB" to OrbitDslLexer.ID,
            "multiple_args" to OrbitDslLexer.ID,
            "(" to OrbitDslLexer.L_PAREN,
            "RGB" to OrbitDslLexer.ID,
            "arg1" to OrbitDslLexer.ID,
            "," to OrbitDslLexer.COMMA,
            "RGB" to OrbitDslLexer.ID,
            "arg2" to OrbitDslLexer.ID,
            ")" to OrbitDslLexer.R_PAREN,
            ";" to OrbitDslLexer.SEMI_COLON,

            "}" to OrbitDslLexer.RC_BRACE,

            "<EOF>" to Lexer.EOF
        )

        val lexer = OrbitDslLexer(CharStreams.fromString(file))

        expectedTokens.forEachIndexed { idx, expected ->
            val token = lexer.nextToken()
            Assertions.assertEquals(expected.first, token.text, "$idx: $expected")
            Assertions.assertEquals(expected.second, token.type, "$idx: $expected")
        }
    }

    private fun testSingleToken(dataType: String, text: String, expectedTokenType: Int): DynamicTest {
        return DynamicTest.dynamicTest(
            String.format("\"%s\" is tokenized as %s", text, dataType)
        ) {
            val lexer = OrbitDslLexer(CharStreams.fromString(text))
            val token = lexer.nextToken()

            Assertions.assertEquals(expectedTokenType, token.type)
            Assertions.assertEquals(text, token.text)

            Assertions.assertEquals(Token.EOF, lexer.nextToken().type)
        }
    }
}
