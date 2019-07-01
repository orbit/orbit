/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.type.check

import cloud.orbit.dsl.ast.TypeReference
import cloud.orbit.dsl.ast.ErrorReporter
import cloud.orbit.dsl.type.PrimitiveType

class VoidUsageCheck : TypeCheck {
    private val supportedContexts = setOf(
        TypeCheck.Context.ACTOR_KEY,
        TypeCheck.Context.METHOD_RETURN
    )

    override fun check(typeReference: TypeReference, context: TypeCheck.Context, errorReporter: ErrorReporter) {
        if (typeReference.name != PrimitiveType.VOID) {
            return
        }

        if (context !in supportedContexts) {
            // Since 'void' is not explicit as key type, we only mention method return type in error message
            errorReporter.reportError(typeReference, "'void' can only be used as method return type")
        }
    }
}
