# Addressables

## Introduction

Any object which can be addressed remotely in Orbit is known as an **Addressable.** This includes [**Actors**](actors.md), [**Observables**](observables.md) and [**Streams**](streams.md). 

## Making an Object Addressable

Any remotely addressable object must be represented by an interface that extends Addressable, this defines the protocol/contract for communicating with the addressable.

Although it is possible to create a raw Addressable manually, this is an advanced topic. Typically you will extend a well known Addressable type such as Actor.

{% code-tabs %}
{% code-tabs-item title="Kotlin" %}
```kotlin
interface Greeter : Observable {
    fun greet(name: String): Deferred<String>
}
```
{% endcode-tabs-item %}
{% endcode-tabs %}

{% code-tabs %}
{% code-tabs-item title="Java" %}
```java
interface Greeter extends Observable {
    CompletableFuture<String> greet(String name);
}
```
{% endcode-tabs-item %}
{% endcode-tabs %}

## Asynchronous Return Types

Addressables must only contain methods which return asynchronous types \(such as promises\).  
The following return types are currently supported.

| Type | For | Implemented In |
| :--- | :--- | :--- |
| [Deferred](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-deferred/) | Kotlin | orbit-runtime |
| [CompletableDeferred](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-completable-deferred/index.html) | Kotlin | orbit-runtime |
| [CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html) | JDK8+ | orbit-runtime |

## Concrete Implementation

For each addressable interface, **exactly one** addressable class must implement it.

{% code-tabs %}
{% code-tabs-item title="Kotlin" %}
```kotlin
class GreeterImpl : Greeter {
    override fun greet(name: String): Deferred<String> = 
        CompletableDeferred("Hi $name")
}
```
{% endcode-tabs-item %}
{% endcode-tabs %}

{% code-tabs %}
{% code-tabs-item title="Java" %}
```java
public class GreeterImpl implements Greeter {
    @Override
    public CompletableFuture<String> greet(String name) {
        return CompletableFuture.completedFuture("Hi " + name);
    }
}
```
{% endcode-tabs-item %}
{% endcode-tabs %}

If you wish an addressable to be purely abstract, you must annotate it with the @NonConcrete annotation. This is how interfaces such as Actor may be implemented more than once but never directly.

{% code-tabs %}
{% code-tabs-item title="Kotlin" %}
```kotlin
@NonConcrete
interface Consumer : Observable {
    fun consume(obj: Any?): Deferred<Unit>
}
```
{% endcode-tabs-item %}
{% endcode-tabs %}

{% code-tabs %}
{% code-tabs-item title="Java" %}
```java
@NonConcrete
interface Consumer extends Observable {
    CompletableFuture consume(Object obj);
}
```
{% endcode-tabs-item %}
{% endcode-tabs %}

## Execution Model

By default, addressables have a strict execution model. 

Orbit guarantees that calls to addressables can never be processed in parallel. This means that if two clients call an addressable at the same time, they are guaranteed to be processed serially \(one after the other\) where one call completes before the next one starts.

## Lifecycle

The lifecycle of an addressable can be managed by Orbit \(such as with Actors\) or manually by a user \(such as with Observers\).

When the lifecycle is managed by Orbit there are certain additional features available that are not available for non-managed addressables.

### ActivatedAddressable

Implementations of managed addressables may extend ActivatedAddressable which gives access to the `context` member variable, this exposes certain informattion about the addressable and runtime that would otherwise not be available.

The following is an example of how AbstractActor is implemented in Orbit and what all actors gain as a result.

{% code-tabs %}
{% code-tabs-item title="Kotlin" %}
```kotlin
abstract class AbstractActor : ActivatedAddressable()

class IdentityActorImpl : IdentityActor, AbstractActor() {
    override fun identity(): Deferred<String> {
        return this.context.reference.toString()
    }
}
```
{% endcode-tabs-item %}
{% endcode-tabs %}

### Lifecycle Events

Implementations of managed addressables may use lifecycle events. A  method in each implementation may be annotated with `@OnActivate` or `@OnDeactivate` and it will automatically be invokved by Orbit.

{% code-tabs %}
{% code-tabs-item title="Kotlin" %}
```kotlin
class LifecycleEventActorImpl : LifecycleEventActor, AbstractActor() {
    @OnActivate
    fun onActivate(): Deferred<Unit> {
        // Do something
        return CompletableDeferred(Unit)
    }

    @OnDeactivate
    fun onDeactivate(): Deferred<Unit> {
        // Do something
        return CompletableDeferred(Unit)
    }
```
{% endcode-tabs-item %}
{% endcode-tabs %}

These methods must return asynchronous results as specified earlier in this document. The return value is ignored.

