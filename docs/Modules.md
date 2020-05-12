# Overview
The main Orbit project is split into several modules.

* orbit-client
* orbit-application
* orbit-prometheus
* orbit-proto
* orbit-server
* orbit-server-etcd
* orbit-server-prometheus
* orbit-shared
* orbit-util

# Orbit Client
`orbit-client` is a JVM library for applications interfacing with an Orbit cluster. It handles maintaining a connection to the mesh, leasing addressables, and routing messages. It will be the main entrypoint for most developers.

Gradle:
```kotlin
implementation("cloud.orbit:orbit-client:$orbitVersion")
```

# Orbit Server
Orbit can be run as a packaged service without having to delve into the remaining modules. However, if a more customized version of Orbit is needed, these modules can be used to build the right server.

## orbit-server
This is the main implementation of the Orbit Server cluster node. It handles the client connections, mesh connections, authorization, node and addressable leases, and message routing.

Gradle:
```kotlin
implementation("cloud.orbit:orbit-server:$orbitVersion")
```

To instantiate an OrbitServer:

```kotlin
import kotlinx.coroutines.runBlocking
import orbit.server.OrbitServerConfig
import orbit.server.OrbitServer

fun main() {
    runBlocking {
        val server = OrbitServer(OrbitServerConfig())
        server.start().join()
    }
}
```

### OrbitServerConfig
The `OrbitServerConfig` class can be used to make changes to server configurations, including things like lease times, persistence technology, resource limitations, metrics, and 
