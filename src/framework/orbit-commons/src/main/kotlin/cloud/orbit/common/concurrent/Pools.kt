/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.common.concurrent

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

/**
 * Concurrent pool/scheduler creation and utilities.
 */
object Pools {

    /**
     * The ideal parallelism for this machine.
     *
     * Usually equal to hardware thread count but always at least 2.
     * The default value used for maxThreads in [createFixedPool] when no override is specified.
     */
    @JvmStatic
    val defaultParallelism
        get() = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)

    /**
     * Creates a fixed pool ideal for CPU intensive tasks.
     *
     * [maxThreads] defaults to [defaultParallelism] when no argument is specified.
     *
     * @param threadPrefix The prefix for the thread name.
     * @param maxThreads The max number of threads to be created. Must be at least 1.
     * @return The created pool.
     */
    @JvmStatic
    @JvmOverloads
    fun createFixedPool(threadPrefix: String, maxThreads: Int = defaultParallelism): CoroutineDispatcher {
        if (maxThreads <= 0) throw IllegalArgumentException("maxThreads must be at least 1")
        return FixedPool(threadPrefix, maxThreads)
    }

    /**
     * Creates a cached pool ideal for IO intensive tasks.
     * @param threadPrefix The prefix for the thread name.
     * @return The created pool.
     */
    @JvmStatic
    fun createCachedPool(threadPrefix: String): CoroutineDispatcher =
        CachedPool(threadPrefix)
}

private abstract class AbstractPool(private val threadPrefix: String) : CoroutineDispatcher() {
    @Volatile
    private var pool: ExecutorService? = null

    private val threadId = AtomicInteger(0)

    abstract fun createPool(): ExecutorService

    @Synchronized
    private fun getOrCreatePool() = pool ?: createPool().also { pool = it }

    fun dispatch(block: Runnable) {
        getOrCreatePool().execute(block)
        Dispatchers.IO
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        dispatch(block)
    }

    protected fun generateThreadName() = "$threadPrefix-${threadId.incrementAndGet()}"
}

private class CachedPool(threadPrefix: String) : AbstractPool(threadPrefix) {
    override fun createPool(): ExecutorService =
        Executors.newCachedThreadPool {
            Thread(it, generateThreadName())
        }
}

private class FixedPool(threadPrefix: String, private val parallelism: Int) : AbstractPool(threadPrefix) {
    override fun createPool(): ExecutorService =
        Executors.newFixedThreadPool(parallelism) {
            Thread(it, generateThreadName()).apply { isDaemon = true }
        }
}
