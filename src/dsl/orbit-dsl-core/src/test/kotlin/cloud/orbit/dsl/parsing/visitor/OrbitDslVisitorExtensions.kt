/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.parsing.visitor

import cloud.orbit.dsl.OrbitDslLexer
import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.OrbitDslVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext

fun <T, U : ParserRuleContext> OrbitDslVisitor<T>.parse(
    input: String,
    parserMethod: (OrbitDslParser) -> U
): T {
    val lexer = OrbitDslLexer(CharStreams.fromString(input))
    val parser = OrbitDslParser(CommonTokenStream(lexer))
    return parserMethod(parser).accept(this)
}
