<img src="http://www.orbit.cloud/img/orbit-logo-black.png" alt="Orbit Logo" width="200px"/>

[![Release](https://img.shields.io/github/release/orbit/orbit.svg)](https://github.com/orbit/orbit/releases)
[![Maven Central](https://img.shields.io/maven-central/v/cloud.orbit/orbit-runtime.svg)](https://repo1.maven.org/maven2/cloud/orbit/)
[![Javadocs](http://www.javadoc.io/badge/cloud.orbit/orbit-runtime.svg)](https://github.com/orbit/orbit/wiki/Javadocs)
[![Build Status](https://img.shields.io/travis/orbit/orbit.svg)](https://travis-ci.org/orbit/orbit)
[![Gitter](https://img.shields.io/badge/style-Join_Chat-ff69b4.svg?style=flat&label=gitter)](https://gitter.im/orbit/orbit)
[![Twitter Follow](https://img.shields.io/twitter/follow/OrbitFramework.svg?style=flat&maxAge=86400)](https://twitter.com/orbitframework)

Orbit is a framework to write distributed systems using virtual actors on the JVM. It allows developers to write highly distributed and scalable applications while greatly simplifying clustering, discovery, networking, state management, actor lifetime and more.

<a href="https://github.com/orbit/orbit/wiki/Duke's-Choice-Award-2016"><img src="http://www.orbit.cloud/img/dca/dca_logo.png" alt="Duke's Choice Award Logo" width="200px" /></a><br />
Orbit received the 2016 Duke's Choice Award for Open Source, read [here](https://github.com/orbit/orbit/wiki/Duke's-Choice-Award-2016) for more information. 

Full Documentation
=======
See the [Wiki](https://github.com/orbit/orbit/wiki) for full documentation, examples and other information.

Developer & License
======
This project was developed by [Electronic Arts](http://www.ea.com) and is licensed under the [BSD 3-Clause License](LICENSE).

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
trait Hello extends Actor 
{
  def sayHello(greeting: String): Task[String]
}

class HelloActor extends AbstractActor[AnyRef] with Hello 
{
  def sayHello(greeting: String): Task[String] = 
  {
    getLogger.info("Here: " + greeting)
    Task.fromValue("Hello There")
  }
}

Actor.getReference(classOf[Hello], "0").sayHello("Meep Meep")
```
