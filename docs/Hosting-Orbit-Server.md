# Hosting Orbit Server

Almost any scenario can be handled by using the prepackaged Orbit Server. For scenarios where the developer needs extensive control over the server, Orbit functionality can be hosted within a custom application.


Describe taking a reference to orbit-server, starting an instance. Role of orbit-application.


Gradle:
```kotlin
implementation("cloud.orbit:orbit-server:{{ book.release }}")
```

To instantiate an OrbitServer:

```kotlin
import kotlinx.coroutines.runBlocking
import orbit.server.OrbitServerConfig
import orbit.server.OrbitServer

fun main() {
    runBlocking {
        val server = OrbitServer(OrbitServerConfig({
            ... configuration
        }))
        server.start().join()
    }
}
```

### OrbitServerConfig
The `OrbitServerConfig` class can be used to make changes to server configurations, including things like lease times, persistence technology, resource limitations, and metrics.
