/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.parsing.visitor

import cloud.orbit.dsl.ast.AstNode
import org.antlr.v4.runtime.Token

interface AstNodeContextProvider {
    fun fromToken(token: Token): AstNode.Context
}
