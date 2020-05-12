# Actors
In Orbit, an actor is an object that interacts with the world using asynchronous messages.

Actors are never created or destroyed; they always exist conceptually. Not all actors in Orbit will be in-memory in the cluster at a given time. Actors which are in-memory are considered “activated” and those which are not are “deactivated”. The process of an actor being created in-memory is known as “Activation” and the process of an actor being removed from memory is known as “Deactivation”.

When a message is sent to an inactive actor, it will be placed somewhere in the pool of backend servers and activated. During the activation process, the actor’s state can be restored from a database. Actors are deactivated based on activity timeouts or server resource usage. Actor state can be persisted at any time, or upon deactivation.

# Actor Runtime Model
Orbit guarantees that only one actor with a given identity can be active at any time in the cluster. Orbit also guarantees that calls to actors can never be processed in parallel.

As such, developers do not need to be concerned about keeping multiple activations/instances of an actor synchronized with one another.

Finally, this also means that developers do not need to worry about concurrent access to an actor. Two calls to an actor can not be processed in parallel.

# Actor Interfaces
Before you can implement an actor you must create an interface for it.

```kotlin
package orbit.carnival.actors

import kotlinx.coroutines.Deferred
import orbit.client.actor.ActorWithStringKey
 
interface Game : ActorWithStringKey {
    fun play(playerId: String): Deferred<PlayedGameResult>
}
```
* Actor interfaces must extend one of the Actor interfaces
* Methods in actor interfaces must return a `Deferred<T>`.

# Actor Implementations
Once you have created an Actor, you must offer an implementation of that Actor for Orbit to use.

```kotlin
package orbit.carnival.actors

import kotlinx.coroutines.Deferred
import orbit.client.actor.AbstractActor
import orbit.carnival.actors.Game
 
class GameImpl() : AbstractActor(), Game {
{
    override fun play(playerId: String): Deferred<PlayedGameResult> = GlobalScope.async {
        ... Game code
    }
}
```
* Actor implementations can extend AbstractActor for access to context, including the actor Id
* Actor implementations must implement a single actor interface
* Only one implementation per actor interface is permitted


## Lifetime
When a message is sent to a given actor reference, the framework will perform the following actions:

1. Check if the actor is already activated in the cluster
    - If so, forward the message to the client
    - If not, proceed to step 2
2. Activate the actor for the actor type and place on a connected client
3. Call the OnActivate hook to initalize any state, such as restoring from persisteance
4. Forward the message to the actor

Orbit does not provide any default persistence functionality, so the `@OnActivate` hook is available to restore state from a store before it receives the message.

```kotlin
class GameImpl() : AbstractActor(), Game {
{
    @OnActivate
    fun onActivate(): Deferred<Unit> = GlobalScope.async {
        loadFromStore()
    }

    @OnDeactivate
    fun onDeactivate(deactivationReason: DeactivationReason): Deferred<Unit> = GlobalScope.async {
        saveToStore()
    }
}
```

Actors can be persisted at any time, but the OnDeactivate event can help assure the latest state is saved, except in the event of an ungraceful shutdown. During shutdown, every actor is deactivated, giving it a final chance to persist.
