/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.net

import java.net.URI

private const val ORBIT_SCHEME = "orbit"

data class OrbitServiceLocator(val host: String, val port: Int, val namespace: String) {
    override fun toString() = "$ORBIT_SCHEME://$host:$port/$namespace"
}

fun URI.toServiceLocator(): OrbitServiceLocator {
    require(scheme.toLowerCase() == ORBIT_SCHEME.toLowerCase()) { "Scheme must be $ORBIT_SCHEME." }
    require(!path.isNullOrEmpty() && path != "/") { "A namespace must be specified." }
    require(port != -1) { "A port must be explicitly specified."}
    return OrbitServiceLocator(this.host, this.port, path.trimStart('/'))
}

fun OrbitServiceLocator.toURI() = URI(this.toString())