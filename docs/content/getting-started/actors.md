---
title: "Actors"
weight: 5
---

Actors are the most common form of addressable, and are suitable for most situations. The distinction is semantic and Actor interfaces are provided to facilitate the actor pattern.

Actors are never created or destroyed; they always exist conceptually. Not all actors in Orbit will be in-memory in the cluster at a given time. Actors which are in-memory are considered “activated” and those which are not are “deactivated”. The process of an actor being created in-memory is known as “Activation” and the process of an actor being removed from memory is known as “Deactivation”.

When a message is sent to an inactive actor, it will be placed somewhere in the pool of connected servers and activated. During the activation process, the actor’s state can be restored from a database. Actors are deactivated based on activity timeouts or server resource usage. Actor state can be persisted at any time or upon deactivation.

```kotlin
package orbit.carnival.actors

import kotlinx.coroutines.Deferred
import orbit.client.actor.ActorWithStringKey
import orbit.client.actor.AbstractActor
 
interface Game : ActorWithStringKey {
    fun play(playerId: String): Deferred<PlayedGameResult>
}

class GameImpl() : AbstractActor(), Game {
{
    override fun play(playerId: String): Deferred<PlayedGameResult> = GlobalScope.async {
        ... Game code
    }
}
```

The same rules for Addressables apply to Actors:
* Actor implementations should extend AbstractActor to access the addressable context
* Actor implementations must implement a single actor interface
* Only one implementation per actor interface is permitted