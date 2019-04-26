/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.ast

enum class TypeOccurrenceContext : AstAnnotation {
    DATA_FIELD,
    METHOD_PARAMETER,
    METHOD_RETURN,
    TYPE_PARAMETER
}
