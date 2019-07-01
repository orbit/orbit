/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.type.check

import cloud.orbit.dsl.ast.TypeReference
import cloud.orbit.dsl.ast.ErrorReporter
import cloud.orbit.dsl.type.PrimitiveType

class ActorKeyTypeCheck : TypeCheck {
    private val supportedActorKeyTypeNames = setOf(
        PrimitiveType.GUID,
        PrimitiveType.INT32,
        PrimitiveType.INT64,
        PrimitiveType.STRING,
        PrimitiveType.VOID
    )

    override fun check(typeReference: TypeReference, context: TypeCheck.Context, errorReporter: ErrorReporter) {
        if (context != TypeCheck.Context.ACTOR_KEY) {
            return
        }

        if (typeReference.name !in supportedActorKeyTypeNames) {
            errorReporter.reportError(
                typeReference,
                "actor key type must be string, int32, int64, or guid; found '${typeReference.name}'"
            )
        }
    }
}
