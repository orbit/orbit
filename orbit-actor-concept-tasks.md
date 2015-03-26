---
layout : page
title : "Orbit : Actor Concept - Tasks"
breadCrumb : "[Orbit](index.html) / [Public Documentation](orbit-public-documentation.html) / [Actors](orbit-actors.html) / [Actor Concepts](orbit-actor-concepts.html)"
next : "orbit-actor-concept-stages.html"
previous: "orbit-actor-concept-actors.html"
---
{% include JB/setup %}



-  [Overview](#ActorConcept-Tasks-Overview)



Overview {#ActorConcept-Tasks-Overview}
----------


In Orbit, a Task represents the result of an asynchronous unit of work. It is based on a Java CompletableFuture and offers methods for checking if a unit of work is complete, waiting for a unit of work to complete or getting the result of a unit of work.


Orbit Tasks also offer static helper methods to perform common actions such as waiting for a List of tasks to complete or creating a completed Task from a value.

