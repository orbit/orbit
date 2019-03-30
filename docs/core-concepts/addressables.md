# Addressables

## Introduction

Any object which can be addressed remotely in Orbit is known as an **Addressable.** This includes [**Actors**](actors.md), [**Observables**](observables.md) and [**Streams**](streams.md). 

Although it is possible to create a raw Addressable manually, this is an advanced topic. Typically you will extend a well known Addressable type such as Actor.

## Making an Object Addressable

Any remotely addressable object must be represented by an interface that extends Addressable, this defines the protocol/contract for communicating with the addressable.

Although it is possible to create a raw Addressable manually, this is an advanced topic. Typically you will extend a well known Addressable type such as Actor.

```kotlin
interface Greeter : Observable {
    fun greet(name: String): Deferred<String>
}
```

## Asynchronous Return Types

Addressables must only contain methods which return asynchronous types \(such as promises\).  
The following return types are currently supported.

| Type | For | Implemented In |
| :--- | :--- | :--- |
| [Deferred](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-deferred/) | Kotlin | orbit-runtime |
| [CompletableDeferred](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-completable-deferred/index.html) | Kotlin | orbit-runtime |
| [CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html) | JDK8+ | orbit-runtime |

## Concrete Implementation

For each addressable interface, **exactly one** addressable ****class must implement it.

```kotlin
class GreeterImpl : Greeter {
    override fun greet(name: String): Deferred<String> = 
        CompletableDeferred("Hi $name")
}
```

If you wish an addressable to be purely abstract, you must annotate it with the @NonConcrete annotation. This is how interfaces such as Actor may be implemented more than once but never directly.

```kotlin
@NonConcrete
interface Consumer : Observable {
    fun consume(obj: Any?): Deferred<Unit>
}
```

## Execution Model

By default, addressables have a strict execution model. 

Orbit guarantees that calls to addressables can never be processed in parallel. This means that if two clients call an addressable at the same time, they are guaranteed to be processed serially \(one after the other\) where one call completes before the next one starts.

