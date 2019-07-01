/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.parsing
/**
 * Represents an input to [OrbitDslFileParser].
 *
 * @param text a string containing Orbit DSL source code.
 * @param packageName the fully qualified name of the package containing this input.
 * @param filePath the filesystem path where this input resides.
 */
data class OrbitDslParseInput(val text: String, val packageName: String, val filePath: String)
