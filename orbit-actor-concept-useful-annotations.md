---
layout : page
title : "Orbit : Actor Concept - Useful Annotations"
breadCrumb : "[Orbit](index.html) / [Public Documentation](orbit-public-documentation.html) / [Actors](orbit-actors.html) / [Actor Concepts](orbit-actor-concepts.html)"
next : "orbit-actor-concept-actors-and-container.html"
previous: "orbit-actor-concept-observers.html"
---
{% include JB/setup %}



-  [Brief](#ActorConcept-UsefulAnnotations-Brief)
-  [Actor Interface Annotations](#ActorConcept-UsefulAnnotations-ActorInterfaceAnnotations)
    -  [StatelessWorker](#ActorConcept-UsefulAnnotations-StatelessWorker)
    -  [NoIdentity](#ActorConcept-UsefulAnnotations-NoIdentity)
-  [Message Annotations](#ActorConcept-UsefulAnnotations-MessageAnnotations)
    -  [OneWay](#ActorConcept-UsefulAnnotations-OneWay)



Brief {#ActorConcept-UsefulAnnotations-Brief}
----------


Orbit offers several useful annotations for customizing the behavior of actors or messages.


Useful annotations are listed below.


 


Actor Interface Annotations {#ActorConcept-UsefulAnnotations-ActorInterfaceAnnotations}
----------


###StatelessWorker {#ActorConcept-UsefulAnnotations-StatelessWorker}


{% highlight java %}
@StatelessWorker
public interface IMyActor extends IActor {}
{% endhighlight %}

Causes your actor to be a stateless worker. See [stateless workers](orbit-actor-concept-stateless-workers.html).


###NoIdentity {#ActorConcept-UsefulAnnotations-NoIdentity}


{% highlight java %}
@NoIdentity
public interface IMyActor extends IActor {}
{% endhighlight %}

Denotes that this actor does have an identity and acts as a singleton. Actor is accessed using getReference() instead of getReference(id).


 


Message Annotations {#ActorConcept-UsefulAnnotations-MessageAnnotations}
----------


###OneWay {#ActorConcept-UsefulAnnotations-OneWay}


{% highlight java %}
@OneWay
public Task someMessage() { return Task.done(); }
{% endhighlight %}

This message is OneWay.  No result (value or status) will be returned, no guarantee that message will be processed.


The Task might contain an exception if there was a problem locating the target object or serializing the message.

