/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.util.concurrent

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import mu.KLogger

class RailWorker<T>(
    private val scope: CoroutineScope,
    private val buffer: Int = 10_000,
    private val railCount: Int = 128,
    private val logger: KLogger? = null,
    autoStart: Boolean = false,
    private val onMessage: suspend (T) -> Unit
) {
    private var channel: Channel<T>? = null
    private var workers: List<Job>? = null

    init {
        if (autoStart) startWorkers()
    }

    val isInitialized get() = workers != null && channel != null

    suspend fun send(msg: T) = channel?.send(msg) ?: throw IllegalStateException("Rail worker is not initialized.")

    fun offer(msg: T) = channel?.offer(msg) ?: throw IllegalStateException("Rail worker is not initialized.")

    fun startWorkers() {
        channel = Channel<T>(buffer).also { chan ->
            workers = List(railCount) {
                scope.launch {
                    for (msg in chan) {
                        try {
                            onMessage(msg)
                        } catch (e: Throwable) {
                            logger?.warn { "Error: Exception caught in rail worker ${e}" }
                        }
                    }
                }
            }
        }

        logger?.info {
            "Started a rail worker with $railCount rails and a $buffer entry buffer."
        }
    }

    fun stopWorkers() {
        channel?.close()
        workers?.forEach {
            it.cancel()
        }
        channel = null
        workers = null
    }
}