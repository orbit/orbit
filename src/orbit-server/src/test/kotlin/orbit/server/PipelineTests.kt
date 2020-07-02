/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.runBlocking
import orbit.server.mesh.ClusterManager
import orbit.server.pipeline.PipelineContext
import orbit.server.pipeline.step.RetryStep
import orbit.shared.mesh.NodeCapabilities
import orbit.shared.mesh.NodeId
import orbit.shared.mesh.NodeInfo
import orbit.shared.mesh.NodeLease
import orbit.shared.mesh.NodeStatus
import orbit.shared.net.Message
import orbit.shared.net.MessageContent
import orbit.shared.net.MessageTarget
import org.junit.Test

class PipelineTests : BaseServerTest() {

    @Test
    fun `When a node isn't present in cluster, message is sent to pipeline with attempts incremented`() {
        runBlocking {
            val testNode = NodeId("test", "test")
            val message = Message(
                mock<MessageContent.InvocationRequest>(),
                target = MessageTarget.Unicast(testNode)
            )

            val clusterManager = mock<ClusterManager>() {
                onBlocking { getNode(testNode) } doReturn null
            }
            val retryStep = RetryStep(clusterManager, OrbitServerConfig())

            val context = mock<PipelineContext>()
            retryStep.onOutbound(context, message)

            verify(context, never()).next(any())
            verify(context).pushNew(message.copy(attempts = 1))
        }
    }

    @Test
    fun `When a node is present in cluster, message is sent to next step`() {
        runBlocking {
            val testNode = NodeId("test", "test")
            val message = Message(
                mock<MessageContent.InvocationRequest>(),
                target = MessageTarget.Unicast(testNode)
            )

            val clusterManager = mock<ClusterManager> {
                onBlocking { getNode(testNode) } doReturn
                        NodeInfo(
                            testNode,
                            NodeCapabilities(listOf("test")),
                            lease = NodeLease.forever,
                            nodeStatus = NodeStatus.ACTIVE
                        )
            }
            val retryStep = RetryStep(clusterManager, OrbitServerConfig())

            val context = mock<PipelineContext>()
            retryStep.onOutbound(context, message)

            verify(context).next(message)
            verify(context, never()).pushNew(any(), anyOrNull())
        }
    }

    @Test
    fun `When a message exceeds retry attempts, error message is sent to pipeline`() {
        runBlocking {
            val testNode = NodeId("test", "test")
            val sourceNode = NodeId("source", "test")
            val message = Message(
                mock<MessageContent.InvocationRequest>(),
                target = MessageTarget.Unicast(testNode),
                source = sourceNode,
                attempts = 11
            )

            val clusterManager = mock<ClusterManager> {
                onBlocking { getNode(testNode) } doReturn null
            }
            val retryStep = RetryStep(clusterManager, OrbitServerConfig(messageRetryAttempts = 10))

            val context = mock<PipelineContext>()
            retryStep.onOutbound(context, message)

            verify(context, never()).next(any())
            verify(context).pushNew(argThat { content is MessageContent.Error }, anyOrNull())
        }
    }
}
