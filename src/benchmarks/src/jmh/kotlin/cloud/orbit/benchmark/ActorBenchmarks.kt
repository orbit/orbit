/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.benchmark

import cloud.orbit.core.actor.AbstractActor
import cloud.orbit.core.actor.ActorWithInt32Key
import cloud.orbit.core.actor.createProxy
import cloud.orbit.runtime.stage.Stage
import cloud.orbit.runtime.stage.StageConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OperationsPerInvocation
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.annotations.Threads
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit

private const val REQUESTS_PER_BATCH = 500
private const val ACTOR_POOL = 1000


interface BasicBenchmarkActor : ActorWithInt32Key {
    fun echo(string: String): Deferred<String>
}

class BasicBenchmarkActorImpl : BasicBenchmarkActor, AbstractActor() {
    override fun echo(string: String): Deferred<String> {
        return CompletableDeferred(string)
    }
}

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 5)
@Measurement(iterations = 20)
open class ActorBenchmarks {
    private var stage: Stage? = null
    private val actors = ArrayList<BasicBenchmarkActor>()

    @Setup
    fun setup() {
        val stageConfig = StageConfig(
            pipelineRailCount = 16,
            allowLoopback = false
        )
        stage = Stage(stageConfig)
        runBlocking {
            stage!!.start().await()

            repeat(ACTOR_POOL) {
                val actor = stage!!.actorProxyFactory.createProxy<BasicBenchmarkActor>(it)
                actor.echo("warmup$it").await()
                actors.add(actor)
            }
        }
    }

    @Benchmark
    @Threads(8)
    @OperationsPerInvocation(REQUESTS_PER_BATCH)
    fun echoThroughputBenchmark() {
        batchIteration()
    }

    @Benchmark
    @Threads(8)
    @OperationsPerInvocation(REQUESTS_PER_BATCH)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    fun echoTimingBenchmark() {
        batchIteration()
    }

    private fun batchIteration() {
        val myList = ArrayList<Deferred<String>>(REQUESTS_PER_BATCH)
        repeat(REQUESTS_PER_BATCH) {
            val actor = actors.random()
            myList.add(actor.echo("call$it"))
        }
        runBlocking {
            myList.awaitAll()
        }
    }

    @TearDown
    fun teardown() {
        runBlocking {
            stage!!.stop().await()
        }
    }
}