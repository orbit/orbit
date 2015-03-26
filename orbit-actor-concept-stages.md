---
layout : page
title : "Orbit : Actor Concept - Stages"
breadCrumb : "[Orbit](index.html) / [Public Documentation](orbit-public-documentation.html) / [Actors](orbit-actors.html) / [Actor Concepts](orbit-actor-concepts.html)"
next : "orbit-actor-concept-persistent-state.html"
previous: "orbit-actor-concept-tasks.html"
---
{% include JB/setup %}



-  [Overview](#ActorConcept-Stages-Overview)
-  [Clustering](#ActorConcept-Stages-Clustering)
-  [Modes](#ActorConcept-Stages-Modes)
    -  [Host](#ActorConcept-Stages-Host)
    -  [Front End](#ActorConcept-Stages-FrontEnd)



Overview {#ActorConcept-Stages-Overview}
----------


An Orbit stage is a runtime execution container and is the primary way that developers interact with actors. 


A collection of stages is known as a cluster. Typically one stage will be used for each node in a cluster. 


Stages can be added to or removed from a cluster dynamically.


 


Clustering {#ActorConcept-Stages-Clustering}
----------


The default Orbit configuration uses UDP Multicast combined with JGroups and Infinispan to cluster servers together.


UDP Multicast is not suitable for cloud deployments, so Orbit allows developers to override this default configuration with other options.


Learn more about advanced cluster configuration [here](orbit-advanced-topic-cluster-configuration.html).


 


Modes {#ActorConcept-Stages-Modes}
----------


Orbit offers two default configurations for stages which take on different roles within an Orbit cluster.


 


###Host {#ActorConcept-Stages-Host}


The default configuration for a stage is Host. Host stages participate fully in the cluster. They can host actor activations and manage actor lifetime.


Hosts require the Actor implementations.


 


###Front End {#ActorConcept-Stages-FrontEnd}


The Front End configuration for a stage allows a stage to participate in the cluster as an external entity. They can interact with actors, and register observers but they are not able to host activations or manage actor lifetime.


Front End stages are typically used for offering endpoints to clients which are not able to participate in the Orbit cluster, such as HTTP/Websockets. 


Front Ends only require the Actor interfaces.


 

