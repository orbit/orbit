---
title: "Migration from Orbit 1.x"
weight: 0
---

## Orbit 2 Philosophy
Orbit 2 accomplishes the same basic goals as Orbit 1.x, but via a whole different approach. Orbit 1.x takes the form of a component included in an application utilizing distributed storage to coordinate messaging between service instances. Orbit 2 has taken all the responsibilities of tracking Actors and routing messaging into a standalone, scalable, and multi-tenant mesh service. Applications connect to the mesh over GRPC through a thin client library that manages the connection, leasing, and message delivery.

This change in overall design leads to greater reliability, fault tolerance, scalability, and helps with ZDT deployment. 


## New concepts
### Addressables

Actors are still very much a first-class focus of Orbit 2, but have been designed as part of a higher level of abstraction. Addressables are the core message target, with Actors as a special case. An Addressable is any entity that can receive a message.

Read more about [Addressables](/getting-started/addressables)

### Orbit Client

Integrating Orbit Client into your application is the easiest way to work with the Orbit Server Mesh. It handles connecting to the mesh, manages the lifetime of that connection, and helps easily send and receive messages to Addressables anywhere.

Read more about [Orbit Client](/client)

## Outdated concepts
### Stage

The stage has been deprecated in favor of the simpler interface of the Orbit Client.

## Future functional parity

Some features of Orbit 1.x have not yet made it into Orbit 2, but will be prioritized based on need and available development time.

### Streams
### Extensions
### Reentrancy
### Placement group?


