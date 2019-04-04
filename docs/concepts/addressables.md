# Addressables

## Introduction

Any object which can be addressed remotely in Orbit is known as an **Addressable.** This includes [**Actors**](actors.md), [**Observers**](observables.md) and [**Streams**](streams.md).

## Making an Object Addressable

Any remotely addressable object must be represented by an interface that extends Addressable, this defines the protocol/contract for communicating with the addressable.

Although it is possible to create a raw addressable manually, this is an advanced topic. Typically you will extend a well known addressble type such as [Actor](actors.md).

{% code-tabs %}
{% code-tabs-item title="Kotlin" %}
```kotlin
interface Greeter : Observer {
    fun greet
(name: String): Deferred<String>
}
```
{% endcode-tabs-item %}
{% endcode-tabs %}

{% code-tabs %}
{% code-tabs-item title="Java" %}
```java
interface Greeter extends Observer {
    CompletableFuture<String> greet(String name);
}
```
{% endcode-tabs-item %}
{% endcode-tabs %}

## Asynchronous Return Types

Addressables must only contain methods which return asynchronous types \(such as promises\).  
The following return types \(and their subtypes\) are currently supported.

| Main Type | Common Subtypes | For | Implemented In |
| :--- | :--- | :--- | :--- |
| [Deferred](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-deferred/) | [CompletableDeferred](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-completable-deferred/) | Kotlin | orbit-runtime |
| [CompletionStage](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html) | [CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html) | JDK8+ | orbit-runtime |

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

If you wish an addressable to be purely abstract, you must annotate it with the `@NonConcrete` annotation. This is how interfaces such as `Actor` may be implemented more than once but never directly.

{% code-tabs %}
{% code-tabs-item title="Kotlin" %}
```kotlin
@NonConcrete
interface Consumer : Observer {
    fun consume(obj: Any?): Deferred<Unit>
}
```
{% endcode-tabs-item %}
{% endcode-tabs %}

{% code-tabs %}
{% code-tabs-item title="Java" %}
```java
@NonConcrete

interface Consumer extends Observer {
    CompletableFuture consume(Object obj);
}
```
{% endcode-tabs-item %}
{% endcode-tabs %}

## Execution Model

Addressables in Orbit can have multiple different execution modes. By default addressables will use [Safe Execution Mode](addressables.md#safe-execution-mode).

### Safe Execution Mode

In safe execution mode Orbit guarantees that calls to addressables can never be processed in parallel. This means that if two clients call an addressable at the same time, they are guaranteed to be processed serially \(one after the other\) where one call completes before the next one starts.

## Context

Addressable implementations which implement `AbstractAddressable` gain access to an Orbit managed context object, this exposes certain information about the addressable and runtime that would otherwise not be available.

The following is an example of how `AbstractActor` is implemented in Orbit and what all actors gain as a result.

{% code-tabs %}
{% code-tabs-item title="Kotlin" %}
```kotlin
abstract class AbstractActor : AbstractAddressable()

class IdentityActorImpl : IdentityActor, AbstractActor() {
    override fun identity(): Deferred<String> {
        val id = context.reference.toString()
        return context.actorProxyFactory.createProxy<IdentityResolver>().resolve(id)
    }
}
```
{% endcode-tabs-item %}
{% endcode-tabs %}

## Lifecycle

The lifecycle of an addressable can be managed by Orbit \(such as with Actors\) or manually by a user \(such as with Observers\).

When the lifecycle is managed by Orbit there are certain additional features available that are not available for non-managed addressables.

### Lifecycle Events

Implementations of managed addressables may use lifecycle events. A method in each implementation may be annotated with `@OnActivate` or `@OnDeactivate` and it will automatically be invoked by Orbit.

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
}
```
{% endcode-tabs-item %}
{% endcode-tabs %}

These methods must return [asynchronous](addressables.md#asynchronous-return-types) results as with other methods which are called by Orbit.

## Keys

Every addressable in Orbit has a unique key of one of the following types which identifies it.

| Orbit Type | JDK Type |
| :--- | :--- |
| NoKey | _N/A_ |
| StringKey | String |
| Int32Key | Integer |
| Int64Key | Long |
| GuidKey | UUID |

