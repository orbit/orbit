/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.util.misc

import orbit.util.test.runTest
import orbit.util.time.Clock
import orbit.util.time.stopwatch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

@Suppress("IMPLICIT_NOTHING_AS_TYPE_PARAMETER")
class ControlFlowUtilsTest {

    @Test
    fun `check attempt success`() =
        runTest {
            val result = attempt {
                "Some result"
            }
            assertEquals(result, "Some result")
        }


    @Test
    fun `check attempt max attempts`() =
        runTest {
            var attempts = 0
            assertFails {
                attempt(
                    maxAttempts = 5,
                    initialDelay = 1
                ) {
                    attempts++
                    throw RuntimeException("FAIL")
                }

            }
            assertEquals(attempts, 5)

        }


    @Test
    fun `check attempt back off`() =
        runTest {
            val (elapsed, _) = stopwatch(Clock()) {
                assertFails {

                    attempt(
                        maxAttempts = 5,
                        initialDelay = 1,
                        factor = 2.0
                    ) {
                        throw RuntimeException("FAIL")
                    }
                }
            }
            assertTrue { elapsed > 10 }

        }

    @Test
    fun `check attempt max delay`() =
        runTest {
            val (elapsed, _) = stopwatch(Clock()) {

                assertFails {

                    attempt(
                        maxAttempts = 5,
                        maxDelay = 100,
                        initialDelay = 1,
                        factor = 1000.0
                    ) {
                        throw RuntimeException("FAIL")
                    }
                }
            }
            assertTrue { elapsed < 1000 }

        }

    @Test
    fun `check attempt success after fail`() =
        runTest {
            var attempts = 0
            val result = attempt(
                maxAttempts = 5,
                initialDelay = 1
            ) {
                if (attempts++ < 3) {
                    throw RuntimeException("FAIL")
                }
                "Hello"
            }

            assertEquals(result, "Hello")
        }
}