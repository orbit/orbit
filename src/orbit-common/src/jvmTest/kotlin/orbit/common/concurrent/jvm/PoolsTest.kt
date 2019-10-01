/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.common.concurrent.jvm

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

class PoolsTest {
    @Test
    fun `check pools behave as expected`() {
        val count = AtomicInteger(0)
        val cpuPool = Pools.createFixedPool("orbit-test-cpu")
        val ioPool = Pools.createCachedPool("orbit-test-io")

        count.incrementAndGet()

        runBlocking(cpuPool) {
            count.incrementAndGet()
        }

        runBlocking(ioPool) {
            count.incrementAndGet()
        }

        runBlocking {
            withContext(coroutineContext + cpuPool) {
                count.incrementAndGet()
            }

            withContext(coroutineContext + ioPool) {
                count.incrementAndGet()
            }
        }

        assertEquals(count.get(), 5)
    }
}