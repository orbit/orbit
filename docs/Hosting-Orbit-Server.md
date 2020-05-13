# Hosting Orbit Server

Describe taking a reference to orbit-server, starting an instance. Role of orbit-application.


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
