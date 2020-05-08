# Actors
In Orbit, an actor is an object that interacts with the world using asynchronous messages.

At any time an actor may be active or inactive. Usually the state of an inactive actor will reside in the database. When a message is sent to an inactive actor it will be activated somewhere in the pool of backend servers. During the activation process the actor’s state is read from the database.

Actors are deactivated based on timeout and on server resource usage.

# Actor Runtime Model
Orbit guarantees that only one activation of an actor with a given identity can exist at any one time in the cluster. Orbit also guarantees that calls to actors can never be processed in parallel.

As such, developers do not need to be concerned about keeping multiple activations/instances of an actor synchronized with one another.

Finally, this also means that developers do not need to worry about concurrent access to an actor. Two calls to an actor can not be processed in parallel.

# Actor Interfaces
Before you can implement an actor you must create an interface for it.

```java
package com.example.orbit.hello;

import cloud.orbit.actors.Actor;
import cloud.orbit.concurrent.Task;
 
public interface Hello extends Actor
{
    Task<String> sayHello(String greeting);
}
```
* Actor interfaces must extend Orbit’s Actor interface
* Methods in actor interfaces must return an Orbit Task.

# Actor Implementations
Once you have created an Actor, you must offer an implementation of that Actor for Orbit to use.

```kotlin
package com.example.orbit.hello;

import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.concurrent.Task;
 
public class HelloImpl extends AbstractActor implements Hello
{
    public Task<String> sayHello(String greeting)
    {
        getLogger().info("Here: " + greeting);
        return Task.fromValue("You said: '" + greeting
                + "', I say: Hello from " + System.identityHashCode(this) + " !");
    }
}
```
* Actor implementations must extend AbstractActor
* Actor implementations must implement a single actor interface
* Only one implementation per actor interface is permitted

# Lifetime
## Overview
Unlike other frameworks, there is no explicit lifetime management for actors in Orbit.

Actors are never created or destroyed, they always exist conceptually. 

Not all actors in Orbit will be in-memory in the cluster at a given time, actors which are in-memory are “activated” and those which are not are “deactivated”. The process of an actor being created in-memory is known as “Activation” and the process of an actor being removed from memory is known as “Deactivation”. 

Activation and Deactivation is transparent to the developer.

## Flow
When a message is sent to a given actor reference, the framework will perform the following actions:

1. Check if the actor is already activated in the cluster
  1. If so, forward the message
  2. If not, proceed to step 2
2. Check if the actor has persisted state
  1. If so, load the state, activate the actor and forward the message
  2. If not, proceed to step 3
3. Activate a default actor for the actor type and forward the message

## Reacting to Activation Events
While the lifetime and activation/deactivation of an actor is transparent, it is sometimes desirable to perform certain actions during activation or deactivation.

For instance: Checking if observers still exist, persisting state that has not yet been written etc.

Orbit offers 2 methods that can be overridden in an actor to allow developers to add logic.

```java
public class SomeActor extends AbstractActor implements Some
{
    @Override
    public Task activateAsync()
    {
        return super.activateAsync();
    }

    @Override
    public Task deactivateAsync()
    {
        return super.deactivateAsync();
    }
}
```

## Requesting Deactivation
It is sometimes useful for an application to request deactivation of an actor if it knows it is no longer needed.

```java
    Actor.deactivate(actorRef);
```