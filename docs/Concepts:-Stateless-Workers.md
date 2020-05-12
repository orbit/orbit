# Overview
## Standard Behavior
Orbit’s standard runtime guarantees ensure that more than one activation of an actor with a given identity can not exist in an Orbit cluster at any given time.  This is typically desired behavior. It ensures that an actors internal state is always consistent, this is desirable even for actors that do not have persistent state as the internal transient state is usually important (non-persisted chat room for example).

## Stateless Workers
In some specific scenarios this strict runtime behavior is not desired. As a consequence, Orbit’s standard behavior adds unnecessary latency to Actors that do not maintain an internal state.

For these scenarios, Orbit offers the concept of Stateless Workers.

* More than one stateless worker actor with a given identity can exist at once
* Orbit will create stateless workers based on demand
* Stateless workers must not rely on any state existing between calls

# Creating A Stateless Worker
Creating a stateless worker simply requires adding an annotation to your Actor Interface.

```kotlin
import cloud.orbit.annotation.StatelessWorker;
import cloud.orbit.actors.Actor;
import cloud.orbit.concurrent.Task;

@StatelessWorker
public interface SomeStatelessWorker extends Actor
{
    Task<String> someMethod(String message);
}
```

The @StatelessWorker annotation on the actor interface makes this actor a Stateless Worker.