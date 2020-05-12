# Shutdown

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
* The addressables are deactivated in parallel with a maximum number of simultaneous deactivates set in Orbit Config (`val deactivationConcurrency: Int = 10`). The proper value for this concurrency will highly depend on constraints of the application, such as expected addressable counts, or any disk or database activity limitations.
* Any messages in-flight to an addressable on the shutting down node will still be delivered until the addressable is deactivating. This can be handy if graceful shutdown with many addressables and a costly deactivation cause shutdown to last seconds or minutes.
