# Addressables

## Introduction

Any object which can be addressed remotely in Orbit is known as an **Addressable.** This includes [**Actors**](actors.md), [**Observables**](observables.md) and [**Streams**](streams.md).

Although it is possible to implement a raw Addressable manually, this is an advanced topic. Typically you will implement a well known Addressable type such as Actor.

## Asynchronous Return Types

Addressables must only contain methods which return asynchronous types \(such as promises\).  
The following return types are currently supported.

| Type | For | Implemented In |
| :--- | :--- | :--- |
| [Deferred](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-deferred/) | Kotlin | orbit-runtime |
| [CompletableDeferred](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-completable-deferred/index.html) | Kotlin | orbit-runtime |
| [CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html) | JDK8+ | orbit-runtime |

