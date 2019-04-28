/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

data class OrbitDslTypeError(val filePath: String, val line: Int, val column: Int, val message: String?) {
    val errorMessage = "error: $filePath:$line:$column: ${message ?: "type error"}"
}
