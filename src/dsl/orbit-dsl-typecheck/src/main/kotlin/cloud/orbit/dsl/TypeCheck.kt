/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

import cloud.orbit.dsl.ast.Type
import cloud.orbit.dsl.ast.error.ErrorReporter

/**
 * A type check.
 *
 * Implementations are not required to visit type parameters.
 */
interface TypeCheck {
    /**
     * Runs a check against a type.
     *
     * @param type the type to check.
     * @param context the context in which this type is being used (e.g. as a data field type).
     */
    fun check(type: Type, context: Context, errorReporter: ErrorReporter)

    enum class Context {
        DATA_FIELD,
        METHOD_RETURN,
        METHOD_PARAMETER,
        TYPE_PARAMETER
    }
}
