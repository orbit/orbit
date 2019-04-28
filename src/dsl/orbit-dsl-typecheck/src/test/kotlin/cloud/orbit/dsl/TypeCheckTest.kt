/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

import cloud.orbit.dsl.ast.Type
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TypeCheckTest {
    @Test
    fun doesNotVisitTypeParameters() {
        val typeCheck = object : TypeCheck() {
            val visitedTypeNames = mutableListOf<String>()

            override fun check(type: Type) {
                visitedTypeNames.add(type.name)
            }
        }

        typeCheck.visitNode(Type("t1", of = listOf(Type("t2"))))

        assertEquals(listOf("t1"), typeCheck.visitedTypeNames)
    }
}
