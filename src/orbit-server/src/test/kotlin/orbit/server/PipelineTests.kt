/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server

import com.nhaarman.mockitokotlin2.never
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
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class PipelineTests : BaseServerTest() {
    // hack to compensate for known problem with Mockito's matchers under Kotlin returning null
    private fun <T> any(): T = null as T

    @Test
    fun `When a node isn't present in cluster, message is sent to pipeline with attempts incremented`() {
        runBlocking {
            val testNode = NodeId("test", "test")
            val message = Message(
                mock(MessageContent.InvocationRequest::class.java),
                target = MessageTarget.Unicast(testNode)
            )

            val clusterManager = mock(ClusterManager::class.java)
            `when`(clusterManager.getNode(testNode)).thenReturn(null)
            val retryStep = RetryStep(clusterManager)

            val context = mock(PipelineContext::class.java)
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
                mock(MessageContent.InvocationRequest::class.java),
                target = MessageTarget.Unicast(testNode)
            )

            val clusterManager = mock(ClusterManager::class.java)
            `when`(clusterManager.getNode(testNode)).thenReturn(
                NodeInfo(
                    testNode,
                    NodeCapabilities(listOf("test")),
                    lease = NodeLease.forever,
                    nodeStatus = NodeStatus.ACTIVE
                )
            )
            val retryStep = RetryStep(clusterManager)

            val context = mock(PipelineContext::class.java)
            retryStep.onOutbound(context, message)

            verify(context).next(message)
            verify(context, never()).pushNew(any())
        }
    }
}
