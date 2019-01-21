/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.common

import cloud.orbit.common.concurrent.Pools
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

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

        assertThat(count.get()).isEqualTo(5)
    }
}
