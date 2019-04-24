/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class OrbitDslSyntaxErrorTest {
    @Test
    fun formatsErrorMessage() {
        Assertions.assertEquals(
            "error: com/example/test.orbit:42:5: syntax error",
            OrbitDslSyntaxError(
                "com/example/test.orbit", line = 42, column = 5, message = "syntax error"
            ).errorMessage
        )
    }
}
