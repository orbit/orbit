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


Tasks are the basic unit of work in Orbit and understanding their purpose and power is important to fully leverage the framework. Tasks are used extensively and are the standard return type for any asynchronous messages or actions.


A Task represents a promise to provide the result of an asynchronous unit of work. Tasks may simply represent a complete state, return an explicit result, be cancelled or communicate failures via exceptional completion.


In reality a Task is a thin wrapper over CompletableFuture which is native to Java and offers a few additional features and a common interface.

