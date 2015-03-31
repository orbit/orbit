---
layout : page
title : "Orbit : Actor Concept - Observers"
breadCrumb : "[Orbit](index.html) / [Public Documentation](orbit-public-documentation.html) / [Actors](orbit-actors.html) / [Actor Concepts](orbit-actor-concepts.html)"
next : "orbit-actor-concept-useful-annotations.html"
previous: "orbit-actor-concept-reminders.html"
---
{% include JB/setup %}



-  [Overview](#ActorConcept-Observers-Overview)
-  [Working With Observers](#ActorConcept-Observers-WorkingWithObservers)
    -  [Observer Interfaces](#ActorConcept-Observers-ObserverInterfaces)
    -  [Implementing An Observer](#ActorConcept-Observers-ImplementingAnObserver)
    -  [Using the Observer](#ActorConcept-Observers-UsingtheObserver)
    -  [In The Actor](#ActorConcept-Observers-InTheActor)
        -  [Registering Observers](#ActorConcept-Observers-RegisteringObservers)
        -  [Notifying Observers](#ActorConcept-Observers-NotifyingObservers)
        -  [Persisting Observers](#ActorConcept-Observers-PersistingObservers)
    -  [Removing Dead Observers](#ActorConcept-Observers-RemovingDeadObservers)



Overview {#ActorConcept-Observers-Overview}
----------


In order to allow code which is external to the actor framework to receive notifications from actors, Orbit supports a concept of Observers.


Observers allow external code to register with an actor. The actor is then able to notify the external code of actions which are happening within it.


Orbit provides the network layer, addressability and messaging to the developer so they don't need to be concerned about the low level details of how an observer notification is handled.


 


Working With Observers {#ActorConcept-Observers-WorkingWithObservers}
----------


###Observer Interfaces {#ActorConcept-Observers-ObserverInterfaces}


Like actors, observers are created by defining an interface.


Observer interfaces typically live in the same location as Actor Interfaces.

**Observer Interface** 
{% highlight java %}
public interface ISomeObserver extends IActorObserver
{
    @OneWay
    Task someEvent(String message);
}
{% endhighlight %}

-  Observers must extend Orbit's IActorObserver interface.
-  Like Actor Interfaces, all methods must return an Orbit Task.
-  It is often desirable for observer messages to be [OneWay](orbit-actor-concept-useful-annotations.html).

###Implementing An Observer {#ActorConcept-Observers-ImplementingAnObserver}


No special actions are required to implement an observer interface.


The implementation of an Observer Interface is usually contained within the external code (such as Frontend).

**Observer Implementation** 
{% highlight java %}
ISomeObserver observer = new ISomeObserver()
{
    @Override
    public Task someEvent(String message)
    {
        return Task.done();
    }
};
{% endhighlight %}

-  The interface is implemented like any other Java interface.

###Using the Observer {#ActorConcept-Observers-UsingtheObserver}


You are now free to use the created observer object, and pass it to an Orbit actor.

**Register Observer** 
{% highlight java %}
ISomeActor actor = stage.getReference(ISomeActor.class, "0");
actor.someMethod(observer).join();
{% endhighlight %}

-  Orbit will automatically handle creating the addressable reference to the local object for you.

 


###In The Actor {#ActorConcept-Observers-InTheActor}


####Registering Observers {#ActorConcept-Observers-RegisteringObservers}


Orbit offers an observer collection named ObserverManager which can be used by actors which need to call observers.

**Registering Observers** 
{% highlight java %}
private ObserverManager<ISomeObserver> observers = new ObserverManager<>();
 
public Task someMethod(ISomeObserver observer)
{
    observers.addObserver(observer);
    return Task.done();
}
{% endhighlight %}

 


####Notifying Observers {#ActorConcept-Observers-NotifyingObservers}


Finally, you are easily able to notify all observers of an event from your Actor.

**Notify Observers** 
{% highlight java %}
observers.notifyObservers(o -> o.someEvent("Hello There"));
{% endhighlight %}

 


####Persisting Observers {#ActorConcept-Observers-PersistingObservers}


There are no special requirements for persisting observers. 

**Persistent Observers** 
{% highlight java %}
public class RandomActor extends OrbitActor<RandomActor.State> implements IRandom
{
    public static class State
    {
        public ObserverManager<ISomeObserver> observers = new ObserverManager<>();
    }
}
{% endhighlight %}

Developers should be careful when persisting observers and see the Removing Dead Observers section below to understand the restrictions.


 


###Removing Dead Observers {#ActorConcept-Observers-RemovingDeadObservers}


It is perfectly possible and valid in Orbit to persist observers as state. However, when developers do this they need to be careful.


Observers that were registered are not guaranteed to be available once an actor's state has been restored. Developers should ping each observer to ensure it still exists.


The ObserverManager offers a simple way to do this, as demonstrated below:

**Remove Dead Observers** 
{% highlight java %}
@Override
public Task activateAsync()
{
    return super.activateAsync().thenRun(() ->
    {
        state().observers.cleanup();
    });
}
{% endhighlight %}
