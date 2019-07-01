/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.type.check

import cloud.orbit.dsl.ast.TypeReference
import cloud.orbit.dsl.ast.ErrorReporter

/**
 * A type check.
 *
 * Implementations are not required to visit type parameters.
 */
interface TypeCheck {
    /**
     * Runs a check against a type.
     *
     * @param typeReference the reference to the type to check.
     * @param context the context in which this type is being referenced (e.g. as a data field type).
     */
    fun check(typeReference: TypeReference, context: Context, errorReporter: ErrorReporter)

    enum class Context {
        ACTOR_KEY,
        DATA_FIELD,
        METHOD_RETURN,
        METHOD_PARAMETER,
        TYPE_PARAMETER
    }
}
