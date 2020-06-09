---
title: "Shutdown"
---

Orbit is designed to handle a graceful or hard shutdown.

In a hard shutdown scenario, Orbit servers will notice a client node disconnecting the GRPC connection and suspend the leases on all addressables placed on that node. Any subsequent messages will be routed to new nodes where the addressables become activated. There may be some message loss during this brief transition period.

Orbit also affords an opportunity for a graceful shutdown through a shutdown procedure that can be invoked by the client application.

By calling `.stop()` on the Orbit Client instance, Orbit will deactivate all the actors placed on this node so they can be 

```kotlin
orbitClient.stop()
```

In a Kotlin application, this can be used in conjunction with a SIGTERM handler to automatically clean up on any graceful application exit.

```kotlin
Runtime.getRuntime().addShutdownHook(object : Thread() {
    override fun run() = runBlocking {
        println("Gracefully shutting down")
        orbitClient.stop().join()
        println("Shutdown complete")
    }
})
```

When `.stop()` is called, the Orbit Client will go through the following procedure.

1. System signals to the client application a shutdown (SIGTERM)
2. Application calls `orbitClient.stop()` and waits for it to return
    * Orbit Client tells the server to remove the client node from the pool for addressable placement
    * Orbit Client iterates through all addressables placed on this node
        * Calls the OnDeactivate method on the addressable, if specified, to perform any cleanup
        * Abandons the lease with the Orbit service
3. Application completes its shutdown

Notes:
* Ideally, client applications should be built around actors persisting their state on write, so there is nothing left to do on shutdown. But in rare cases where that's required, this option is available.
* A graceful shutdown will likely lead to less message loss than a hard shutdown.
* Any messages in-flight to an addressable on the shutting down node will still be delivered until the addressable is deactivating. This can be handy if graceful shutdown with many addressables and a costly deactivation cause shutdown to last seconds or minutes.

## Deactivation Modes
There are 4 available modes for deactivation, reflecting different scenarios.

### Instant
This is the default mode of operation. Orbit client will call to deactivate the addressables as fast as possible. This is suitable for scenarios where there is no need to persist to a potentially bottlenecked resource such as a database, and there is no expectation another client node reactivating the addressables will put any undue strain on a resource.
```
OrbitClientConfig(
    ...
    addressableDeactivator = AddressableDeactivator.Instant.Config()
)
```

### Concurrent
This lets the number of deactivations be limited to a set number occurring simultaneously. This is useful for cases where some resource like database connections in a pool needs to be protected from a flood of last-minute writes.
```
OrbitClientConfig(
    ...
    addressableDeactivator = AddressableDeactivator.Concurrent.Config(concurrentDeactivations = 20)
)
```

### Rate Limited
This controls the number of deactivations initiated per second, regardless of how long they take. This is for scenarios where either deactivation or reactivation on a new host needs to be controlled at a sustainable rate.
```
OrbitClientConfig(
    ...
    addressableDeactivator = AddressableDeactivator.RateLimited.Config(deactivationsPerSecond = 100)
)
```

### Time Span
Similar to rate limiting, but spreads the deactivations over a number of milliseconds, regardless of the number of active addressables.
```
OrbitClientConfig(
    ...
    addressableDeactivator = AddressableDeactivator.TimeSpan.Config(deactivationTimeMilliseconds = 10000)
)
```

## Overriding deactivation mode
In some cases it's not guaranteed that the configured deactivation mode is desired, like if a hard shutdown is required in some cases. An addressable deactivation method can be specified when shutting down the client by passing it into the `orbitClient.stop()` method.
```
orbitClient.stop(
    deactivator = AddressableDeactivator.TimeSpan(AddressableDeactivator.TimeSpan.Config(10000))
)
```

## SIGTERM Caveat
The method described above to hook shutdown into SIGTERM is the simplest solution that handles most situations. Detaching a debugger, Ctrl-C from the command line, or shutting down a Docker container all invoke this path.

In a larger application, the time to gracefully drain all actors and shut down could take seconds or minutes. If there are system constraints that timeout before sending a SIGKILL, the graceful shutdown could be incomplete.

Additionally, in more complicated applications, many components could be listening for the SIGTERM event leading to non-determinism in tearing the application down. For example, metrics services may be shut off before the drain is complete, leaving the larger environment blind to the application's inner state.

It is up to the application developer to balance these concerns and determine the best way to tear down the application.