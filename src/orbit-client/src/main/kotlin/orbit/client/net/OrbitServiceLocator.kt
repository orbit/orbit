/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.net

import java.net.URI

private const val ORBIT_SCHEME = "orbit"

data class OrbitServiceLocator(val host: String, val port: Int, val namespace: String)

fun URI.toServiceLocator(): OrbitServiceLocator {
    require(this.scheme.toLowerCase() == ORBIT_SCHEME.toLowerCase()) { "Scheme must be $ORBIT_SCHEME." }
    requireNotNull(this.path)
    return OrbitServiceLocator(this.host, this.port, this.path)
}

fun OrbitServiceLocator.toURI() = URI("$ORBIT_SCHEME://$host:$port/$namespace")