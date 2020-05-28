---
title: "Addressables"
weight: 4
---

In Orbit, an addressable is an object that interacts with the world through asynchronous messages. Simply, it has an address and can receive messages, even remotely.

Orbit will activate an addressable when it receives a message, and deactivate it when after a configurable period of inactivity. This patterns allows developers to speed up interactions and reduce load on databases and external services.

Orbit guarantees that only one addressable with a given identity can be active at any time in the cluster. As such, developers do not need to be concerned about keeping multiple activations/instances of an addressable synchronized with one another.

Orbit also guarantees that calls to addressables can never be processed in parallel, meaning developers do not need to worry about concurrent access to an addressable. Two calls to an addressable can not be processed in parallel.

## Addressable Interfaces

Before you can implement an addressable you must create an interface for it.

```kotlin
package sample.addressables

import kotlinx.coroutines.Deferred
import orbit.client.addressable.Addresable
 
interface Greeter : Addressable {
    fun hello(message: String): Deferred<String>
}
```

## Asynchronous Return Types
Addressables must only contain methods which return asynchronous types (such as promises).
The following return types (and their subtypes) are currently supported.

| Main Type | Common Subtypes | For |
| :--- | :--- | :--- |
| [Deferred](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-deferred/)	| [CompletableDeferred](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-completable-deferred/) | Kotlin |
| [CompletionStage](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html) | [CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html) | JDK8+ |

## Concrete Implementation
Once you have created an Addressable interface, you must offer an implementation for Orbit to use. For each addressable interface, exactly one addressable class must implement it.

```kotlin
package sample.addressables

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.CompletableDeferred
import orbit.client.addressables.AbstractAddressable
import sample.addressables.Greeter
 
class GreeterImpl() : AbstractAddressable(), Greeter {
{
    override fun hello(message: String): Deferred<String> {
        return CompletableDeferred("Message: ${message}")
    }
}
```

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

## Execution Model
Addressables in Orbit can have multiple different execution modes. By default, addressables will use Safe Execution Mode.

### Safe Execution Mode
In safe execution mode Orbit guarantees that calls to addressables can never be processed in parallel. This means that if two clients call an addressable at the same time, they are guaranteed to be processed serially (one after the other) where one call completes before the next one starts.

Orbit guarantees "at most once" delivery, meaning that best attempts will be made to deliver a message one time, but will deliver an error upon failure to deliver.

## Context

If an Addressable extends the AbstractAddressable or AbstractActor class, it will have access to an Orbit-managed context object. The AddressableContext provides access to the AddressableReference (identifier) and the OrbitClient instance. The AddressableReference can be used during the @OnActivate hook to identify which addressable needs to be restored from persistence.

```kotlin
abstract class AbstractAddressable {
    lateinit var context: AddressableContext
}

data class AddressableContext(
    val reference: AddressableReference,
    val client: OrbitClient
)
```
For example, if the addressable is an Actor with a String-type key, 
```kotlin
interface IdentityActor : ActorWithStringKey {
    suspend fun identity(): Deferred<String>    
}

class IdentityActorImpl() : IdentityActor, AbstractActor() {
    override suspend fun identity(): Deferred<String> {
        val id = context.reference
        return context.actorFactory.createProxy<IdentityResolver>().resolve(id).await()
    }
}
```

## Keys
Every addressable in Orbit has a unique key of one of the following types which identifies it.

Orbit Type|JDK Type
---|---
NoKey|N/A
StringKey|String
Int32Key|Integer
Int64Key|Long
