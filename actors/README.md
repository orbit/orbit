Orbit Actors
============

Orbit Actors is a framework to write distributed systems using virtual actors. It was developed by [BioWare](http://www.bioware.com), a division of [Electronic Arts](http://www.ea.com), and is heavily inspired by the [Orleans](https://github.com/dotnet/Orleans) project.

A virtual actor is an object that interacts with the world using asynchronous messages.

At any time an actor may be active or inactive. Usually the state of an inactive actor will reside in the database.
When a message is sent to an inactive actor it will be activated somewhere in the pool of backend servers.
During the activation process the actor's state is read from the database.

Actors are deactivated based on timeout and on server resource usage.

Documentation
=======

Documentation is located [here](http://orbit.bioware.com/).

License
=======
Orbit is licensed under the [BSD 3-Clause License](../LICENSE).

Simple Examples
=======
#### Simple Actor
```java
public interface IHello extends IActor
{
    Task<String> sayHello(String greeting);
}
 
public class HelloActor extends OrbitActor implements IHello
{
    public Task<String> sayHello(String greeting)
    {
        getLogger().info("Here: " + greeting);
        return Task.fromValue("Hello There");
    }
}
 
IActor.getReference(IHello.class, "0").sayHello("Meep Meep");
```
