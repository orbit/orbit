---
title: "Addressables"
---

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
* Actor implementations must implement a single actor interface
* Only one implementation per addressable interface is permitted

## Lifetime
When a message is sent to a given addressable reference, the framework will perform the following actions:

1. Check if the addressable is already activated in the cluster
    - If so, forward the message to the client
    - If not, proceed to step 2
2. Activate the addressable for the addressable type and place on a connected client
3. Call the OnActivate hook to initialize any state, such as restoring from persistence
4. Forward the message to the addressable

Orbit does not provide any default persistence functionality, but the `@OnActivate` hook is available to restore state from a store before it receives the message.

```kotlin
class GreeterImpl() : AbstractAddressable(), Game {
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

Addressables can be persisted at any time, but the @OnDeactivate hook can help assure the latest state is saved, except in the event of an hard shutdown. During graceful shutdown, every actor is deactivated giving it a final chance to persist.

## Context

If an Addressable extends the AbstractAddressable or AbstractActor class, it will have access to the AddressableContext. The AddressableContext provides access to the AddressableReference (identifier) and the OrbitClient instance. The AddressableReference can be used during the @OnActivate hook to identify which addressable needs to be restored from persistence.

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
For example, if the addressable is an Actor with a String type key, 
```kotlin
interface Player : ActorWithStringKey {}

class PlayerImpl(private val playerStore: PlayerStore) : AbstractActor(), Player {
    ...
    private suspend fun loadFromStore() {
        val loadedPlayer = playerStore.get((this.context.reference.key as Key.StringKey).key)

        rewards = loadedPlayer?.rewards?.toMutableList() ?: mutableListOf()
    }
    ...
}

```
