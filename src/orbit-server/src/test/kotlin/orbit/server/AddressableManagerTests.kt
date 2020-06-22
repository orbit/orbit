/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import orbit.server.mesh.AddressableManager
import orbit.server.mesh.ClusterManager
import orbit.server.mesh.LeaseDuration
import orbit.server.mesh.NodeDirectory
import orbit.server.mesh.local.LocalAddressableDirectory
import orbit.server.mesh.local.LocalNodeDirectory
import orbit.shared.addressable.AddressableReference
import orbit.shared.addressable.Key
import orbit.shared.addressable.NamespacedAddressableReference
import orbit.shared.mesh.NodeCapabilities
import orbit.shared.mesh.NodeId
import orbit.shared.mesh.NodeInfo
import orbit.shared.mesh.NodeLease
import orbit.shared.mesh.NodeStatus
import orbit.util.time.Clock
import orbit.util.time.Timestamp
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class AddressableManagerTests {
    lateinit var clusterManager: ClusterManager
    private val clock: Clock = Clock()
    private val nodeDirectory: NodeDirectory = LocalNodeDirectory(clock)
    private val addressableDirectory = LocalAddressableDirectory(clock)

    private lateinit var addressableManager: AddressableManager

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
            addressableDirectory,
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
            val nodes = mapOf(
                1 to join("test"),
                2 to join("test"),
                3 to join("test")
            )

            iterateTest {
                addressableManager.locateOrPlace("test", address, listOf(nodes[2]!!.id, nodes[3]!!.id)) shouldBe nodes[1]!!.id
            }
        }
    }

    @Test
    fun `Abandoning an addressable lease should remove it from addressable directory`() {
        runBlocking {
            val address = AddressableReference("testActor", Key.StringKey("a"))
            val node = join("test")
            addressableManager.locateOrPlace("test", address)
            addressableDirectory.get(NamespacedAddressableReference("test", address)) shouldNotBe null

            addressableManager.abandonLease(address, node.id) shouldBe true
            addressableDirectory.get(NamespacedAddressableReference("test", address)) shouldBe null
        }
    }

    @Test
    fun `Abandoning an addressable lease not assigned to node is ignored`() {
        runBlocking {
            val address = AddressableReference("testActor", Key.StringKey("a"))
            join("test")
            addressableManager.locateOrPlace("test", address)
            val node2 = join("test")
            addressableManager.abandonLease(address, node2.id) shouldBe false
        }
    }

    @Test
    fun `Abandoning a non-existant addressable lease is ignored`() {
        runBlocking {
            val address = AddressableReference("testActor", Key.StringKey("a"))
            addressableManager.abandonLease(address, NodeId("node", "test")) shouldBe false
        }
    }

    @Test
    fun `Abandoning an expired lease doesn't remove from directory`() {
        runBlocking {
            val address = AddressableReference("testActor", Key.StringKey("a"))
            join("test")
            addressableManager.locateOrPlace("test", address)
            val reference = NamespacedAddressableReference("test", address)
            val lease = addressableDirectory.get(reference)!!
            addressableDirectory.compareAndSet(reference, lease, lease.copy(expiresAt = Timestamp(0, 0)))

            addressableManager.abandonLease(address, NodeId("node", "test")) shouldBe false
            addressableDirectory.get(reference) shouldNotBe null
        }
    }
}
