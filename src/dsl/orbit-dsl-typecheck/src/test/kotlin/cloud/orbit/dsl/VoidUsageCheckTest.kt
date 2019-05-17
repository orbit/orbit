/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

import cloud.orbit.dsl.ast.TypeReference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VoidUsageCheckTest {
    private lateinit var check: VoidUsageCheck
    private lateinit var errorReporter: TestErrorReporter

    @BeforeEach
    fun beforeEach() {
        check = VoidUsageCheck()
        errorReporter = TestErrorReporter()
    }

    @Test
    fun doesNotReportErrorOnNonVoidTypeReference() {
        check.check(TypeReference("any"), TypeCheck.Context.DATA_FIELD, errorReporter)

        assertTrue(errorReporter.errors.isEmpty())
    }

    @Test
    fun doesNotReportErrorOnActorKeyTypeReturn() {
        check.check(TypeReference("void"), TypeCheck.Context.ACTOR_KEY, errorReporter)

        assertTrue(errorReporter.errors.isEmpty())
    }

    @Test
    fun doesNotReportErrorOnMethodReturn() {
        check.check(TypeReference("void"), TypeCheck.Context.METHOD_RETURN, errorReporter)

        assertTrue(errorReporter.errors.isEmpty())
    }

    @Test
    fun reportsErrorOnIllegalUsage() {
        val typeCheckContexts = TypeCheck.Context.values().filter {
            it !in setOf(TypeCheck.Context.ACTOR_KEY, TypeCheck.Context.METHOD_RETURN)
        }

        typeCheckContexts.forEach {
            check.check(TypeReference("void"), it, errorReporter)
        }

        assertEquals(typeCheckContexts.size, errorReporter.errors.size)
        assertTrue(errorReporter.errors.all { it == "'void' can only be used as method return type" })
    }
}
