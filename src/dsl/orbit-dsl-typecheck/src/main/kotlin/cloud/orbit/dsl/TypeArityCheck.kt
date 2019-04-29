/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

import cloud.orbit.dsl.ast.Type

class TypeArityCheck(
    private val knownTypes: Map<String, TypeDescriptor>
) : TypeCheck() {
    override fun check(type: Type) {
        val descriptor = knownTypes[type.name] ?: return

        if (type.of.size != descriptor.arity) {
            reportError(
                type,
                "expected parameter count for type '${type.name}' is ${descriptor.arity}, found ${type.of.size}"
            )
        }
    }
}
