/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.ast

data class DataDeclaration(
    override val name: String,
    val fields: List<DataField> = emptyList()
) : AstNode(), Declaration
