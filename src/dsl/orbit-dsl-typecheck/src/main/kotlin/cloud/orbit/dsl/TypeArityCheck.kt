/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

import cloud.orbit.dsl.ast.Type
import cloud.orbit.dsl.ast.error.ErrorReporter

class TypeArityCheck(
    private val knownTypes: Map<String, TypeDescriptor>
) : TypeCheck {
    override fun check(type: Type, context: TypeCheck.Context, errorReporter: ErrorReporter) {
        val descriptor = knownTypes[type.name] ?: return

        if (type.of.size != descriptor.arity) {
            errorReporter.reportError(
                type,
                "expected parameter count for type '${type.name}' is ${descriptor.arity}, found ${type.of.size}"
            )
        }
    }
}
