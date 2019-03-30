# Actors

## Introduction

In Orbit, an actor is an object that interacts with the world using asynchronous messages.

At any time an actor may be active or inactive. Usually the state of an inactive actor will reside in the database. When a message is sent to an inactive actor it will be activated somewhere in the pool of backend servers. During the activation process the actor’s state is read from the database.

Actors are deactivated based on timeout and on server resource usage.

## Runtime Model

Orbit guarantees that only one activation of an actor with a given identity can exist at any one time in the cluster. Orbit also guarantees that calls to actors can never be processed in parallel.

As such, developers do not need to be concerned about keeping multiple activations/instances of an actor synchronized with one another.

Finally, this also means that developers do not need to worry about concurrent access to an actor. Two calls to an actor can not be processed in parallel.

