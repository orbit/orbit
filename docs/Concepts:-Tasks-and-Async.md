# Tasks
Tasks are the basic unit of work in Orbit Actors and understanding their purpose and power is important to fully leverage the framework. Tasks are used extensively and are the standard return type for any asynchronous messages or actions.

A Task represents a promise to provide the result of an asynchronous unit of work. Tasks may simply represent a complete state, return an explicit result, be cancelled or communicate failures via exceptional completion.

In reality a Task is a thin wrapper over CompletableFuture which is native to Java and offers a few additional features and a common interface.

# Async
Internally Orbit leverages [EA Async](https://www.github.com/electronicarts/ea-async), which allows developers to write asynchronous code in a sequential manner. 

However, EA Async is **not** a transitive dependency in Orbit, so including Orbit will not include async in your project.
We do recommend that you include async in your project as it greatly simplifies how you work with actors, though there is no requirement for this.