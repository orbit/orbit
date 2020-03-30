/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

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
import orbit.server.mesh.LeaseDuration
import orbit.server.mesh.LocalServerInfo
import orbit.server.mesh.local.LocalAddressableDirectory
import orbit.server.mesh.local.LocalNodeDirectory
import orbit.shared.mesh.NodeStatus
import orbit.shared.net.Message
import orbit.util.di.ComponentContainerRoot
import orbit.util.di.ExternallyConfigured
import orbit.util.time.Clock
import orbit.util.time.TimeMs
import org.junit.After
import java.time.Duration
import java.util.concurrent.TimeUnit

open class BaseServerTest {
    private var clock: Clock = Clock()
    private var servers: MutableList<OrbitServer> = mutableListOf()
    private var clients: MutableList<TestClient> = mutableListOf()

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
                    Duration.ofSeconds(addressableLeaseDurationSeconds)
                ),
                nodeLeaseDuration = LeaseDuration(
                    Duration.ofSeconds(nodeLeaseDurationSeconds),
                    Duration.ofSeconds(nodeLeaseDurationSeconds)
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

    suspend fun startClient(onReceive: (msg: Message) -> Unit = {}) : TestClient {
        val client = TestClient(onReceive).connect()

        clients.add(client)

        return client
    }

    fun disconnectClient(client: TestClient) {
        client.disconnect()
        clients.remove(client)
    }
}