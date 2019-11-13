/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.benchmark

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import orbit.client.OrbitClient
import orbit.client.OrbitClientConfig
import orbit.client.actor.AbstractActor
import orbit.client.actor.ActorWithInt32Key
import orbit.client.actor.createProxy
import orbit.client.net.OrbitServiceLocator
import orbit.server.OrbitServer
import orbit.server.OrbitServerConfig
import orbit.server.mesh.LocalServerInfo
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
import java.net.URI
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
    private val targetUri = URI.create("orbit://localhost:5899/test")

    private lateinit var server: OrbitServer
    private lateinit var client: OrbitClient
    private val actors = ArrayList<BasicBenchmarkActor>()

    @Setup
    fun setup() {
        val serverConfig = OrbitServerConfig(
            LocalServerInfo(
                port = targetUri.port,
                url = targetUri.toString()
            )
        )
        val clientConfig =
            OrbitClientConfig(
                serviceLocator = OrbitServiceLocator(targetUri),
                packages = listOf("orbit.benchmark")
            )

        server = OrbitServer(serverConfig)
        client = OrbitClient(clientConfig)

        runBlocking {
            server.start().join()
            client.start().join()

            repeat(ACTOR_POOL) {
                val actor = client.actorFactory.createProxy<BasicBenchmarkActor>(it)
                actor.echo("Chevron $it encoded...").await()
                actors.add(actor)
            }
        }
    }

    @Benchmark
    @Threads(8)
    @OperationsPerInvocation(REQUESTS_PER_BATCH)
    fun echoThroughputBenchmark() = batchIteration()

    @Benchmark
    @Threads(8)
    @OperationsPerInvocation(REQUESTS_PER_BATCH)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    fun echoTimingBenchmark() = batchIteration()

    private fun batchIteration() {
        val myList = ArrayList<Deferred<String>>(REQUESTS_PER_BATCH)
        repeat(REQUESTS_PER_BATCH) {
            val actor = actors.random()
            myList.add(actor.echo("Chevron $it locked."))
        }
        runBlocking {
            myList.awaitAll()
        }
    }

    @TearDown
    fun teardown() {
        runBlocking {
            client.stop().join()
            server.stop().join()
        }
    }
}