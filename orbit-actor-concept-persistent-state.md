---
layout : page
title : "Orbit : Actor Concept - Persistent State"
breadCrumb : "[Orbit](index.html) / [Public Documentation](orbit-public-documentation.html) / [Actors](orbit-actors.html) / [Actor Concepts](orbit-actor-concepts.html)"
next : "orbit-actor-concept-stateless-workers.html"
previous: "orbit-actor-concept-stages.html"
---
{% include JB/setup %}



-  [Overview](#ActorConcept-PersistentState-Overview)
-  [Working With State](#ActorConcept-PersistentState-WorkingWithState)
    -  [Adding State](#ActorConcept-PersistentState-AddingState)
    -  [Accessing State](#ActorConcept-PersistentState-AccessingState)
    -  [Retrieving State](#ActorConcept-PersistentState-RetrievingState)
    -  [Writing State](#ActorConcept-PersistentState-WritingState)
    -  [Clearing State](#ActorConcept-PersistentState-ClearingState)
    -  [Writing State On Deactivation](#ActorConcept-PersistentState-WritingStateOnDeactivation)
-  [Storage Providers](#ActorConcept-PersistentState-StorageProviders)



 


Overview {#ActorConcept-PersistentState-Overview}
----------


In Orbit actor state is typically handled as part of the system itself rather than storage strategies being entirely defined by the developer.


State is automatically retrieved when an actor is activated. Writing state is developer defined.


Interacting with any state method will always result in a standard Orbit Task being returned.


Working With State {#ActorConcept-PersistentState-WorkingWithState}
----------


###Adding State {#ActorConcept-PersistentState-AddingState}


Adding state to an actor in Orbit is simple. When extending OrbitActor the developer simply passes a state object as a generic parameter.


The state object must be serializeable.

**Stateful Actor** 
{% highlight java %}
public class StatefulActor extends OrbitActor<StatefulActor.State> implements ISomeActor
{
    public static class State
    {
        String lastMessage;
    }
}
{% endhighlight %}

 


###Accessing State {#ActorConcept-PersistentState-AccessingState}


Accessing state in a stateful actor is simple. The state methods provides access to the current state.

**Accessing State** 
{% highlight java %}
public Task doSomeState()
{
    System.out.println(state().lastMessage);
    state().lastMessage = "Meep";
    return Task.done();
}
{% endhighlight %}

 


###Retrieving State {#ActorConcept-PersistentState-RetrievingState}


State is automatically retrieved when an actor is activated.


Developers can also manually re-retrieve the state using the readState method.

**Retrieving State** 
{% highlight java %}
public Task doReadState()
{
    readState().join();
    // New state is accessible here	
    return Task.done();
}
{% endhighlight %}

 


###Writing State {#ActorConcept-PersistentState-WritingState}


The writing is of State is determined only by developers, Orbit will not automatically write state.

**Writing State** 
{% highlight java %}
public Task doWriteState()
{
    return writeState();
}
{% endhighlight %}

 


###Clearing State {#ActorConcept-PersistentState-ClearingState}


While actors are never created or destroyed in Orbit, developers can choose to clear an actors state if they wish.

**Clearing State** 
{% highlight java %}
public Task doClearState()
{
    return clearState();
}
{% endhighlight %}

 


###Writing State On Deactivation {#ActorConcept-PersistentState-WritingStateOnDeactivation}


Sometimes it is desirable to write state on actor deactivation, this ensures that the latest state is persisted once the actor has been deactivated.

**Writing State on Deactivation** 
{% highlight java %}
@Override
public Task deactivateAsync()
{
    return writeState().thenCompose(x -> super.deactivateAsync());
}
{% endhighlight %}

 


Storage Providers {#ActorConcept-PersistentState-StorageProviders}
----------


The underlying storage mechanism for state in Orbit is determined by the Storage Provider.


Developers are free to add storage providers for any provider they wish (Databases etc).


Orbit offers built in providers for the following storage systems:


-  MongoDB

 

