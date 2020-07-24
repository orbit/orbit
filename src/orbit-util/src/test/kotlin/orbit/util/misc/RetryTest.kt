/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.util.misc

import io.kotlintest.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test

class RetryTest {
    @Test
    fun `will attempt multiple times`() {
        runBlocking {
            var count = 0
            retry {
                if (++count < 5) throw Exception("Fail")
            }

            count shouldBe 5
        }
    }

    @Test
    fun `will stop attempting after specified amount`() {
        var count = 0
        assertThrows<RetriesExceededException> {
            runBlocking {
                retry(attempts = 5) {
                    count++
                    throw Exception("Fail")
                }
            }
        }
        count shouldBe 5
    }
}