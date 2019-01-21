/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.core.net

data class NodeInfo(
    val clusterName: ClusterName,
    val nodeIdentity: NodeIdentity,
    val nodeStatus: NodeStatus
)

data class ClusterInfo(
    val clusterName: ClusterName,
    val nodes: Collection<NodeInfo>
)

enum class NodeStatus {
    STOPPED,
    STARTING,
    RUNNING,
    STOPPING
}