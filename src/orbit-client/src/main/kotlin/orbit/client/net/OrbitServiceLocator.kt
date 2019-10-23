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

    companion object {
        operator fun invoke(uri: URI) = uri.toServiceLocator()
        operator fun invoke(uri: String) = invoke(URI(uri))

        fun fromURI(uri: URI): OrbitServiceLocator {
            require(uri.scheme.toLowerCase() == ORBIT_SCHEME.toLowerCase()) { "Scheme must be $ORBIT_SCHEME." }
            require(!uri.path.isNullOrEmpty() && uri.path != "/") { "A namespace must be specified." }
            require(uri.port != -1) { "A port must be explicitly specified." }
            return OrbitServiceLocator(uri.host, uri.port, uri.path.trimStart('/'))
        }
    }
}

fun URI.toServiceLocator() = OrbitServiceLocator.fromURI(this)
fun OrbitServiceLocator.toURI() = URI(this.toString())