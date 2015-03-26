---
layout : page
title : "Orbit : Actor Overview"
breadCrumb : "[Orbit](index.html) / [Public Documentation](orbit-public-documentation.html) / [Actors](orbit-actors.html)"
next : "orbit-actor-concepts.html"
previous: "orbit-actors.html"
---
{% include JB/setup %}



-  [Brief](#ActorOverview-Brief)
-  [Dependencies](#ActorOverview-Dependencies)
-  [FAQs](#ActorOverview-FAQs)
    -  [Why not use another Actor framework?](#ActorOverview-WhynotuseanotherActorframework_)
-  [References](#ActorOverview-References)



Brief {#ActorOverview-Brief}
----------


Orbit Actors is a framework to write distributed systems using virtual actors. It was developed by [BioWare](http://www.bioware.com/), a division of [Electronic Arts](http://www.ea.com/), and is heavily inspired by the [Orleans](https://github.com/dotnet/Orleans) project.


A virtual actor is an object that interacts with the world using asynchronous messages.  At any time an actor may be active or inactive. Usually the state of an inactive actor will reside in the database. When a message is sent to an inactive actor it will be activated somewhere in the pool of backend servers. During the activation process the actor's state is read from the database. Actors are deactivated based on timeout and on server resource usage.


 


Dependencies {#ActorOverview-Dependencies}
----------


The Orbit Actor framework can be used independently of other orbit modules. It is highly modular and configurable. Cluster networking, serialization, and actor persistence are replaceable.


The Actor framework can be used with dependency injection frameworks or configured manually.


 


FAQs {#ActorOverview-FAQs}
----------


####Why not use another Actor framework? {#ActorOverview-WhynotuseanotherActorframework_}


Orbit Actors solves many of the problems that make working with actor frameworks difficult. It aims to be lightweight and simple to use, providing location transparency and state as a first-class citizen.


 


References {#ActorOverview-References}
----------


-  [Actor Model](http://en.wikipedia.org/wiki/Actor_model)
-  [Orleans](https://github.com/dotnet/Orleans)
-  [Akka](http://akka.io/)
