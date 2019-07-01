/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.parsing

import cloud.orbit.dsl.OrbitDslBaseVisitor
import cloud.orbit.dsl.OrbitDslLexer
import cloud.orbit.dsl.OrbitDslParser
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.junit.jupiter.api.Test

class OrbitDslParserTest {
    @Test
    fun parsesEnum_Empty() {
        checkParse("enum foo{}")
    }

    @Test
    fun parsesEnum_SingleMember() {
        checkParse(
            """
            enum foo {
                val1 = 3;
            }
        """
        )
    }

    @Test
    fun parsesEnum_MultipleMembers() {
        checkParse(
            """
            enum foo {
                val1 = 3;
                val2 = 5;
            }
        """
        )
    }

    @Test
    fun parsesData_Empty() {
        checkParse("data foo{}")
    }

    @Test
    fun parsesData_SingleField() {
        checkParse(
            """
            data foo {
                int f1 = 3;
            }
        """
        )
    }

    @Test
    fun parsesData_MultipleFields() {
        checkParse(
            """
            data foo {
                int f1 = 3;
                string f2 = 5;
            }
        """
        )
    }

    @Test
    fun parsesData_GenericField_SingleType() {
        checkParse(
            """
            data foo {
                list<int32> f1 = 3;
            }
        """
        )
    }

    @Test
    fun parsesData_GenericField_MultipleTypes() {
        checkParse(
            """
            data foo {
                map<string, int32> f1 = 3;
            }
        """
        )
    }

    @Test
    fun parsesData_GenericField_Nested() {
        checkParse(
            """
            data foo {
                list<map<string, int32>> f1 = 3;
            }
        """
        )
    }

    @Test
    fun parsesActor_Empty() {
        checkParse("actor foo{}")
    }

    @Test
    fun parsesActor_WithKeyType() {
        checkParse("actor foo<string> {}")
    }

    @Test
    fun parsesActor_SingleMethod() {
        checkParse(
            """
            actor foo {
                void bar();
            }
        """
        )
    }

    @Test
    fun parsesActor_MultipleMethods() {
        checkParse(
            """
            actor foo {
                void bar();
                void baz();
            }
        """
        )
    }

    @Test
    fun parsesActor_MethodWithOneParam() {
        checkParse(
            """
            actor foo {
                void bar(int k);
            }
        """
        )
    }

    @Test
    fun parsesActor_MethodWithMultipleParams() {
        checkParse(
            """
            actor foo {
                void bar(int arg1, RGB arg2);
            }
        """
        )
    }

    private fun checkParse(input: String) {
        val lexer = OrbitDslLexer(CharStreams.fromString(input))
        val parser = OrbitDslParser(CommonTokenStream(lexer))
        parser.addErrorListener(object : BaseErrorListener() {
            override fun syntaxError(
                recognizer: Recognizer<*, *>?,
                offendingSymbol: Any?,
                line: Int,
                charPositionInLine: Int,
                msg: String?,
                e: RecognitionException?
            ) {
                throw ParseCancellationException(msg, e)
            }
        })
        val visitor = object : OrbitDslBaseVisitor<Unit>() {}

        visitor.visitFile(parser.file())
    }
}
