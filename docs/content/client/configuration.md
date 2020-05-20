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
                packages = listOf("orbit.carnival.actors"),
                ...
            )
        )
        orbitClient.start().join()
    }
}
```

## Required fields

### `namespace: String`
The namespace to use when connecting to the Orbit cluster.

### `grpcEndpoint: String`
The gRPC endpoint where the Orbit cluster is located.

## Interesting optional fields

### `packages: List<String>`
Packages to scan for addressables. If left blank, all packages will be scanned.


### `addressableTTL: Duration`
Time To Live (TTL) for addressables before deactivation

### `addressableConstructor: ExternallyConfigured<AddressableConstructor>`
The system to use to construct addressables.

### `platformExceptions: Boolean`
Rethrow platform specific exceptions. Should only be used when all clients are using the same SDK.

### `containerOverrides: ComponentContainerRoot.() -> Unit`
Hook to update internal container registrations after initialization

### `bufferCount: Int`
The number of messages (inbound) that may be queued before new messages are rejected.


