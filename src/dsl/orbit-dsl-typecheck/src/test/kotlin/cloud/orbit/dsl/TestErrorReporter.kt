/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

import cloud.orbit.dsl.ast.AstNode
import cloud.orbit.dsl.ast.error.ErrorReporter

class TestErrorReporter : ErrorReporter {
    val errors = mutableListOf<String>()

    override fun reportError(astNode: AstNode, message: String) {
        errors.add(message)
    }
}
