# Overview
In Orbit actor state is typically handled as part of the system itself rather than storage strategies being entirely defined by the developer.

State is automatically retrieved when an actor is activated. Writing state is developer defined.

# Working with Persistent State
## Adding State

Adding active record state to an actor in Orbit is simple. When extending AbstractActor the developer simply passes a state object as a generic parameter.

The state object must be serializeable.

```kotlin
public class StatefulActor extends AbstractActor<StatefulActor.State> implements Some
{
    public static class State
    {
        String lastMessage;
    }
}
```

## Accessing State

Accessing active record state in a stateful actor is simple. The state methods provides access to the current state.
 ```kotlin
public Task doSomeState()
{
    System.out.println(state().lastMessage);
    state().lastMessage = "Meep";
    return Task.done();
}
```
 
## Retrieving State
Active record state is automatically retrieved when an actor is activated.

Developers can also manually re-retrieve the state using the readState method.

```kotlin
public Task doReadState()
{
    await(readState());
    // New state is accessible here	
    return Task.done();
}
```

## Writing State
The writing of active record state is determined only by developers, Orbit will not automatically write state.

```kotlin
public Task doWriteState()
{
    return writeState();
}
```

## Clearing State
While actors are never created or destroyed in Orbit, developers can choose to clear an actors state if they wish.

```kotlin
public Task doClearState()
{
    return clearState();
}
```

## Writing State On Deactivation
Sometimes it is desirable to write state on actor deactivation, this ensures that the latest state is persisted once the actor has been deactivated.

```kotlin
@Override
public Task deactivateAsync()
{
    await(writeState());
    return super.deactivateAsync();
}
```