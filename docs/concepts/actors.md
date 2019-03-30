# Actors

## Introduction

In Orbit, an actor is an object that interacts with the world using asynchronous messages.

At any time an actor may be active or inactive. Usually the state of an inactive actor will reside in the database. When a message is sent to an inactive actor it will be activated somewhere in the pool of backend servers. During the activation process the actor’s state is read from the database.

Actors are deactivated based on timeout and on server resource usage.

## Runtime Model

Orbit guarantees that only one activation of an actor with a given identity can exist at any one time in the cluster by default. As such, developers do not need to be concerned about keeping multiple activations/instances of an actor synchronized with one another.

By default, Orbit also guarantees that calls to actors can never be processed in parallel using [safe execution mode](addressables.md#safe-execution-mode). This means that developers do not need to worry about concurrent access to an actor. Two calls to an actor can not be processed in parallel. 

## Keys

Like all addressables, every actor in Orbit has a [key](addressables.md#keys). Additionally, to ensure they are type safe every actor interface must choose only one key type, this is achieved by extending one of the following actor interfaces.

| Actor Interface | JDK Type | Orbit Type |
| :--- | :--- | :--- |
| ActorWithNoKey | _N/A_ | NoKey |
| ActorWithStringKey | String | StringKey |
| ActorWithInt32Key | Integer | Int32Key |
| ActorWithInt64Key | Long | Int64Key |
| ActorWithGuidKey | UUID | GuidKey |

## Interface

Before you can implement an actor you must create an interface for it.

{% code-tabs %}
{% code-tabs-item title="Kotlin" %}
```kotlin
interface Greeter : ActorWithNoKey {
    fun greet(name: String): Deferred<String>
}
```
{% endcode-tabs-item %}
{% endcode-tabs %}

{% code-tabs %}
{% code-tabs-item title="Java" %}
```java
interface Greeter extends ActorWithNoKey {
    CompletableFuture<String> greet(String name);
}
```
{% endcode-tabs-item %}
{% endcode-tabs %}

* Actor interfaces must extend an Orbit [actor type](actors.md#keys).
* Interface methods must return an [asynchronous type](addressables.md#asynchronous-return-types).

## Implementation

Once you have created an Actor type, you must offer an implementation of that Actor for Orbit to use.

{% code-tabs %}
{% code-tabs-item title="Kotlin" %}
```kotlin
class GreeterActor : Greeter, AbstractActor() {
    private val logger by logger()

    override fun greet(name: String): Deferred<String> {
        logger.info("I was called by: $name. My identity is ${this.context.reference}")
        return CompletableDeferred("Hello $name!")
    }
}
```
{% endcode-tabs-item %}
{% endcode-tabs %}

{% code-tabs %}
{% code-tabs-item title="Java" %}
```java
public class GreeterActor extends AbstractActor implements Greeter {
    private static Logger logger = Logging.getLogger(GreeterActor.class);

    @Override
    public CompletableFuture<String> greet(String name) {
        logger.info("I was called by: " + name + ". My identity is " + getContext().getReference());
        return CompletableFuture.completedFuture("Hello " + name + "!");
    }
}
```
{% endcode-tabs-item %}
{% endcode-tabs %}

* Actor implementations must extend AbstractActor.
* Actor implementations must implement at least one [concrete](addressables.md#concrete-implementation) interface.
* Only one implementation per [concrete](addressables.md#concrete-implementation) interface is permitted.

## Calling

Actors are called via a proxy. A proxy is created via the Orbit runtime context, typically [Stage](stage.md) or [AddressableContext](addressables.md#context).

### Accessing the Proxy Factory

{% code-tabs %}
{% code-tabs-item title="Kotlin" %}
```kotlin
// Via Stage
stage.actorProxyFactory.getReference<Greeter>()

// From an Addressable
context.runtime.actorProxyFactory.getReference<Greeter>()
```
{% endcode-tabs-item %}
{% endcode-tabs %}

{% code-tabs %}
{% code-tabs-item title="Java" %}
```java
// Via Stage
stage.getActorProxyFactory().getReference(Greeter.class);

// From an Addressable
getContext().getRuntime().getActorProxyFactory().getReference(Greeter.class);
```
{% endcode-tabs-item %}
{% endcode-tabs %}

### Type Safe

The identity [key](actors.md#keys) of an actor is specified by the actor interface. This ensures that access is type safe.

{% code-tabs %}
{% code-tabs-item title="Kotlin" %}
```kotlin
interface MyNoKey : ActorWithNoKey
interface MyStringKey : ActorWithStringKey
interface MyIntKey : ActorWithInt32Key

actorProxyFactory.getReference<MyNoKey>() // Valid
actorProxyFactory.getReference<MyNoKey>("beep") // Compile error
actorProxyFactory.getReference<MyStringKey>("beep") // Valid
actorProxyFactory.getReference<MyIntKey>(1234) // Valid
actorProxyFactory.getReference<MyStringKey>(1234) // Compile error
```
{% endcode-tabs-item %}
{% endcode-tabs %}

{% code-tabs %}
{% code-tabs-item title="Java" %}
```java
public interface MyNoKey extends ActorWithNoKey {}
public interface MyStringKey extends ActorWithStringKey {}
public interface MyIntKey extends ActorWithInt32Key {}

getActorProxyFactory().getReference(MyNoKey.class); // Valid
getActorProxyFactory().getReference(MyNoKey.class, "beep"); // Compile error
getActorProxyFactory().getReference(MyStringKey.class, "beep"); // Valid
getActorProxyFactory().getReference(MyIntKey.class, 1234); // Compile error
getActorProxyFactory().getReference(MyStringKey.class, 123); // Compile error
```
{% endcode-tabs-item %}
{% endcode-tabs %}

