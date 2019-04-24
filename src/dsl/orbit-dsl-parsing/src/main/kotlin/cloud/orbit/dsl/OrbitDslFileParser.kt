/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

import cloud.orbit.dsl.ast.CompilationUnit
import cloud.orbit.dsl.ast.ParseContext
import cloud.orbit.dsl.visitor.ActorDeclarationVisitor
import cloud.orbit.dsl.visitor.CompilationUnitBuilderVisitor
import cloud.orbit.dsl.visitor.DataDeclarationVisitor
import cloud.orbit.dsl.visitor.EnumDeclarationVisitor
import cloud.orbit.dsl.visitor.ParseContextProvider
import cloud.orbit.dsl.visitor.SyntaxVisitor
import cloud.orbit.dsl.visitor.TypeVisitor
import cloud.orbit.dsl.visitor.UnsupportedActorKeyTypeException
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ConsoleErrorListener
import org.antlr.v4.runtime.Token

class OrbitDslFileParser {
    /**
     * Obtain an Orbit DSL [CompilationUnit] from source code stored in a string.
     *
     * @param input the string containing Orbit DSL source code.
     * @param packageName the package name the resulting [CompilationUnit] will be associated with.
     * @param filePath the filesystem path that syntax errors and syntax tree annotations will be associated with.
     * @return A [CompilationUnit] with abstract syntax trees for the entities parsed from the input source code.
     */
    fun parse(input: String, packageName: String, filePath: String): CompilationUnit {
        checkSyntax(input, filePath)
        return buildCompilationUnit(input, packageName, filePath)
    }

    private fun checkSyntax(contents: String, filePath: String) {
        val errorListener = CollectingErrorListener(filePath)
        val lexer = OrbitDslLexer(CharStreams.fromString(contents))
        val parser = OrbitDslParser(CommonTokenStream(lexer)).also {
            it.removeErrorListener(ConsoleErrorListener.INSTANCE)
        }

        try {
            parser.addErrorListener(errorListener)

            SyntaxVisitor().visitFile(parser.file())

            if (errorListener.syntaxErrors.isNotEmpty()) {
                throw OrbitDslParsingException(errorListener.syntaxErrors)
            }
        } finally {
            parser.removeErrorListener(errorListener)
        }
    }

    private fun buildCompilationUnit(contents: String, packageName: String, filePath: String): CompilationUnit {
        val lexer = OrbitDslLexer(CharStreams.fromString(contents))
        val parser = OrbitDslParser(CommonTokenStream(lexer)).also {
            it.removeErrorListener(ConsoleErrorListener.INSTANCE)
        }

        val parseContextProvider = object : ParseContextProvider {
            override fun fromToken(token: Token) =
                ParseContext(filePath, token.line, token.charPositionInLine)
        }

        val typeVisitor = TypeVisitor(parseContextProvider)
        val enumDeclarationVisitor = EnumDeclarationVisitor(parseContextProvider)
        val dataDeclarationVisitor = DataDeclarationVisitor(typeVisitor, parseContextProvider)
        val actorDeclarationVisitor = ActorDeclarationVisitor(typeVisitor, parseContextProvider)

        try {
            return CompilationUnitBuilderVisitor(
                packageName,
                enumDeclarationVisitor,
                dataDeclarationVisitor,
                actorDeclarationVisitor
            ).visitFile(parser.file())
        } catch (e: UnsupportedActorKeyTypeException) {
            throw OrbitDslParsingException(
                listOf(
                    OrbitDslSyntaxError(
                        filePath,
                        e.line,
                        e.column,
                        e.message
                    )
                )
            )
        }
    }
}
