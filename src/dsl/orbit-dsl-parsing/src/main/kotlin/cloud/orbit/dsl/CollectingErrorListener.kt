/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

class CollectingErrorListener(private val filePath: String) : BaseErrorListener() {
    val syntaxErrors = mutableListOf<OrbitDslSyntaxError>()

    override fun syntaxError(
        recognizer: Recognizer<*, *>?,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String?,
        e: RecognitionException?
    ) {
        syntaxErrors.add(OrbitDslSyntaxError(filePath, line, charPositionInLine, msg))
        super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e)
    }
}
