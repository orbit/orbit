/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.common

import cloud.orbit.common.time.Clock
import cloud.orbit.common.time.stopwatch
import cloud.orbit.common.util.attempt
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class FlowUtilsTest {

    @Test
    fun `check success`() {
        val result = runBlocking {
            attempt {
                "Some result"
            }
        }
        Assertions.assertThat(result).isEqualTo("Some result")
    }

    @Test
    fun `check max attempts`() {
        var attempts = 0
        Assertions.assertThatThrownBy {
            runBlocking {
                attempt(
                    maxAttempts = 5,
                    initialDelay = 1
                ) {
                    attempts++
                    throw RuntimeException("FAIL")
                }
            }
        }

        Assertions.assertThat(attempts).isEqualTo(5)
    }

    @Test
    fun `check back off`() {
        val (elapsed, _) = stopwatch(Clock()) {
            Assertions.assertThatThrownBy {
                runBlocking {
                    attempt(
                        maxAttempts = 5,
                        initialDelay = 1,
                        factor = 2.0
                    ) {
                        throw RuntimeException("FAIL")
                    }
                }
            }
        }

        Assertions.assertThat(elapsed).isGreaterThan(10)
    }

    @Test
    fun `check max delay`() {
        val (elapsed, _) = stopwatch(Clock()) {
            Assertions.assertThatThrownBy {
                runBlocking {
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
        }

        Assertions.assertThat(elapsed).isLessThan(1000)
    }

    @Test
    fun `check success after fail`() {
        var attempts = 0
        val result = runBlocking {
            attempt(
                maxAttempts = 5,
                initialDelay = 1
            ) {
                if(attempts++ < 3) {
                    throw RuntimeException("FAIL")
                }
                "Hello"
            }
        }

        Assertions.assertThat(result).isEqualTo("Hello")
    }
}