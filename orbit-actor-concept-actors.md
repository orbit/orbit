---
layout : page
title : "Orbit : Actor Concept - Actors"
breadCrumb : "[Orbit](index.html) / [Public Documentation](orbit-public-documentation.html) / [Actors](orbit-actors.html) / [Actor Concepts](orbit-actor-concepts.html)"
next : "orbit-actor-concept-tasks.html"
previous: "orbit-actor-concepts.html"
---
{% include JB/setup %}



-  [Actors](#ActorConcept-Actors-Actors)
-  [Actor Runtime Model](#ActorConcept-Actors-ActorRuntimeModel)
-  [Actor Interfaces](#ActorConcept-Actors-ActorInterfaces)
-  [Actor Implementations](#ActorConcept-Actors-ActorImplementations)
-  [Lifetime](#ActorConcept-Actors-Lifetime)
    -  [Overview](#ActorConcept-Actors-Overview)
    -  [Lifetime Flow](#ActorConcept-Actors-LifetimeFlow)
    -  [Reacting to Activation Events](#ActorConcept-Actors-ReactingtoActivationEvents)



Actors {#ActorConcept-Actors-Actors}
----------


In Orbit, an actor is an object that interacts with the world using asynchronous messages.


At any time an actor may be active or inactive. Usually the state of an inactive actor will reside in the database. When a message is sent to an inactive actor it will be activated somewhere in the pool of backend servers. During the activation process the actor's state is read from the database.


Actors are deactivated based on timeout and on server resource usage.


 


Actor Runtime Model {#ActorConcept-Actors-ActorRuntimeModel}
----------


Orbit guarantees that only one activation of an actor with a given identity can exist at any one time in the cluster. Orbit also guarantees that calls to actors can never be processed in parallel.


As such, developers do not need to be concerned about keeping multiple activations/instances of an actor synchronized with one another.


Finally, this also means that developers do not need to worry about concurrent access to an actor. Two calls to an actor can not be processed in parallel.


 


Actor Interfaces {#ActorConcept-Actors-ActorInterfaces}
----------


Before you can implement an actor you must create an interface for it.

**IHello.java** 
{% highlight java %}
package com.example.orbit.hello;

import com.ea.orbit.actors.IActor;
import com.ea.orbit.concurrent.Task;
 
public interface IHello extends IActor
{
    Task<String> sayHello(String greeting);
}
{% endhighlight %}

-  Actor interfaces must extend Orbit's IActor interface
-  Methods in actor interfaces must return an Orbit [Task](orbit-actor-concept-tasks.html).

Actor Implementations {#ActorConcept-Actors-ActorImplementations}
----------


Once you have created an Actor, you must offer an implementation of that Actor for Orbit to use.

**HelloActor.java** 
{% highlight java %}
package com.example.orbit.hello;

import com.ea.orbit.actors.runtime.OrbitActor;
import com.ea.orbit.concurrent.Task;
 
public class HelloActor extends OrbitActor implements IHello
{
    public Task<String> sayHello(String greeting)
    {
        getLogger().info("Here: " + greeting);
        return Task.fromValue("You said: '" + greeting
                + "', I say: Hello from " + System.identityHashCode(this) + " !");
    }
}


{% endhighlight %}

-  Actor implementations must extend OrbitActor
-  Actor implementations must implement a single actor interface
-  Only one implementation per actor interface is permitted

 


Lifetime {#ActorConcept-Actors-Lifetime}
----------


###Overview {#ActorConcept-Actors-Overview}


Unlike other frameworks, there is no explicit lifetime management for actors in Orbit.


Actors are never created or destroyed, they always exist conceptually. 


Not all actors in Orbit will be in-memory in the cluster at a given time, actors which are in-memory are "activated" and those which are not are "deactivated". The process of an actor being created in-memory is known as "Activation" and the process of an actor being removed from memory is known as "Deactivation". 


Activation and Deactivation is transparent to the developer.


 


###Lifetime Flow {#ActorConcept-Actors-LifetimeFlow}


When a message is sent to a given actor reference, the framework will perform the following actions:


1.  Check if the actor is already activated in the cluster
    1.  If so, forward the message
    2.  If not, proceed to step 2
2.  Check if the actor has persisted state
    1.  If so, load the state, activate the actor and forward the message
    2.  If not, proceed to step 3
3.  Activate a default actor for the actor type and forward the message

 


###Reacting to Activation Events {#ActorConcept-Actors-ReactingtoActivationEvents}


While the lifetime and activation/deactivation of an actor is transparent, it is sometimes desirable to perform certain actions during activation or deactivation.


For instance: Checking if observers still exist, persisting state that has not yet been written etc.


Orbit offers 2 methods that can be overridden in an actor to allow developers to add logic.

**Activation Events** 
{% highlight java %}
public class SomeActor extends OrbitActor implements ISomeActor
{
    @Override
    public Task activateAsync()
    {
        return super.activateAsync();
    }

    @Override
    public Task deactivateAsync()
    {
        return super.deactivateAsync();
    }
}
{% endhighlight %}
