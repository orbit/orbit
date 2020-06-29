---
title: "Hello World"
weight: 2
---

In this guide we’ll cover how to get a very simple Orbit application running in the form of “Hello World”.  It shows using a single-module in a single-process environment, often useful for development purposes. This will demonstrate how to set up an Actor and communicate with an Orbit Server.

This tutorial assumes that you have set up a development environment as described in the [Getting Started Prerequisites](/getting-started/prerequisites) document and have some familiarity with Gradle based Kotlin projects. All samples are in Kotlin, the language in which Orbit is developed, but the principles work for a Java app as well.

# Gradle Project
The first step is to set up a Gradle project and pull in the Orbit dependencies.

```
implementation("cloud.orbit:orbit-client:{{< release >}}")
```

# Actor Interface
In Orbit all actors must have an interface, below we’ll create a very simple actor interface.

**greeter.kt**
```kotlin
package orbit.hello

import orbit.client.actor.ActorWithStringKey

interface Greeter : ActorWithStringKey {
    suspend fun sayHello(): String
}
```
* Actor interfaces are standard Kotlin interfaces with special constraints
* All Actor interfaces must extend an Actor type
* All interface methods must return a future in the form of a Deferred or be a suspending Kotlin method
* The return type must be serializable or Unit.
 
# Actor Implementation
Once you have an actor interface in place, the final step to complete the actor is to create an actor implementation.

**greeterImpl.kt**
```kotlin
package orbit.hello

import orbit.client.actor.AbstractActor

class GreeterImpl : AbstractActor(), Player {
{
    override suspend fun sayHello(greeting: String): String {
    {
        println("Here: ${greeting}")
        return "You said: '${greeting}', I say: 'Hello from ${context.reference.key} at node ${context.client.nodeId?.key}!'")
    }
}
```

* An actor implementation is a standard Kotlin or Java class
* Extending AbstractActor grants access to the context with a reference to client and the actor's address
* The actor must implement an actor interface
* Only one actor implementation per actor interface is permitted
 
# Using The Actor
The final step to get a working example is for us to actually use the actor.

**app.kt**
```kotlin
package orbit.hello

import orbit.hello.actors.Actor
import orbit.hello.actors.Stage

fun main() {
    runBlocking {
        val orbitClient = OrbitClient(
            OrbitClientConfig(
                namespace = "hello",
                grpcEndpoint = "dns:///localhost:50056/"
            )
        )

        orbitClient.start().join()

        val greeter = orbit.actorFactory.createProxy<Greeter>("Tim")
        val response = greeter.hello("Welcome to Orbit")
        println(response)

        orbit.stop().join()
    }
}
```
* We create an orbit client instance in our namespace "hello"
* We get a reference to an actor
* The framework will handle the activation of the actor.
* You can communicate with the actor without knowing it's status.

# Running Orbit Server
Assure Docker Desktop is running and Docker Compose is installed from the [Prerequisites](/getting-started/prerequisites) step.

In a terminal window, run the following.

```shell script
> docker run -it -p 50056:50056 orbitframework/orbit:{{< release >}}
Listening for transport dt_socket at address: 5005
[main] INFO orbit.application.impl.SettingsLoader - Searching for Orbit Settings...
[main] INFO orbit.application.impl.SettingsLoader - No settings found. Using defaults.
[main] INFO orbit.server.mesh.local.LocalMeterRegistry - Starting simple meter registry
[orbit-cpu-1] INFO orbit.server.OrbitServer - Starting Orbit server...
[orbit-cpu-1] INFO orbit.server.OrbitServer - Lease expirations: Addressable: 600s, Node: 10s
[orbit-cpu-1] INFO orbit.server.pipeline.Pipeline - Started a rail worker with 32 rails and a 10000 entry buffer.
[orbit-cpu-1] INFO orbit.server.mesh.LocalNodeInfo - Joined cluster as (NodeId(key=aFmxt6MWvWP5HihR, namespace=management))
[orbit-cpu-1] INFO orbit.server.service.GrpcEndpoint - Starting gRPC Endpoint on LocalServerInfo(port=50056, url=localhost:50056).port...
[orbit-cpu-1] INFO orbit.server.service.GrpcEndpoint - gRPC Endpoint started on LocalServerInfo(port=50056, url=localhost:50056).port.
[orbit-cpu-1] INFO orbit.server.OrbitServer - Orbit server started successfully in 338ms.
```

# Running
You should now be able to run the project. If everything has gone well, you should see output similar to the following:

```
Connecting to Orbit at dns:///localhost:50056/
Here: Welcome to Orbit
You said: 'Welcome to Orbit!', I say: Hello from Tim at node aFmxt6MWvWP5HihR!
```

Congratulations, you have created your first Orbit application!