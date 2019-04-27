/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl
/**
 * @param text a string containing Orbit DSL source code.
 * @param packageName the package name the resulting [CompilationUnit] will be associated with.
 * @param filePath the filesystem path that syntax errors and syntax tree annotations will be associated with.
 */
data class OrbitDslParseInput(val text: String, val packageName: String, val filePath: String)
