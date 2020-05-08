# Overview
By default, the Orbit execution serializer requires an activation to completely finish processing one request before invoking the next request.
An activation cannot process a new request until all of the dependencies of the returned tasks are complete.

Actor implementation methods may be marked with the Reentrant annotation to indicate that an activation
may process another request while waiting for other tasks to complete. 

Orbit still ensures that only a single-thread may be acting on an actor at any one time, so the resultant behavior seen by developers is that the framework may interleave requests. Developers need to ensure that their actor implementation handles this behavior correctly.

```java
@Override
@Reentrant
public Task<Void> doSomething()
{
    await(sync.task("lock2")); // While this is awaiting, other messages may interleave
    return Task.done();
}
```