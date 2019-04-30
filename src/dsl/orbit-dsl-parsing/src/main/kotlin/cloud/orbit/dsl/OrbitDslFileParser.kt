/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

import cloud.orbit.dsl.ast.CompilationUnit
import cloud.orbit.dsl.ast.ParseContext
import cloud.orbit.dsl.ast.TypeOccurrenceContext
import cloud.orbit.dsl.error.OrbitDslCompilationException
import cloud.orbit.dsl.error.OrbitDslError
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
     * Obtain a list of Orbit DSL [CompilationUnit] instances from input source code.
     *
     * @param inputs A list of [OrbitDslParseInput] instances containing the source code to be parsed.
     * @return A list of [CompilationUnit] instances with abstract syntax trees for the entities parsed
     * from the input source code.
     */
    fun parse(inputs: List<OrbitDslParseInput>): List<CompilationUnit> {
        checkSyntax(inputs)
        return inputs.map(::buildCompilationUnit)
    }

    private fun checkSyntax(inputs: List<OrbitDslParseInput>) {
        val syntaxErrors = inputs.flatMap { input ->
            val errorListener = SyntaxErrorListener(input.filePath)
            val lexer = OrbitDslLexer(CharStreams.fromString(input.text))
            val parser = OrbitDslParser(CommonTokenStream(lexer)).also {
                it.addErrorListener(errorListener)
                it.removeErrorListener(ConsoleErrorListener.INSTANCE)
            }

            SyntaxVisitor().visitFile(parser.file())
            errorListener.syntaxErrors
        }

        if (syntaxErrors.isNotEmpty()) {
            throw OrbitDslCompilationException(syntaxErrors)
        }
    }

    private fun buildCompilationUnit(input: OrbitDslParseInput): CompilationUnit {
        val lexer = OrbitDslLexer(CharStreams.fromString(input.text))
        val parser = OrbitDslParser(CommonTokenStream(lexer)).also {
            it.removeErrorListener(ConsoleErrorListener.INSTANCE)
        }

        val parseContextProvider = object : ParseContextProvider {
            override fun fromToken(token: Token) =
                ParseContext(input.filePath, token.line, token.charPositionInLine)
        }

        val typeParameterVisitor = TypeVisitor(TypeOccurrenceContext.TYPE_PARAMETER, parseContextProvider)
        val enumDeclarationVisitor = EnumDeclarationVisitor(parseContextProvider)
        val dataDeclarationVisitor = DataDeclarationVisitor(
            TypeVisitor(TypeOccurrenceContext.DATA_FIELD, typeParameterVisitor, parseContextProvider),
            parseContextProvider
        )
        val actorDeclarationVisitor = ActorDeclarationVisitor(
            TypeVisitor(TypeOccurrenceContext.METHOD_RETURN, typeParameterVisitor, parseContextProvider),
            TypeVisitor(TypeOccurrenceContext.METHOD_PARAMETER, typeParameterVisitor, parseContextProvider),
            parseContextProvider
        )

        try {
            return CompilationUnitBuilderVisitor(
                input.packageName,
                enumDeclarationVisitor,
                dataDeclarationVisitor,
                actorDeclarationVisitor
            ).visitFile(parser.file())
        } catch (e: UnsupportedActorKeyTypeException) {
            throw OrbitDslCompilationException(
                listOf(
                    OrbitDslError(
                        input.filePath,
                        e.line,
                        e.column,
                        e.message!!
                    )
                )
            )
        }
    }
}
