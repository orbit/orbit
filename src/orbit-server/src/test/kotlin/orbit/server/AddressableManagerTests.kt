/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

import io.kotlintest.shouldBe
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import orbit.server.mesh.AddressableDirectory
import orbit.server.mesh.AddressableManager
import orbit.server.mesh.ClusterManager
import orbit.server.mesh.LeaseDuration
import orbit.server.mesh.NodeDirectory
import orbit.server.mesh.local.LocalAddressableDirectory
import orbit.server.mesh.local.LocalNodeDirectory
import orbit.shared.addressable.AddressableReference
import orbit.shared.addressable.Key
import orbit.shared.mesh.NodeCapabilities
import orbit.shared.mesh.NodeInfo
import orbit.shared.mesh.NodeLease
import orbit.shared.mesh.NodeStatus
import orbit.util.time.Clock
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class AddressableManagerTests {
    lateinit var clusterManager: ClusterManager
    val clock: Clock = Clock()
    val nodeDirectory: NodeDirectory = LocalNodeDirectory(clock)
    lateinit var addressableManager: AddressableManager

    private fun join(namespace: String = "test", addressableType: String = "testActor"): NodeInfo {
        return runBlocking {
            clusterManager.joinCluster(
                namespace,
                NodeCapabilities(listOf(addressableType)),
                nodeStatus = NodeStatus.ACTIVE
            )
        }
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun SetupClass() {
            Metrics.globalRegistry.add(SimpleMeterRegistry())
        }
    }

    @Before
    fun BeforeTest() {
        initialize()
    }

    fun initialize(nodeLeaseDurationSeconds: Long = 10L) {
        val config = OrbitServerConfig(nodeLeaseDuration = LeaseDuration(nodeLeaseDurationSeconds))
        clock.resetToNow()
        LocalNodeDirectory.clear()
        LocalAddressableDirectory.clear()

        clusterManager = ClusterManager(config, clock, nodeDirectory)
        addressableManager = AddressableManager(
            LocalAddressableDirectory(clock),
            clusterManager,
            clock,
            config
        )
    }

    private suspend fun iterateTest(
        test: suspend () -> Unit
    ) {
        repeat(100) {
            test()
            LocalAddressableDirectory.clear()
        }
    }

    @Test
    fun `Addressable is placed on available node`() {
        runBlocking {
            val address = AddressableReference("testActor", Key.StringKey("a"))
            val node = join("test")
            val placedNode = addressableManager.locateOrPlace("test", address)

            placedNode shouldBe node.id
        }
    }

    @Test
    fun `Addressable should be placed only on node in namespace`() {
        runBlocking {
            val address = AddressableReference("testActor", Key.StringKey("a"))
            val nodes = mapOf(
                1 to join("test"),
                2 to join("test2")
            )
            iterateTest {
                addressableManager.locateOrPlace("test", address) shouldBe nodes[1]!!.id
            }
        }
    }

    @Test
    fun `Addressable should be placed only on node with matching capability`() {
        runBlocking {
            val address = AddressableReference("testActor", Key.StringKey("a"))
            val nodes = mapOf(
                1 to join(addressableType = "testActor"),
                2 to join(addressableType = "invalidActor")
            )

            iterateTest {
                addressableManager.locateOrPlace("test", address) shouldBe nodes[1]!!.id
            }
        }
    }

    @Test
    fun `Addressable should not be placed on removed node`() {
        runBlocking {
            val address = AddressableReference("testActor", Key.StringKey("a"))
            val nodes = mapOf(
                1 to join(),
                2 to join()
            )

            nodeDirectory.remove(nodes[1]!!.id)
            clusterManager.tick()

            iterateTest {
                addressableManager.locateOrPlace("test", address) shouldBe nodes[2]!!.id
            }
        }
    }

    @Test
    fun `Addressable should not be placed on a draining node`() {
        runBlocking {
            val address = AddressableReference("testActor", Key.StringKey("a"))
            val nodes = mapOf(
                1 to join(),
                2 to join()
            )

            clusterManager.updateNode(nodes[1]!!.id) {
                it!!.copy(
                    nodeStatus = NodeStatus.DRAINING
                )
            }

            iterateTest {
                addressableManager.locateOrPlace("test", address) shouldBe nodes[2]!!.id
            }
        }
    }

    @Test
    fun `Addressable should be replaced from expired node`() {
        runBlocking {
            val address = AddressableReference("testActor", Key.StringKey("a"))
            val nodes = mapOf(
                1 to join(),
                2 to join()
            )

            clusterManager.updateNode(nodes[1]!!.id) {
                it!!.copy(
                    lease = NodeLease.expired
                )
            }

            iterateTest {
                addressableManager.locateOrPlace("test", address) shouldBe nodes[2]!!.id
            }
        }
    }

    @Test
    fun `Ineligible nodes are not included in placement`() {
        runBlocking {
            val address = AddressableReference("testActor", Key.StringKey("a"))
            val node1 = join("test")
            val node2 = join("test")
            val node3 = join("test")

            iterateTest {
                addressableManager.locateOrPlace("test", address, listOf(node2.id, node3.id)) shouldBe node1.id
            }
        }
    }


}