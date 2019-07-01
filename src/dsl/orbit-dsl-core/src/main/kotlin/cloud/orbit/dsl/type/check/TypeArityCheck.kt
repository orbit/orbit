/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.type.check

import cloud.orbit.dsl.ast.TypeReference
import cloud.orbit.dsl.ast.ErrorReporter

class TypeArityCheck(
    private val knownTypes: Map<String, TypeDescriptor>
) : TypeCheck {
    override fun check(typeReference: TypeReference, context: TypeCheck.Context, errorReporter: ErrorReporter) {
        val descriptor = knownTypes[typeReference.name] ?: return

        if (typeReference.of.size != descriptor.arity) {
            errorReporter.reportError(
                typeReference,
                "expected parameter count for type '${typeReference.name}' is ${descriptor.arity}, found ${typeReference.of.size}"
            )
        }
    }
}
