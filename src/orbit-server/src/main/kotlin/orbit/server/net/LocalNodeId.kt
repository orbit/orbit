/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

data class LocalNodeId(val nodeId: NodeId = NodeId.generate()) {
    companion object Generate {
        @JvmStatic
        fun generate(prefix: String = ""): LocalNodeId {
            return LocalNodeId(NodeId.generate(prefix))
        }
    }
}