/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.type.check

import cloud.orbit.dsl.ast.ErrorReporter
import cloud.orbit.dsl.ast.TypeReference
import cloud.orbit.dsl.type.PrimitiveType
import io.mockk.called
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class ActorKeyTypeCheckTest {
    private val check = ActorKeyTypeCheck()

    @MockK(relaxUnitFun = true)
    lateinit var errorReporter: ErrorReporter

    @Test
    fun noErrorWhenNotInActorKeyContext() {
        TypeCheck.Context.values()
            .filter {
                it != TypeCheck.Context.ACTOR_KEY
            }
            .forEach {
                check.check(TypeReference("any"), it, errorReporter)
            }

        verify { errorReporter wasNot called }
        confirmVerified(errorReporter)
    }

    @Test
    fun noErrorWhenTypeIsSupportedAsActorKeyType() {
        listOf(
            PrimitiveType.GUID,
            PrimitiveType.INT32,
            PrimitiveType.INT64,
            PrimitiveType.STRING,
            PrimitiveType.VOID
        ).map {
            TypeReference(it)
        }.forEach {
            check.check(it, TypeCheck.Context.ACTOR_KEY, errorReporter)
        }

        verify { errorReporter wasNot called }
        confirmVerified(errorReporter)
    }

    @Test
    fun errorWhenTypeIsNotSupportedAsActorKeyType() {
        check.check(TypeReference("foo"), TypeCheck.Context.ACTOR_KEY, errorReporter)

        verify {
            errorReporter.reportError(
                TypeReference("foo"),
                "actor key type must be string, int32, int64, or guid; found 'foo'"
            )
        }
        confirmVerified(errorReporter)
    }
}
