/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

import cloud.orbit.dsl.ast.TypeReference
import cloud.orbit.dsl.ast.error.ErrorReporter
import cloud.orbit.dsl.type.PrimitiveType

class VoidUsageCheck : TypeCheck {
    override fun check(typeReference: TypeReference, context: TypeCheck.Context, errorReporter: ErrorReporter) {
        if (typeReference.name != PrimitiveType.VOID) {
            return
        }

        if (context != TypeCheck.Context.METHOD_RETURN) {
            errorReporter.reportError(typeReference, "'void' can only be used as method return type")
        }
    }
}
