# Overview
Orbit offers developers streams, a system for developing asynchronous and event-based logic. Streams are an extension of the observer pattern and like the Orbit actor framework, the primary aim is to abstract away complex logic such as threading, networking and concurrency into an easy to use API.

The Orbit Streams implementation is designed to scale horizontally to support many thousands of subscribers without significant overhead.

# Using Streams
Applications interact with Orbit streams via an interface which is similar to the virtual actor abstraction, AsyncStream. 

### Subscribe to Stream

```kotlin
private StreamSubscriptionHandle<String> handle;

public Task subscribeToStream()
{
    // Get stream handle
    final AsyncStream<String> stream = AsyncStream.getStream(String.class, "myStream");

    // Subscribe to stream
    final Task<StreamSubscriptionHandle<String>> subscribeTask = stream.subscribe(d ->
    {
        System.out.println(d);
        return Task.done();
    });

    // Wait for subscription confirmation
    handle = await(subscribeTask);

    return Task.done();
}
```

### Unsubscribe from Stream

```kotlin
private StreamSubscriptionHandle<String> handle;

public Task unsubscribeFromStream()
{
    // Get stream handle
    final AsyncStream<String> stream = AsyncStream.getStream(String.class, "myStream");

    // Unsubscribe request
    final Task unsubscribeTask = stream.unsubscribe(handle);

    // Wait for unsubscribe to complete
    return unsubscribeTask;
}
```

### Publish to Stream

```kotlin
public Task publishToStream()
{
    // Get stream handle
    final AsyncStream<String> stream = AsyncStream.getStream(String.class, "myStream");

    // Publish to stream
    final Task publishTask = stream.publish("Hello");

    // Wait for publish to complete
    return publishTask;
}
```

## Lifetime
Streams make use of the Orbit virtual actor framework for lifetime management. Conceptually a stream always exists and there is no need to create or destroy a stream.

Streams are persistent, so if the stage a stream is hosted on goes down, or the stream is idle and gets deactivated, further interactions with the stream will cause another activation and the observer list will be restored.

Subscribers are not persisted in the same way as streams, subscribers which are destroyed or unavailable will be implicitly unsubscribed. 

## With Actors
Streams work well with virtual actors within the Orbit framework. 

Actors are free to subscribe to streams and additionally the framework will ensure that stream messages are processed according to the threading guarantees for actors.

It’s important to note that actors are subject to the same lifetime restrictions as any other subscriber. For instance, if a virtual actor that is subscribed to a stream is deactivated it will be removed as an observer and the stream system will not cause actor activation. As such, it’s important that actors subscribe to a stream on every activation if required.