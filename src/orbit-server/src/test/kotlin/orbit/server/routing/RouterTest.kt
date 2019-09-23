package orbit.server.routing

import orbit.server.addressable.AddressableReference
import orbit.server.net.Message
import orbit.server.net.NodeId

internal class RouterTest {

//    @Test
//    fun `can route message to client node`() {
//
//        val nodeDirectory = InMemoryNodeDirectory()
//        val addressDirectory = InMemoryAddressableDirectory()
//
//        val accountAddress = Address()
//        val message = Message(MessageContent.Request("This is a test message", accountAddress))
//        val router = Router(NodeId("node1"), addressDirectory, nodeDirectory, TestAddressablePlacementStrategy())
//
//        nodeDirectory.connectNode(TestRemoteNode(NodeId("node1")))
//        nodeDirectory.connectNode(TestRemoteNode(NodeId("node2")));
//        nodeDirectory.connectNode(
//            LocalClientNode<Address>(NodeId("client"), listOf(Capability("test"))),
//            NodeId("node2")
//        )
//
//        addressDirectory.setLocation(accountAddress, NodeId("client"))
//
//        val route = router.getRoute(message)
//
//        assertThat(route?.path).containsExactly(
//            Mesh.Instance.id,
//            NodeId("node2"),
//            NodeId("client")
//        )
//    }
//
//    @Test
//    fun `when addressable not in node, uses strategy to pick new node`() {
//        val nodeDirectory = InMemoryNodeDirectory()
//        val addressDirectory = InMemoryAddressableDirectory()
//
//        val accountAddress = Address()
//        val message = Message(MessageContent.Request("This is a test message", accountAddress))
//        val router = Router(
//            NodeId("node1"), addressDirectory, nodeDirectory,
//            TestAddressablePlacementStrategy(NodeId("client"))
//        )
//        nodeDirectory.connectNode(TestRemoteNode(NodeId("node1")))
//        nodeDirectory.connectNode(TestRemoteNode(NodeId("node2")));
//        nodeDirectory.connectNode(
//            LocalClientNode<Address>(NodeId("client"), listOf(Capability("test"))),
//            NodeId("node2")
//        )
//
//        val route = router.getRoute(message)
//
//        assertThat(route?.path).containsExactly(
//            Mesh.Instance.id,
//            NodeId("node2"),
//            NodeId("client")
//        )
//    }
//
//    @Test
//    fun `when valid projected route is supplied, prefers that route`() {
//        val nodeDirectory = InMemoryNodeDirectory()
//        val addressDirectory = InMemoryAddressableDirectory()
//
//        val accountAddress = Address()
//        val message = Message(MessageContent.Request("This is a test message", accountAddress))
//        val router = Router(NodeId("node1"), addressDirectory, nodeDirectory, TestAddressablePlacementStrategy())
//
//        nodeDirectory.connectNode(TestRemoteNode(NodeId("node1")))
//        nodeDirectory.connectNode(TestRemoteNode(NodeId("node2")));
//        nodeDirectory.connectNode(TestRemoteNode(NodeId("node3")));
//        nodeDirectory.connectNode(
//            LocalClientNode<Address>(NodeId("client"), listOf(Capability("test"))),
//            NodeId("node3")
//        )
//
//        addressDirectory.setLocation(accountAddress, NodeId("client"))
//
//        val projectedRoute = Route(
//            listOf(
//                NodeId("node1"),
//                NodeId("mesh"),
//                NodeId("node2"),
//                NodeId("mesh"),
//                NodeId("node3"),
//                NodeId("client")
//            )
//        )
//        val route = router.getRoute(message, projectedRoute)
//
//        assertThat(route?.path).containsExactlyElementsOf(projectedRoute.pop().route.path)
//    }
//
//    @Test
//    fun `when invalid projected route is supplied, finds new route`() {
//        val nodeDirectory = InMemoryNodeDirectory()
//        val addressDirectory = InMemoryAddressableDirectory()
//
//        val accountAddress = Address()
//        val message = Message(MessageContent.Request("This is a test message", accountAddress))
//        val router = Router(NodeId("node1"), addressDirectory, nodeDirectory, TestAddressablePlacementStrategy())
//
//        nodeDirectory.connectNode(TestRemoteNode(NodeId("node1")))
//        nodeDirectory.connectNode(TestRemoteNode(NodeId("node2")));
//        nodeDirectory.connectNode(TestRemoteNode(NodeId("node3")));
//        nodeDirectory.connectNode(
//            LocalClientNode<Address>(NodeId("client"), listOf(Capability("test"))),
//            NodeId("node3")
//        )
//
//        addressDirectory.setLocation(accountAddress, NodeId("client"))
//
//        val route = router.getRoute(
//            message, Route(
//                listOf(
//                    NodeId("node1"),
//                    NodeId("client")
//                )
//            )
//        )
//
//        assertThat(route?.path).containsExactly(
//            Mesh.Instance.id,
//            NodeId("node3"),
//            NodeId("client")
//        )
//
//    }

    fun Address(): AddressableReference {
        return AddressableReference("user", "0")
    }

    class TestAddressablePlacementStrategy(val selectedNode: NodeId = NodeId("")) : AddressablePlacementStrategy {
        suspend override fun chooseNode(address: AddressableReference): NodeId {
            return selectedNode
        }
    }

    class TestRemoteNode(override val id: NodeId) : MeshNode {

        suspend override fun sendMessage(message: Message, route: Route?) {
            println("Sending message on Node ${id}: ${message.content}")
        }
    }
}
