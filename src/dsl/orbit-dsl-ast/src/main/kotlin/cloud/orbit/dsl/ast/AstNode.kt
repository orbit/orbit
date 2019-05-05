/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.ast

interface AstNode {
    val context: Context

    data class Context(val parseContext: ParseContext) {
        companion object {
            val NONE = Context(ParseContext.NONE)
        }
    }
}
