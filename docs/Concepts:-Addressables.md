# Addressables
In Orbit, an addressable is an object that interacts with the world through asynchronous messages.

Orbit guarantees that only one addressable with a given identity can be active at any time in the cluster. As such, developers do not need to be concerned about keeping multiple activations/instances of an addressable synchronized with one another.

Orbit also guarantees that calls to addressables can never be processed in parallel, meaning developers do not need to worry about concurrent access to an addressable. Two calls to an addressable can not be processed in parallel.

## Addressable Interfaces

Before you can implement an addressable you must create an interface for it.

```kotlin
package sample.addressables

import kotlinx.coroutines.Deferred
import orbit.client.addressable.Addresable
 
interface Greeter : Addressable {
    fun hello(message: String): Deferred<Unit>
}
```

* Addressable interfaces must extend Addressable or a derived interface
* Methods in addressable interfaces must return a Deferred.

## Addressable Implementations
Once you have created an Addressable, you must offer an implementation of that Addressable for Orbit to use.

```kotlin
package sample.addressables

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.CompletableDeferred
import orbit.client.addressables.AbstractAddressable
import sample.addressables.Greeter
 
class GreeterImpl() : AbstractAddressable(), Greeter {
{
    override fun hello(message: String): Deferred<Unit> {
        ... Hello code

        return CompletableDeferred(Unit)
    }
}
```
* Addressable implementations should extend AbstractAddressable to access the addressable context
* Addressable Context contains the AddressableReference (identifier) and access to the Orbit Client instance
* Actor implementations must implement a single actor interface
* Only one implementation per addressable interface is permitted

## Lifetime
When a message is sent to a given addressable reference, the framework will perform the following actions:

1. Check if the addressable is already activated in the cluster
    - If so, forward the message to the client
    - If not, proceed to step 2
2. Activate the addressable for the addressable type and place on a connected client
3. Call the OnActivate hook to initialize any state, such as restoring from persistance
4. Forward the message to the addressable

Orbit does not provide any default persistence functionality, but the `@OnActivate` hook is available to restore state from a store before it receives the message.


# Actors

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

## Context

The AddressableContext holds 

```kotlin
abstract class AbstractAddressable {
    /**
     * The Orbit context. It will be available after the [Addressable] is registered with Orbit.
     * Attempting to access this variable before registration is undefined behavior.
     */
    lateinit var context: AddressableContext
}
```

```kotlin
data class AddressableContext(
    /**
     * A reference to this [Addressable].
     */
    val reference: AddressableReference,
    /**
     * A reference to the [OrbitClient].
     */
    val client: OrbitClient
)
```




## Flow
When a message is sent to a given actor reference, the framework will perform the following actions:

1. Check if the actor is already activated in the cluster
    - If so, forward the message to the client
    - If not, proceed to step 2
2. Activate the actor for the actor type and place on a connected client
3. Call the OnActivate hook to initalize any state, such as restoring from persisteance
4. Forward the message to the actor

Orbit does not provide any default persistence functionality, so any actor that needs to be loaded from a store needs to do so before it can receive a message. This is handled through the OnActivate event. 

```kotlin
class GameImpl() : AbstractActor(), Game {
{
    @OnActivate
    fun onActivate(): Deferred<Unit> = GlobalScope.async {
        load()
    }

    @OnDeactivate
    fun onDeactivate(deactivationReason: DeactivationReason): Deferred<Unit> = GlobalScope.async {
        save()
    }
}
```

Actors can be persisted at any time, but the OnDeactivate event can help assure the latest state is saved, except in the event of an hard shutdown. During graceful shutdown, every actor is deactivated, giving it a final chance to persist.
