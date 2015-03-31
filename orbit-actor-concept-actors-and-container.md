---
layout : page
title : "Orbit : Actor Concept - Actors and Container"
breadCrumb : "[Orbit](index.html) / [Public Documentation](orbit-public-documentation.html) / [Actors](orbit-actors.html) / [Actor Concepts](orbit-actor-concepts.html)"
next : "orbit-actor-concept-advanced-topics.html"
previous: "orbit-actor-concept-useful-annotations.html"
---
{% include JB/setup %}



-  [Overview](#ActorConcept-ActorsandContainer-Overview)
-  [Using Orbit Container](#ActorConcept-ActorsandContainer-UsingOrbitContainer)



Overview {#ActorConcept-ActorsandContainer-Overview}
----------


Orbit Actors is designed to work with any existing dependency injection and inversion of control containers. Developers are free to choose the solution which makes the most sense for their project.


However, Orbit Actors is designed to work with [Orbit Container](orbit-container.html) out of the box and offer a fully configured dependency injection and inversion of control container. For new projects this path offers a simple pre-configured way to get up and running.


 


Using Orbit Container {#ActorConcept-ActorsandContainer-UsingOrbitContainer}
----------


Using Container with Actors is very simple, there are only two requirements:


-  Your classpath must contain Actors and Container
-  You must start the Container

 

**Starting Container** 
{% highlight java %}
final OrbitContainer container = new OrbitContainer();
container.start();
{% endhighlight %}

Container will automatically start an Actors stage during initialization.  Container will also automatically resolve @Inject and @Config annotations during actor activation without any further work required by the developer.


 


You can see an example of Container and Actors integration in the [chat sample](orbit-sample-chat.html).

