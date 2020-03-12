/*
 Copyright (C) 2015 - 2020 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.server.prometheus

import com.sun.net.httpserver.HttpServer
import io.micrometer.prometheus.PrometheusMeterRegistry
import java.io.PrintWriter
import java.net.InetSocketAddress

class PrometheusMeterEndpoint(
    registry: PrometheusMeterRegistry,
    endpoint: String = "/metrics",
    port: Int = 8080
) {
    init {
        HttpServer.create(InetSocketAddress(port), 0).apply {

            createContext(endpoint) { http ->
                http.responseHeaders.add("Content-type", "text/plain")
                http.sendResponseHeaders(200, 0)
                PrintWriter(http.responseBody).use { out ->
                    out.println(registry.scrape())
                }
            }

            start()
        }
    }
}