/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

import cloud.orbit.dsl.ast.AstNode
import cloud.orbit.dsl.ast.error.ErrorListener
import cloud.orbit.dsl.ast.ParseContext
import cloud.orbit.dsl.error.OrbitDslError

class TypeErrorListener : ErrorListener {
    val typeErrors = mutableListOf<OrbitDslError>()

    override fun onError(astNode: AstNode, message: String) {
        typeErrors.add(
            OrbitDslError(
                astNode.parseContext.filePath,
                astNode.parseContext.line,
                astNode.parseContext.column,
                message
            )
        )
    }
}
