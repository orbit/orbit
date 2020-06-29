---
title: "Actors"
weight: 5
---

Actors are the most common form of addressable and are suitable for most situations. The distinction is semantic and Actor interfaces are provided to facilitate the actor pattern.

Actors are never created or destroyed; they always exist conceptually. Not all actors in Orbit will be in-memory in the cluster at a given time. Actors which are in-memory are considered “activated” and those which are not are “deactivated”. The process of an actor being created in-memory is known as “Activation” and the process of an actor being removed from memory is known as “Deactivation”.

When a message is sent to an inactive actor, it will be placed somewhere in the pool of connected servers and activated. During the activation process, the actor’s state can be restored from a database. Actors are deactivated based on activity timeouts or server resource usage. Actor state can be persisted at any time or upon deactivation.

## Actors
Just like with an Addressable, Actors are defined by interfaces and implementations.

```kotlin
package orbit.carnival.actors

import orbit.client.actor.ActorWithStringKey
import orbit.client.actor.AbstractActor
 
interface Game : ActorWithStringKey {
    suspend fun play(playerId: String): PlayedGameResult
}

class GameImpl() : AbstractActor(), Game {
{
    override suspend fun play(playerId: String): PlayedGameResult {
        ... Game code
    }
}
```

## Calling
Actors are called via a proxy. A proxy is created via the Orbit runtime context, typically Stage or AddressableContext.

### Accessing the Proxy Factory

**Kotlin**
```kotlin
// Via Orbit Client
orbit.actorFactory.createProxy<Greeter>()

// From an Addressable
context.client.actorFactory.createProxy<Greeter>()
```

**Java**
```java
// Via Orbit Client
orbit.getActorFactory().createProxy(Greeter.class);

// From an Addressable
getContext().getClient().getActorFactory().createProxy(Greeter.class);
```

## Runtime Model
Orbit guarantees that only one activation of an actor with a given identity can exist at any one time in the cluster by default. As such, developers do not need to be concerned about keeping multiple activations/instances of an actor synchronized with one another.

By default, Orbit also guarantees that calls to actors can never be processed in parallel using safe execution mode. This means that developers do not need to worry about concurrent access to an actor. Two calls to an actor can not be processed in parallel.

## Keys
Like all addressables, every actor in Orbit has a key. Additionally, to ensure they are type safe every actor interface must choose only one key type, this is achieved by extending one of the following actor interfaces. An example of how this works can be seen below.

Actor Interface | JDK Type | Orbit Type
---|---|---
ActorWithNoKey | N/A | NoKey
ActorWithStringKey | String | StringKey
ActorWithInt32Key | Integer | Int32Key
ActorWithInt64Key | Long | Int64Key