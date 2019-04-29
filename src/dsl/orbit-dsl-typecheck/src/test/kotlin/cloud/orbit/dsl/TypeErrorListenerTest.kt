/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

import cloud.orbit.dsl.ast.ParseContext
import cloud.orbit.dsl.ast.Type
import cloud.orbit.dsl.ast.annotated
import cloud.orbit.dsl.error.OrbitDslError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TypeErrorListenerTest {
    @Test
    fun reportsErrorWithParseContext() {
        val typeErrorListener = TypeErrorListener()
        typeErrorListener.onError(
            Type("t")
                .annotated(
                    ParseContext(
                        "path/to/file.orbit",
                        line = 2,
                        column = 17
                    )
                ),
            "error here"
        )

        assertEquals(
            OrbitDslError(
                "path/to/file.orbit",
                line = 2,
                column = 17,
                message = "error here"
            ),
            typeErrorListener.typeErrors.first()
        )
    }

    @Test
    fun reportsErrorWithUnknownParseContext() {
        val typeErrorListener = TypeErrorListener()
        typeErrorListener.onError(
            Type("t"),
            "error here"
        )

        assertEquals(
            OrbitDslError("<unknown>", line = 0, column = 0, message = "error here"),
            typeErrorListener.typeErrors.first()
        )
    }
}
