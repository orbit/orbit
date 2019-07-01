/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.type.check

import cloud.orbit.dsl.ast.AstNode
import cloud.orbit.dsl.ast.ErrorListener
import cloud.orbit.dsl.error.OrbitDslError

class TypeErrorListener : ErrorListener {
    val typeErrors = mutableListOf<OrbitDslError>()

    override fun onError(astNode: AstNode, message: String) {
        typeErrors.add(
            OrbitDslError(
                astNode.context.parseContext.filePath,
                astNode.context.parseContext.line,
                astNode.context.parseContext.column,
                message
            )
        )
    }
}
