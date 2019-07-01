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
import io.mockk.verifySequence
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class TypeArityCheckTest {
    private val context = TypeCheck.Context.values().random()

    private val typeArityCheck = TypeArityCheck(
        mapOf(
            "t0" to TypeDescriptor("t0", 0),
            "t1" to TypeDescriptor("t1", 1),
            "t2" to TypeDescriptor("t2", 2)
        )
    )

    @MockK(relaxUnitFun = true)
    lateinit var errorReporter: ErrorReporter

    @Test
    fun noErrorWhenTypeIsUnknown() {
        typeArityCheck.check(TypeReference("t"), context, errorReporter)

        verify { errorReporter wasNot called }
        confirmVerified(errorReporter)
    }

    @Test
    fun noErrorWhenTypeArityIsCorrect() {
        typeArityCheck.check(TypeReference(name = "t0"), context, errorReporter)
        typeArityCheck.check(
            TypeReference(
                name = "t1",
                of = listOf(
                    TypeReference("t")
                )
            ),
            context,
            errorReporter
        )
        typeArityCheck.check(
            TypeReference(
                name = "t2",
                of = listOf(
                    TypeReference("t"),
                    TypeReference("t")
                )
            ),
            context,
            errorReporter
        )

        verify { errorReporter wasNot called }
        confirmVerified(errorReporter)
    }

    @Test
    fun reportsErrorWhenTypeArityIsIncorrect() {
        val t0Ref = TypeReference("t0", of = listOf(TypeReference("t")))
        val t1Ref = TypeReference("t1")
        val t2Ref = TypeReference("t2", of = listOf(TypeReference("t")))

        typeArityCheck.check(t0Ref, context, errorReporter)
        typeArityCheck.check(t1Ref, context, errorReporter)
        typeArityCheck.check(t2Ref, context, errorReporter)

        verifySequence {
            errorReporter.reportError(t0Ref, "expected parameter count for type 't0' is 0, found 1")
            errorReporter.reportError(t1Ref, "expected parameter count for type 't1' is 1, found 0")
            errorReporter.reportError(t2Ref, "expected parameter count for type 't2' is 2, found 1")
        }
    }
}
