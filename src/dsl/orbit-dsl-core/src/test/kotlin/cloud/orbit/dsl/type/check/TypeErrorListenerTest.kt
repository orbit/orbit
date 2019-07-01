/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.type.check

import cloud.orbit.dsl.ast.AstNode
import cloud.orbit.dsl.ast.ParseContext
import cloud.orbit.dsl.ast.TypeReference
import cloud.orbit.dsl.error.OrbitDslError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TypeErrorListenerTest {
    @Test
    fun reportsErrorWithParseContext() {
        val typeErrorListener = TypeErrorListener()
        typeErrorListener.onError(
            TypeReference(
                "t",
                context = AstNode.Context(
                    ParseContext(
                        "path/to/file.orbit",
                        line = 2,
                        column = 17
                    )
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
            TypeReference("t"),
            "error here"
        )

        assertEquals(
            OrbitDslError("", line = 0, column = 0, message = "error here"),
            typeErrorListener.typeErrors.first()
        )
    }
}
