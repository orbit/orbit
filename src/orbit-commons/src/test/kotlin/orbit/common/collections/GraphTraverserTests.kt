/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.common.collections

import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.*
import kotlin.math.max

class GraphTraverserTests {

    @Test
    fun `can traverse nodes breadth first`() {
        val rootNode = TestNode("1")

        rootNode.children.addAll(listOf(TestNode("1.1"), TestNode("1.2")))
        rootNode.children.elementAt(0).children.addAll(listOf(TestNode("1.1.1"), TestNode("1.1.2"), TestNode("1.1.3")))
        rootNode.children.elementAt(0).children.elementAt(2).children.addAll(listOf(TestNode("1.1.3.1"), TestNode("1.1.3.2"), TestNode("1.1.3.3")))
        rootNode.children.elementAt(1).children.addAll(listOf(TestNode("1.2.1"), TestNode("1.2.2")))
        rootNode.children.elementAt(1).children.elementAt(1).children.addAll(listOf(TestNode("1.2.2.1"), TestNode("1.2.2.2")))

        val traversal = GraphTraverser<TestNode> { node -> node.children.asSequence() }

        var highWater = 0
        traversal.traverse(rootNode).take(30).toList().forEach { node ->
            println("~# ${node.parent?.id} -> ${node.child.id}")
            assertThat(node.child.id.length).isGreaterThanOrEqualTo(highWater)
            highWater = max(highWater, node.child.id.length)
        }
    }

    class TestNode(val id: String) {
        val children = HashSet<TestNode>()
    }
}