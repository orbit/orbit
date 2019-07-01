/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.parsing

import cloud.orbit.dsl.OrbitDslLexer
import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.ast.AstNode
import cloud.orbit.dsl.ast.CompilationUnit
import cloud.orbit.dsl.ast.ParseContext
import cloud.orbit.dsl.error.OrbitDslCompilationException
import cloud.orbit.dsl.parsing.visitor.ActorDeclarationVisitor
import cloud.orbit.dsl.parsing.visitor.AstNodeContextProvider
import cloud.orbit.dsl.parsing.visitor.CompilationUnitVisitor
import cloud.orbit.dsl.parsing.visitor.DataDeclarationVisitor
import cloud.orbit.dsl.parsing.visitor.EnumDeclarationVisitor
import cloud.orbit.dsl.parsing.visitor.SyntaxVisitor
import cloud.orbit.dsl.parsing.visitor.TypeReferenceVisitor
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

        val contextProvider = object : AstNodeContextProvider {
            override fun fromToken(token: Token) =
                AstNode.Context(ParseContext(input.filePath, token.line, token.charPositionInLine))
        }

        val typeReferenceVisitor = TypeReferenceVisitor(contextProvider)
        val enumDeclarationVisitor = EnumDeclarationVisitor(contextProvider)
        val dataDeclarationVisitor = DataDeclarationVisitor(typeReferenceVisitor, contextProvider)
        val actorDeclarationVisitor = ActorDeclarationVisitor(typeReferenceVisitor, contextProvider)

        return CompilationUnitVisitor(
            input.packageName,
            enumDeclarationVisitor,
            dataDeclarationVisitor,
            actorDeclarationVisitor
        ).visitFile(parser.file())
    }
}
