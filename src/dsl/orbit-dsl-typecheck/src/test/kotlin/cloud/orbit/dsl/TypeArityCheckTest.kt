/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

import cloud.orbit.dsl.ast.Type
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TypeArityCheckTest {
    private val errorListener = TypeErrorListener()
    private val typeArityCheck = TypeArityCheck(
        mapOf(
            "t0" to TypeDescriptor("t0", 0),
            "t1" to TypeDescriptor("t1", 1),
            "t2" to TypeDescriptor("t2", 2)
        )
    )

    init {
        typeArityCheck.addErrorListener(errorListener)
    }

    @Test
    fun noOpWhenTypeIsUnknown() {
        typeArityCheck.visitNode(Type("t"))

        assertTrue(errorListener.typeErrors.isEmpty())
    }

    @Test
    fun noErrorsWhenTypeArityIsCorrect() {
        typeArityCheck.visitNode(Type("t0"))
        typeArityCheck.visitNode(Type("t1", of = listOf(Type("t"))))
        typeArityCheck.visitNode(Type("t2", of = listOf(Type("t"), Type("t"))))

        assertTrue(errorListener.typeErrors.isEmpty())
    }

    @Test
    fun reportsErrorWhenTypeArityIsIncorrect() {
        typeArityCheck.visitNode(Type("t0", of = listOf(Type("t"))))
        typeArityCheck.visitNode(Type("t1"))
        typeArityCheck.visitNode(Type("t2", of = listOf(Type("t"))))

        val errorMessages = errorListener.typeErrors.map { it.message }
        assertTrue(errorMessages.contains("expected parameter count for type 't0' is 0, found 1"))
        assertTrue(errorMessages.contains("expected parameter count for type 't1' is 1, found 0"))
        assertTrue(errorMessages.contains("expected parameter count for type 't2' is 2, found 1"))
    }
}
