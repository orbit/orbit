# Actors

## Introduction

In Orbit, an actor is an object that interacts with the world using asynchronous messages.

At any time an actor may be active or inactive. Usually the state of an inactive actor will reside in the database. When a message is sent to an inactive actor it will be activated somewhere in the pool of backend servers. During the activation process the actor’s state is read from the database.

Actors are deactivated based on timeout and on server resource usage.

## Keys

Like all addressables, every actor in Orbit has a [key](addressables.md#keys). Additionally, to ensure they are type safe every actor interface must choose only one key type, this is achieved by extending one of the following actor interfaces.

| Actor Interface | JDK Type | Orbit Type |
| :--- | :--- | :--- |
| ActorWithNoKey | N/A | NoKey |
| ActorWithStringKey | String | StringKey |
| ActorWithInt32Key | Integer | Int32Key |
| ActorWithInt64Key | Long | Int64Key |
| ActorWithGuidKey | UUID | GuidKey |

## Runtime Model

Orbit guarantees that only one activation of an actor with a given identity can exist at any one time in the cluster by default. As such, developers do not need to be concerned about keeping multiple activations/instances of an actor synchronized with one another.

By default, Orbit also guarantees that calls to actors can never be processed in parallel using [safe execution mode](addressables.md#safe-execution-mode). This means that developers do not need to worry about concurrent access to an actor. Two calls to an actor can not be processed in parallel. 

