Orbit Framework
=======

Orbit is a modern framework for JVM languages that makes it easier to build and maintain distributed and scalable online services.

Orbit contains two primary components:
-  Orbit Actors, a framework to write distributed systems using virtual actors.
-  Orbit Container, a minimal inversion of control container for building online services.

It was developed by [BioWare](http://www.bioware.com), a division of [Electronic Arts](http://www.ea.com). For the latest news, follow us on [Twitter](https://twitter.com/OrbitFramework). 
<br /> If you're looking for virtual actors on the .NET CLR, see [Orleans](https://github.com/dotnet/Orleans).

Documentation
=======

Documentation is located [here](http://orbit.bioware.com/). <br />
See the [Hello World](samples/hello) sample.

License
=======
Orbit is licensed under the [BSD 3-Clause License](LICENSE).

Simple Examples
=======
#### Actors - Java
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

#### Actors - Scala
```java
trait IHello extends IActor {
  def sayHello(greeting: String): Task[String]
}

class HelloActor extends OrbitActor[AnyRef] with IHello {
  def sayHello(greeting: String): Task[String] = {
    getLogger.info("Here: " + greeting)
    Task.fromValue("Hello There")
  }
}

IActor.getReference(classOf[IHello], "0").sayHello("Meep Meep")
```
