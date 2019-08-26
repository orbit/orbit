/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.net

import orbit.common.util.RNGUtils

inline class NodeId(val value: String) {
    companion object {
        fun generate(prefix: String? = ""): NodeId {
            return NodeId("$prefix:${RNGUtils.secureRandomString()}")
        }
    }
}