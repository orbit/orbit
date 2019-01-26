/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.core.net

/**
 * Information about an Orbit node that is shared within the cluster.
 */
data class NodeInfo(
    /**
     * The name of the cluster this node is part of.
     */
    val clusterName: ClusterName,
    /**
     * The identity of this node.
     */
    val nodeIdentity: NodeIdentity,
    /**
     * The mode of this node.
     */
    val nodeMode: NodeMode,
    /**
     * The status of this node.
     */
    val nodeStatus: NodeStatus,
    /**
     * The capabilities of this node.
     */
    val nodeCapabilities: NodeCapabilities
)

/**
 * Information about an Orbit cluster.
 */
data class ClusterInfo(
    /**
     * The name of this cluster.
     */
    val clusterName: ClusterName,
    /**
     * The nodes in this cluster.
     */
    val nodes: Collection<NodeInfo>
)

/**
 * The capabilities of a node.
 */
data class NodeCapabilities(
    /**
     * The addressables which have concrete implementations on this node.
     */
    val implementedAddressables: List<String>
)

/**
 * The status of an Orbit node.
 */
enum class NodeStatus {
    /**
     * The node is stopped and idle
     */
    STOPPED,
    /**
     * The node is starting and attempting to join the cluster.
     */
    STARTING,
    /**
     * The node is running an active in the cluster.
     */
    RUNNING,
    /**
     * The node is stopping and leaving the cluster.
     */
    STOPPING
}

/**
 * The mode of an Orbit node.
 */
enum class NodeMode {
    /**
     * Server - Hosts actors and other objects.
     */
    SERVER,
    /**
     * Client - Does not actively host actors and other objects.
     */
    CLIENT
}