# Overview
In order to allow code which is external to the actor framework to receive notifications from actors, Orbit supports a concept of Observers.

Observers allow external code to register with an actor. The actor is then able to notify the external code of actions which are happening within it.

Orbit provides the network layer, addressability and messaging to the developer so they don’t need to be concerned about the low level details of how an observer notification is handled.

**Important Note**: Using observers is still a valid way to work with the framework, but in many cases [[streams|Concepts: Streams]] may be a better fit for new applications. Make a decision based on your specific use case.

# Working With Observers
## Observer Interfaces
Like actors, observers are created by defining an interface.

Observer interfaces typically live in the same location as Actor Interfaces.

```java
public interface SomeObserver extends ActorObserver
{
    @OneWay
    Task someEvent(String message);
}
```
* Observers must extend Orbit’s ActorObserver interface.
* Like Actor Interfaces, all methods must return an Orbit Task.
* It is often desirable for observer messages to be OneWay.

## Implementing An Observer
No special actions are required to implement an observer interface.

The implementation of an Observer Interface is usually contained within the external code (such as Frontend).

```java
SomeObserver observer = new SomeObserver()
{
    @Override
    public Task someEvent(String message)
    {
        return Task.done();
    }
};
```
* The interface is implemented like any other Java interface.

## Using the Observer
You are now free to use the created observer object, and pass it to an Orbit actor.

```java
SomeActor actor = Actor.getReference(SomeActor.class, "0");
actor.someMethod(observer).join();
```
* Orbit will automatically handle creating the addressable reference to the local object for you.
 

## In The Actor
### Registering Observers

Orbit offers an observer collection named ObserverManager which can be used by actors which need to call observers.

```java
private ObserverManager<SomeObserver> observers = new ObserverManager<>();
 
public Task someMethod(SomeObserver observer)
{
    observers.addObserver(observer);
    return Task.done();
}
 

### Notifying Observers

Finally, you are easily able to notify all observers of an event from your Actor.

```java
observers.notifyObservers(o -> o.someEvent("Hello There"));
```
 

### Persisting Observers

There are no special requirements for persisting observers. 

```java
public class RandomActor extends AbstractActor<RandomActor.State> implements Random
{
    public static class State
    {
        public ObserverManager<ISomeObserver> observers = new ObserverManager<>();
    }
}
```
* Developers should be careful when persisting observers and see the Removing Dead Observers section below to understand the restrictions.

## Removing Dead Observers
It is perfectly possible and valid in Orbit to persist observers as state. However, when developers do this they need to be careful.

Observers that were registered are not guaranteed to be available once an actor’s state has been restored. Developers should ping each observer to ensure it still exists.

The ObserverManager offers a simple way to do this, as demonstrated below:

```java
@Override
public Task activateAsync()
{
    return super.activateAsync().thenRun(() ->
    {
        state().observers.cleanup();
    });
}
```