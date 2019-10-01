/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.common.concurrent.jvm

import java.util.concurrent.CountDownLatch

/**
 * Prevents the JVM from shutting down by creating a thread blocked on a latch.
 */
class ShutdownLatch {
    private var latch = CountDownLatch(1)
    private lateinit var thread: Thread

    /**
     * Acquires the latch thread and blocks shutdown.
     */
    fun acquire() {
        thread = Thread(Runnable {
            latch.await()
        }).also {
            it.name = "orbit-shutdown-latch"
            it.isDaemon = false
            it.start()
        }
    }

    /**
     * Releases the latch thread and allows shutdown.
     */
    fun release() {
        latch.countDown()
    }
}