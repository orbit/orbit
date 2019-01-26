/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.common

import cloud.orbit.common.concurrent.Pools
import cloud.orbit.common.concurrent.atomicSet
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference

class AtomicReferenceTest {
    @Test
    fun `check set atomic once`() {
        val ref = AtomicReference(0)
        assertThat(ref.get()).isEqualTo(0)
        ref.atomicSet {
            it + 1
        }
        assertThat(ref.get()).isEqualTo(1)
    }

    @Test
    fun `check set atomic 100 times concurrently`() {
        runBlocking {
            val cpuPool = Pools.createFixedPool("orbit-atomref-test", 8)
            val ref = AtomicReference(0)
            val jobs = mutableListOf<Job>()

            repeat(100) {
                val job = launch(coroutineContext + cpuPool) {
                    ref.atomicSet { ref ->
                        ref + 1
                    }
                }
                jobs.add(job)
            }

            jobs.joinAll()

            assertThat(ref.get()).isEqualTo(100)
        }
    }
}