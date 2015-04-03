Orbit Framework
=======

Orbit is a modern Java framework that makes it easier to build and maintain distributed, secure and scalable online services.

Orbit contains two primary components:
-  Orbit Actors, a framework to write distributed systems using virtual actors.
-  Orbit Container, a minimal inversion of control container for building online services.

It was developed by [BioWare](http://www.bioware.com), a division of [Electronic Arts](http://www.ea.com). Check out the [announcement](http://blog.bioware.com/2015/03/30/launching-into-orbit/) at the BioWare blog.

Documentation
=======

Documentation is located [here](http://orbit.bioware.com/).

License
=======
Orbit is licensed under the [BSD 3-Clause License](LICENSE).

Simple Examples
=======
#### Actors
##### java
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
##### scala
```scala
trait IHello extends IActor {
  def sayHello(greeting: String): Task[String]
}

class HelloActor extends OrbitActor[AnyRef] with IHello {
  def sayHello(greeting: String): Task[String] = {
    getLogger.info("Here: " + greeting)
    Task.fromValue("Hello There")
  }
}

IActor.getReference(classOf[IHello], "0").sayHello("Hi from 01").join()
```
