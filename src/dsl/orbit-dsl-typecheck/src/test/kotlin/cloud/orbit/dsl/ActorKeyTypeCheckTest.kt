/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

import cloud.orbit.dsl.ast.TypeReference
import cloud.orbit.dsl.type.PrimitiveType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ActorKeyTypeCheckTest {
    private lateinit var check: ActorKeyTypeCheck
    private lateinit var errorReporter: TestErrorReporter

    @BeforeEach
    fun beforeEach() {
        check = ActorKeyTypeCheck()
        errorReporter = TestErrorReporter()
    }

    @Test
    fun noErrorWhenNotInActorKeyContext() {
        TypeCheck.Context.values().filter { it != TypeCheck.Context.ACTOR_KEY }.forEach {
            check.check(TypeReference("any"), it, errorReporter)
        }
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
    }

    @Test
    fun errorWhenTypeIsNotSupportedAsActorKeyType() {
        check.check(TypeReference("foo"), TypeCheck.Context.ACTOR_KEY, errorReporter)
        Assertions.assertEquals(
            listOf(
                "actor key type must be string, int32, int64, or guid; found 'foo'"
            ),
            errorReporter.errors
        )
    }
}
