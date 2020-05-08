# Overview
An Orbit stage is a runtime execution container and is the primary way that developers interact with actors. 

A collection of stages is known as a cluster. Typically one stage will be used for each node in a cluster. 

Stages can be added to or removed from a cluster dynamically.

# Clustering
The default Orbit configuration uses UDP Multicast combined with JGroups and Infinispan to cluster servers together using a unique cluster name.

UDP Multicast is not suitable for cloud deployments, so Orbit allows developers to [[override|Advanced: JGroups Cluster Configuration]] this default configuration with other options.

# Modes
## Host
The default configuration for a stage is Host. Host stages participate fully in the cluster. They can host actor activations and manage actor lifetime.

Hosts require the Actor implementations.

## Client
The client configuration for a stage allows a stage to participate in the cluster as an external entity. They can interact with actors, and register observers but they are not able to host activations or manage actor lifetime.

Client stages are typically used for offering endpoints to clients which are not able to participate in the Orbit cluster, such as HTTP/Websockets. 

Client stages only require the Actor interfaces.