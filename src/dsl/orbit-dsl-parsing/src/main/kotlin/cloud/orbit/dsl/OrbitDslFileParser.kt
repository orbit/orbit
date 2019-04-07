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
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ConsoleErrorListener

class OrbitDslFileParser : OrbitDslBaseVisitor<Any>() {
    private val enums = mutableListOf<EnumDeclaration>()
    private val data = mutableListOf<DataDeclaration>()
    private val actors = mutableListOf<ActorDeclaration>()

    // This code is not thread safe. Need to find a way to pass the context into the visitor
    fun parse(input: String, packageName: String): CompilationUnit {
        val lexer = OrbitDslLexer(CharStreams.fromString(input))
        val parser = OrbitDslParser(CommonTokenStream(lexer)).also {
            it.addErrorListener(ThrowingErrorListener())
            it.removeErrorListener(ConsoleErrorListener.INSTANCE)
        }

        actors.clear()
        data.clear()

        visitFile(parser.file())

        return CompilationUnit(
            packageName,
            enums,
            data,
            actors
        )
    }

    override fun visitEnumDeclaration(ctx: OrbitDslParser.EnumDeclarationContext?) = enums.add(
        EnumDeclaration(
            ctx!!.name.text,
            ctx.children
                .asSequence()
                .filterIsInstance(OrbitDslParser.EnumMemberContext::class.java)
                .map { EnumMember(it.name.text, it.index.text.toInt()) }
                .toList()))

    override fun visitDataDeclaration(ctx: OrbitDslParser.DataDeclarationContext?) = data.add(
        DataDeclaration(
            ctx!!.name.text,
            ctx.children
                .asSequence()
                .filterIsInstance(OrbitDslParser.DataFieldContext::class.java)
                .map { DataField(it.name.text, makeType(it.type()), it.index.text.toInt()) }
                .toList()))

    override fun visitActorDeclaration(ctx: OrbitDslParser.ActorDeclarationContext?) = actors.add(
        ActorDeclaration(
            ctx!!.name.text,
            ctx.keyType?.toActorKeyType() ?: ActorKeyType.NO_KEY,
            ctx.children
                .asSequence()
                .filterIsInstance(OrbitDslParser.ActorMethodContext::class.java)
                .map { m ->
                    ActorMethod(
                        name = m.name.text,
                        returnType = makeType(m.returnType),
                        params = m.children
                            .asSequence()
                            .filterIsInstance(OrbitDslParser.MethodParamContext::class.java)
                            .map { p -> MethodParameter(p.name.text, makeType(p.type())) }
                            .toList())
                }
                .toList()))

    private fun makeType(ctx: OrbitDslParser.TypeContext): Type =
        Type(
            ctx.name.text, ctx.children
                .asSequence()
                .filterIsInstance(OrbitDslParser.TypeContext::class.java)
                .map(::makeType)
                .toList()
        )

    private fun OrbitDslParser.TypeContext.toActorKeyType() =
        when (this.text) {
            "string" -> ActorKeyType.STRING
            "int32" -> ActorKeyType.INT32
            "int64" -> ActorKeyType.INT64
            "guid" -> ActorKeyType.GUID
            else -> throw OrbitDslException(
                "Actor key type must be string, int32, int64, or guid; found '${this.text}'"
            )
        }
}
