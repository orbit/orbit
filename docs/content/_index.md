---
title: "What is Orbit?"
url: /
---

# What is Orbit?

Orbit is a framework to write distributed systems using virtual actors on the JVM. A virtual actor is an object that interacts with the world using asynchronous messages.

At any time an actor may be active or inactive. Usually the state of an inactive actor will reside in the database. When a message is sent to an inactive actor it will be activated somewhere in the pool of backend servers. During the activation process the actor's state is read from the database.

Actors are deactivated based on timeout and on server resource usage.

It is heavily inspired by the [Microsoft Orleans](https://github.com/dotnet/Orleans) project.

# Documentation
The Orbit documentation is located on this site. Please see the sidebar for navigation.

# Source Code & Other Projects
The core orbit framework and runtime code is located in the [orbit/orbit](https://github.com/orbit/orbit) project.

# Sample Application
A sample application has been built to demonstrate the most common scenarios for integrating with an Orbit Server at [orbit/orbit-sample](https://github.com/orbit/orbit-sample)
