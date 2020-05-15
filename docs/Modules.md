# Modules
The main Orbit project is split into several modules for client and server.

# Client
## orbit-client
A JVM library for applications interfacing with an Orbit cluster. It handles maintaining a connection to the mesh, leasing addressables, and routing messages. It will be the main entrypoint for most developers.

Gradle:
```kotlin
implementation("cloud.orbit:orbit-client:{{ release }}")
implementation("cloud.orbit:orbit-client:{{ book.release }}")
```

# Server
Orbit can be run as a packaged service without having to delve into the server modules. However, if a more customized version of Orbit is needed, these modules can be used to build the server suitable to the task.

## orbit-server
This is the main implementation of the Orbit Server cluster node. It handles the client connections, mesh connections, authorization, node and addressable leases, and message routing.

## orbit-application
Default hosting application for orbit-server. Decodes settings from a file and initiates the server runtime.

## orbit-proto
Contains the protobuf definitions for communicating with the service and helper methods for translation to internal types.

## orbit-server-etcd
Implementations of the Node Directory and Addressable Directory against an etcd store. By default orbit-server will use in-memory directories, but those won't support clustering multiple Orbit Server nodes nor will values persist between restarts.

## orbit-server-prometheus
Implementation of a Prometheus endpoint for exposing metrics gathered by [micrometer](https://micrometer.io).


## orbit-shared & orbit-util
Shared and utility classes mostly to support internal operations
