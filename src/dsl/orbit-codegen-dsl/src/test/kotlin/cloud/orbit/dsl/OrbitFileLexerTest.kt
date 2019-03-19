/*
 Copyright (C) 2015 - 2018 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.Token
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class OrbitFileLexerTest {
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
        "Grain"
    )
        .map { testSingleToken("Id", it, OrbitLexer.ID) }

    @TestFactory
    fun tokenizeInt() = listOf(
        "0",
        "1",
        "32",
        "2147483647",
        "9223372036854775807"
    )
        .map { testSingleToken("Int", it, OrbitLexer.INT) }

    @TestFactory
    fun tokenizeKeywords() = mapOf(
        "enum" to OrbitLexer.ENUM,
        "data" to OrbitLexer.DATA,
        "grain" to OrbitLexer.GRAIN
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
            grain MyGrain{
                // Comment
                int no_args();
                void one_arg(RGB a); // Comment
                RGB multiple_args(RGB arg1, RGB arg2);
                // RGB multiple_args(RGB arg1, RGB arg2); This should be ignored
            }
        """.trimIndent()

        val expectedTokens = listOf(
            // ==== Enum ====
            "enum" to OrbitLexer.ENUM,
            "RGB" to OrbitLexer.ID,
            "{" to OrbitLexer.LC_BRACE,

            // Enum value
            "R" to OrbitLexer.ID,
            "=" to OrbitLexer.EQUAL,
            "1" to OrbitLexer.INT,
            ";" to OrbitLexer.SEMI_COLON,

            // Enum value
            "G" to OrbitLexer.ID,
            "=" to OrbitLexer.EQUAL,
            "2" to OrbitLexer.INT,
            ";" to OrbitLexer.SEMI_COLON,

            // Enum value
            "B" to OrbitLexer.ID,
            "=" to OrbitLexer.EQUAL,
            "3" to OrbitLexer.INT,
            ";" to OrbitLexer.SEMI_COLON,

            "}" to OrbitLexer.RC_BRACE,

            // ==== Data ====
            "data" to OrbitLexer.DATA,
            "Payload" to OrbitLexer.ID,
            "{" to OrbitLexer.LC_BRACE,

            // Data field
            "int" to OrbitLexer.ID,
            "field1" to OrbitLexer.ID,
            "=" to OrbitLexer.EQUAL,
            "1" to OrbitLexer.INT,
            ";" to OrbitLexer.SEMI_COLON,

            // Data field
            "RGB" to OrbitLexer.ID,
            "field2" to OrbitLexer.ID,
            "=" to OrbitLexer.EQUAL,
            "2" to OrbitLexer.INT,
            ";" to OrbitLexer.SEMI_COLON,

            "}" to OrbitLexer.RC_BRACE,

            // ==== Grain ====
            "grain" to OrbitLexer.GRAIN,
            "MyGrain" to OrbitLexer.ID,
            "{" to OrbitLexer.LC_BRACE,

            // Grain method
            "int" to OrbitLexer.ID,
            "no_args" to OrbitLexer.ID,
            "(" to OrbitLexer.L_PAREN,
            ")" to OrbitLexer.R_PAREN,
            ";" to OrbitLexer.SEMI_COLON,

            // Grain method
            "void" to OrbitLexer.ID,
            "one_arg" to OrbitLexer.ID,
            "(" to OrbitLexer.L_PAREN,
            "RGB" to OrbitLexer.ID,
            "a" to OrbitLexer.ID,
            ")" to OrbitLexer.R_PAREN,
            ";" to OrbitLexer.SEMI_COLON,

            // Grain method
            "RGB" to OrbitLexer.ID,
            "multiple_args" to OrbitLexer.ID,
            "(" to OrbitLexer.L_PAREN,
            "RGB" to OrbitLexer.ID,
            "arg1" to OrbitLexer.ID,
            "," to OrbitLexer.COMMA,
            "RGB" to OrbitLexer.ID,
            "arg2" to OrbitLexer.ID,
            ")" to OrbitLexer.R_PAREN,
            ";" to OrbitLexer.SEMI_COLON,

            "}" to OrbitLexer.RC_BRACE,

            "<EOF>" to Lexer.EOF
        )

        val lexer = OrbitLexer(CharStreams.fromString(file))

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
            val lexer = OrbitLexer(CharStreams.fromString(text))
            val token = lexer.nextToken()

            Assertions.assertEquals(expectedTokenType, token.type)
            Assertions.assertEquals(text, token.text)

            Assertions.assertEquals(Token.EOF, lexer.nextToken().type)
        }
    }
}
