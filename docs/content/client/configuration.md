---
title: "Client Configuration"
---

Orbit Client offers a broad set of configuration options to be supplied in its constructor. Some settings are required to discover addressables and connect to an Orbit Server cluster, while others can fine-tune how the client operates.

The `OrbitClientConfig` class contains the configuration values the `OrbitClient` needs to operate and is supplied in the constructor:

```kotlin
import orbit.client.OrbitClient
import orbit.client.OrbitClientConfig

fun main() {
    runBlocking {
        val orbitClient = OrbitClient(
            OrbitClientConfig(
                namespace = "carnival",
                grpcEndpoint = "dns:///localhost:50056/",
                ...
            )
        )
        orbitClient.start().join()
    }
}
```

## Required fields

#### `namespace: String`
The namespace to use when connecting to the Orbit cluster. This namespace will identify all the client nodes as part of a system, isolated from any other namespaces. Care must be taken if multiple independent deployments utilize the same Orbit services so that the namespaces they use don't intersect. This will result in confusion as addressables are activated in unexpected places.

#### `grpcEndpoint: String`
The gRPC endpoint where the Orbit server cluster is located.

## Interesting optional fields

#### `packages: List<String>`
Packages to scan for addressables. If left blank, all packages will be scanned. Example:
```
    packages = listOf("orbit.carnival.actors")
```

#### `addressableTTL: Duration`
Time To Live (TTL) for addressables before deactivation.

#### `addressableConstructor: ExternallyConfigured<AddressableConstructor>`
The system to use to construct addressables. The default value calls the addressable's default empty constructor. More information on bringing your own DI can be found in the [Dependency Injection](/client/dependency-injection) section.

#### `platformExceptions: Boolean`
Rethrow platform specific exceptions. Note: Should only be used when all clients are using the same SDK.

#### `containerOverrides: ComponentContainerRoot.() -> Unit`
Hook to update internal container registrations after initialization. Example:
```
    containerOverrides = {
        instance(kodein)
        instance(sqlDatabaseAdapter)
        definition<HealthService>()
    }
```

#### `bufferCount: Int`
The number of messages (inbound) that may be queued before new messages are rejected. This applies back pressure protection to prevent being overwhelmed with incoming messages.
