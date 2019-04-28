/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.ast

data class ParseContext(val filePath: String, val line: Int, val column: Int) : AstAnnotation {
    companion object {
        val UNKNOWN = ParseContext("<unknown>", 0, 0)
    }
}
