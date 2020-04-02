/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client

import io.kotlintest.eventually
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MockClock
import io.micrometer.core.instrument.simple.SimpleConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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
    private var clock: Clock = Clock()
    private var servers: MutableList<OrbitServer> = mutableListOf()
    private var clients: MutableList<OrbitClient> = mutableListOf()

    protected lateinit var server: OrbitServer
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
            server = startServer()
            client = startClient()
        }
    }

    @After
    fun afterTest() {
        runBlocking {
            clients.toList().forEach { client -> disconnectClient(client) }
            delay(100)
            servers.toList().forEach { server -> disconnectServer(server) }
            LocalNodeDirectory.clear()
            LocalAddressableDirectory.clear()
            Metrics.globalRegistry.clear()
            clock = Clock()
        }
    }

    fun startServer(
        port: Int = 50056,
        addressableLeaseDurationSeconds: Long = 5,
        nodeLeaseDurationSeconds: Long = 10,
        containerOverrides: ComponentContainerRoot.() -> Unit = { }
    ): OrbitServer {
        val server = OrbitServer(
            OrbitServerConfig(
                serverInfo = LocalServerInfo(
                    port = port,
                    url = "localhost:${port}"
                ),
                meterRegistry = MockMeterRegistry.Config,
                addressableLeaseDuration = LeaseDuration(
                    Duration.ofSeconds(addressableLeaseDurationSeconds),
                    Duration.ofSeconds(addressableLeaseDurationSeconds / 2)
                ),
                nodeLeaseDuration = LeaseDuration(
                    Duration.ofSeconds(nodeLeaseDurationSeconds),
                    Duration.ofSeconds(nodeLeaseDurationSeconds / 2)
                ),
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
        platformExceptions: Boolean = false
    ): OrbitClient {
        val client = OrbitClient(
            OrbitClientConfig(
                grpcEndpoint = "dns:///localhost:${port}",
                namespace = namespace,
                packages = packages,
                platformExceptions = platformExceptions
            )
        )

        client.start().join()
        clients.add(client)

        return client
    }

    fun disconnectClient(client: OrbitClient) {
        client.stop()
        clients.remove(client)
    }
}