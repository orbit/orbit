/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.router

import orbit.server.mesh.ClusterManager
import orbit.server.mesh.LocalNodeInfo
import orbit.shared.mesh.NodeId
import orbit.shared.router.Route
import org.jgrapht.alg.shortestpath.DijkstraShortestPath

@Suppress("UnstableApiUsage")
class Router(private val localNode: LocalNodeInfo, private val clusterManager: ClusterManager) {
    @Suppress("UNUSED_PARAMETER")
    fun findRoute(targetNode: NodeId, possibleRoute: Route? = null): Route {
        val graph = clusterManager.getGraph()
        val path = DijkstraShortestPath.findPathBetween(graph, localNode.info.id, targetNode)?.vertexList

        checkNotNull(path) { "Could not find path for $targetNode" }

        return Route(path.drop(1))
    }
}




