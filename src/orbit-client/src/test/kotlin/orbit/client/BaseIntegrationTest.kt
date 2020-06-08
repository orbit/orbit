/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client

import io.kotlintest.eventually
import io.kotlintest.minutes
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MockClock
import io.micrometer.core.instrument.simple.SimpleConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import orbit.client.actor.TrackingGlobals
import orbit.client.execution.AddressableDeactivator
import orbit.server.OrbitServer
import orbit.server.OrbitServerConfig
import orbit.server.mesh.LeaseDuration
import orbit.server.mesh.LocalServerInfo
import orbit.server.mesh.local.LocalAddressableDirectory
import orbit.server.mesh.local.LocalNodeDirectory
import orbit.shared.mesh.NodeStatus
import orbit.util.di.ComponentContainerRoot
import orbit.util.di.ExternallyConfigured
import orbit.util.time.Clock
import orbit.util.time.TimeMs
import org.junit.After
import org.junit.Before
import java.time.Duration
import java.util.concurrent.TimeUnit

open class BaseIntegrationTest {
    protected var clock: Clock = Clock()
    private var servers: MutableList<OrbitServer> = mutableListOf()
    private var clients: MutableList<OrbitClient> = mutableListOf()

    protected lateinit var client: OrbitClient

    class MockMeterRegistry : SimpleMeterRegistry(SimpleConfig.DEFAULT, MockClock()) {
        object Config : ExternallyConfigured<MeterRegistry> {
            override val instanceType = MockMeterRegistry::class.java
        }

        fun advanceTime(timeMs: TimeMs) {
            (this.clock as MockClock).add(timeMs, TimeUnit.MILLISECONDS)
        }
    }

    fun advanceTime(duration: Duration) {
        clock.advanceTime(duration.toMillis())
        Metrics.globalRegistry.registries.forEach { r -> (r as? MockMeterRegistry)?.advanceTime(duration.toMillis()) }
    }

    @Before
    fun beforeTest() {
        runBlocking {
            startServer()
            client = startClient()
        }
    }

    @After
    fun afterTest() {
        runBlocking {
            clients.toList().forEach { client -> disconnectClient(client) }
            servers.toList().forEach { server -> disconnectServer(server) }
            LocalNodeDirectory.clear()
            LocalAddressableDirectory.clear()
            Metrics.globalRegistry.clear()
            TrackingGlobals.reset()
            clock = Clock()
        }
    }

    fun startServer(
        port: Int = 50056,
        addressableLeaseDurationSeconds: Long = 10,
        nodeLeaseDurationSeconds: Long = 600,
        tickRate: Duration = 1.seconds,
        containerOverrides: ComponentContainerRoot.() -> Unit = { }
    ): OrbitServer {
        val server = OrbitServer(
            OrbitServerConfig(
                serverInfo = LocalServerInfo(
                    port = port,
                    url = "localhost:${port}"
                ),
                meterRegistry = MockMeterRegistry.Config,
                addressableLeaseDuration = LeaseDuration(addressableLeaseDurationSeconds),
                nodeLeaseDuration = LeaseDuration(nodeLeaseDurationSeconds),
                tickRate = tickRate,
                clock = clock,
                containerOverrides = containerOverrides
            )
        )

        server.start()

        eventually(10.seconds) {
            server.nodeStatus shouldBe NodeStatus.ACTIVE
        }

        servers.add(server)
        return server
    }

    fun disconnectServer(server: OrbitServer?) {
        if (server == null) {
            return
        }

        server.stop()

        eventually(10.seconds) {
            server.nodeStatus shouldBe NodeStatus.STOPPED
        }

        servers.remove(server)
    }

    suspend fun startClient(
        port: Int = 50056,
        namespace: String = "test",
        packages: List<String> = listOf("orbit.client.actor"),
        platformExceptions: Boolean = false,
        addressableDeactivation: ExternallyConfigured<AddressableDeactivator> = AddressableDeactivator.Instant.Config()
    ): OrbitClient {

        val client = OrbitClient(
            OrbitClientConfig(
                grpcEndpoint = "dns:///localhost:${port}",
                namespace = namespace,
                packages = packages,
                clock = clock,
                platformExceptions = platformExceptions,
                addressableTTL = 1.minutes,
                addressableDeactivator = addressableDeactivation
            )
        )

        client.start().join()
        clients.add(client)

        return client
    }

    suspend fun disconnectClient(client: OrbitClient = this.client, deactivator: AddressableDeactivator? = null) {
        client.stop(deactivator).join()
        clients.remove(client)
    }
}