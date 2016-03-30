Orbit Framework
=======
[![Release](https://img.shields.io/github/release/orbit/orbit.svg)](https://github.com/orbit/orbit/releases)
[![Maven Central](https://img.shields.io/maven-central/v/cloud.orbit/orbit-runtime.svg)](https://repo1.maven.org/maven2/com/ea/orbit/)
[![Javadocs](https://img.shields.io/maven-central/v/cloud.orbit/orbit-runtime.svg?label=Javadocs)](http://www.javadoc.io/doc/cloud.orbit/orbit-runtime)
[![Build Status](https://img.shields.io/travis/orbit/orbit.svg)](https://travis-ci.org/orbit/orbit)
[![Gitter](https://img.shields.io/badge/style-Join_Chat-ff69b4.svg?style=flat&label=gitter)](https://gitter.im/orbit/orbit?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

Orbit Actors is a JVM based framework to write distributed systems using virtual actors.
A virtual actor is an object that interacts with the world using asynchronous messages.

At any time an actor may be active or inactive. Usually the state of an inactive actor will reside in the database.
When a message is sent to an inactive actor it will be activated somewhere in the pool of backend servers.
During the activation process the actor's state is read from the database.

Actors are deactivated based on timeout and on server resource usage.

It was developed by [BioWare](http://www.bioware.com), a division of [Electronic Arts](http://www.ea.com) and is heavily inspired by the [Microsoft Orleans](https://github.com/dotnet/Orleans) project. For the latest news, follow us on [Twitter](https://twitter.com/OrbitFramework).

Documentation
=======
Documentation is located [here](http://orbit.bioware.com/). <br />


Simple Example
=======
#### Java
```java
public interface Hello extends Actor
{
    Task<String> sayHello(String greeting);
}

public class HelloActor extends AbstractActor implements Hello
{
    public Task<String> sayHello(String greeting)
    {
        getLogger().info("Here: " + greeting);
        return Task.fromValue("Hello There");
    }
}

Actor.getReference(Hello.class, "0").sayHello("Meep Meep");
```

#### Scala
```scala
trait Hello extends Actor {
  def sayHello(greeting: String): Task[String]
}

class HelloActor extends AbstractActor[AnyRef] with Hello {
  def sayHello(greeting: String): Task[String] = {
    getLogger.info("Here: " + greeting)
    Task.fromValue("Hello There")
  }
}

Actor.getReference(classOf[Hello], "0").sayHello("Meep Meep")
```

License
=======
Orbit is licensed under the [BSD 3-Clause License](LICENSE).
