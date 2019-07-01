/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.type.check

import cloud.orbit.dsl.ast.ErrorReporter
import cloud.orbit.dsl.ast.TypeReference
import io.mockk.called
import io.mockk.confirmVerified
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class VoidUsageCheckTest {
    private val check = VoidUsageCheck()

    @MockK(relaxUnitFun = true)
    private lateinit var errorReporter: ErrorReporter

    @Test
    fun doesNotReportErrorOnNonVoidTypeReference() {
        check.check(TypeReference("any"), TypeCheck.Context.DATA_FIELD, errorReporter)

        verify { errorReporter wasNot called }
        confirmVerified(errorReporter)
    }

    @Test
    fun doesNotReportErrorOnActorKeyType() {
        check.check(TypeReference("void"), TypeCheck.Context.ACTOR_KEY, errorReporter)

        verify { errorReporter wasNot called }
        confirmVerified(errorReporter)
    }

    @Test
    fun doesNotReportErrorOnMethodReturn() {
        check.check(TypeReference("void"), TypeCheck.Context.METHOD_RETURN, errorReporter)

        verify { errorReporter wasNot called }
        confirmVerified(errorReporter)
    }

    @Test
    fun reportsErrorOnIllegalUsage() {
        val typeCheckContexts = TypeCheck.Context.values().filter {
            it !in setOf(TypeCheck.Context.ACTOR_KEY, TypeCheck.Context.METHOD_RETURN)
        }

        typeCheckContexts.forEach {
            check.check(TypeReference("void"), it, errorReporter)
        }

        verify(exactly = typeCheckContexts.size) {
            errorReporter.reportError(
                TypeReference("void"),
                "'void' can only be used as method return type"
            )
        }
        confirmVerified(errorReporter)
    }
}
