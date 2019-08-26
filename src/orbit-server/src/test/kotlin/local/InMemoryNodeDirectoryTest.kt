package orbit.server.local

import LocalClientNode
import orbit.server.*
import orbit.server.net.Message
import orbit.server.net.NodeId
import orbit.server.routing.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class InMemoryNodeDirectoryTest {

    @Test
    fun `can find node connected to client`() {
        val directory = InMemoryNodeDirectory()
        val node = TestNode(NodeId("node1"))
        directory.connectNode(node)

        val connectedNode = LocalClientNode<Address>(NodeId("node2"), capabilities = listOf(Capability("test")))
        directory.connectNode(connectedNode, node.id)

        val nodes = directory.lookupConnectedNodes(connectedNode.id, Address())

        assertThat(node).isEqualTo(nodes.elementAt(0))
    }

    @Test
    fun `can find virtual mesh node from mesh node`() {
        val directory = InMemoryNodeDirectory()
        val node = TestNode(NodeId("node1"))
        directory.connectNode(node)

        val connectedNode = TestNode(NodeId("node2"), capabilities = listOf(Capability("test")))
        directory.connectNode(connectedNode)

        val nodes = directory.lookupConnectedNodes(connectedNode.id, Address())

        assertThat(Mesh.Instance).isEqualTo(nodes.elementAt(0))
    }

    @Test
    fun `cannot find current node`() {
        val directory = InMemoryNodeDirectory()
        val node = TestNode(NodeId("node1"))
        directory.connectNode(node)
        directory.connectNode(TestNode(NodeId("node2")))

        val nodes = directory.lookupConnectedNodes(node.id, Address())
        assertThat(nodes.toList()).doesNotContain(node)
    }

    @Test
    fun `can find client connected to node`() {
        val directory = InMemoryNodeDirectory()
        val node = TestNode(NodeId("node1"))
        directory.connectNode(node)
        val client = LocalClientNode<Address>(NodeId("client1"), capabilities = listOf(Capability("test")))
        directory.connectNode(client, node.id)

        val nodes = directory.lookupConnectedNodes(node.id, Address())
        assertThat(nodes.toList()).contains(client)
    }

    @Test
    fun `returns node connected to client`() {
        val directory = InMemoryNodeDirectory()
        val node = TestNode(NodeId("node1"))
        directory.connectNode(node)
        val client = LocalClientNode<Address>(NodeId("client1"), capabilities = listOf(Capability("test")))
        directory.connectNode(client, node.id)

        val nodes = directory.lookupConnectedNodes(client.id, Address())
        assertThat(nodes.toList()).contains(node)
    }

    @Test
    fun `does not return client if not connected to node`() {
        val directory = InMemoryNodeDirectory()
        val node = TestNode(NodeId("node1"))
        val node2 = TestNode(NodeId("node2"))

        directory.connectNode(node)
        directory.connectNode(node2)
        val client = LocalClientNode<Address>(NodeId("client1"))
        directory.connectNode(client, node.id)

        val nodes = directory.lookupConnectedNodes(node2.id, Address())
        assertThat(nodes.toList()).doesNotContain(client)
    }

    @Test
    fun `does not return unknown node connected to client`() {
        val directory = InMemoryNodeDirectory()
        val client = LocalClientNode<Address>(NodeId("client1"))
        directory.connectNode(client, NodeId("node2"))

        val nodes = directory.lookupConnectedNodes(client.id, Address())
        assertThat(nodes.map { n -> n.id }.toList()).doesNotContain(NodeId("node2"))
    }

    @Test
    fun `can find all mesh nodes from virtual mesh`() {
        val directory = InMemoryNodeDirectory()
        val node = TestNode()
        directory.connectNode(node)

        val nodes = directory.lookupConnectedNodes(Mesh.Instance.id, Address())
        assertThat(nodes.map { n -> n.id }.toList()).contains(node.id)
    }

    @Test
    fun `can register connections`() {
        val directory = InMemoryNodeDirectory()
        val node = TestNode()
        val connectedNode = TestNode()
        directory.connectNode(node)
        directory.connectNode(connectedNode)
        directory.reportConnections(node.id, listOf(connectedNode.id))

        val nodes = directory.lookupConnectedNodes(node.id, Address())
        assertThat(nodes.map { n -> n.id }.toList()).contains(connectedNode.id)
    }

    @Test
    fun `can disconnect a node connection`() {
        val directory = InMemoryNodeDirectory()
        val node = TestNode()
        val connectedNode = TestNode()
        directory.connectNode(connectedNode)
        directory.connectNode(node, connectedNode.id)
        directory.reportConnections(node.id, listOf())

        val nodes = directory.lookupConnectedNodes(node.id, Address())
        assertThat(nodes.map { n -> n.id }.toList()).doesNotContain(connectedNode.id)
    }

    fun Address(): Address {
        return Address(AddressId("test"), Capability("test"))
    }

    class TestNode(
        override val id: NodeId = NodeId.generate(),
        override val capabilities: List<Capability> = listOf(Capability.Routing)
    ) : MeshNode {
        override fun <T : Address> canHandle(address: T): Boolean {
            return true
        }

        override fun sendMessage(message: Message, route: Route?) {

        }

    }
}
