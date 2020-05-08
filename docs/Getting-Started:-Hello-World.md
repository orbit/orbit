# Overview
In this guide we’ll cover how to get a very simple Orbit application running in the form of “Hello World”. It shows using a single-module in a single-process environment, often useful for development purposes.

This tutorial assumes that you have set up a development environment as described in the [[prerequisites|Getting Started: Prerequisites]] document and have some familiarity with Maven based Java projects.

# Maven Project
The first step is to set up a Maven project that is able to pull in the Orbit dependencies.

Replace [ORBIT-VERSION] with the latest version of Orbit (see [Releases](https://github.com/orbit/orbit/releases)).

**pom.xml**
```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <packaging>pom</packaging>
    <groupId>cloud.orbit.samples</groupId>
    <artifactId>orbit-samples-helloworld</artifactId>
    <version>0.1-SNAPSHOT</version>
	
    <dependencies>
        <dependency>
            <groupId>cloud.orbit</groupId>
            <artifactId>orbit-runtime</artifactId>
            <version>[ORBIT-VERSION]</version>
        </dependency>
    </dependencies>
</project>
```
 

# Actor Interface
In Orbit all actors must have an interface, below we’ll create a very simple actor interface.

**Hello.java**
```java
package cloud.orbit.samples.helloworld;

import cloud.orbit.actors.Actor;
import cloud.orbit.concurrent.Task;

public interface Hello extends Actor
{
    Task<String> sayHello(String greeting);
}

```
* Actor interfaces are standard Java interfaces with special constraints
* All Actor interfaces must extend Actor
* All interface methods must return a promise in the form of a Task.
* The future type (if any) must be serializable.
 

# Actor Implementation
Once you have an actor interface in place, the final step to complete the actor is to create an actor implementation.

**HelloActor.java**
```java
package cloud.orbit.samples.helloworld;

import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.concurrent.Task;

public class HelloActor extends AbstractActor implements Hello
{
    public Task<String> sayHello(String greeting)
    {
        System.out.println("Here: " + greeting);
        return Task.fromValue("You said: '" + greeting
                + "', I say: Hello from " + System.identityHashCode(this) + " !");
    }
}
```
* An actor implementation is a standard Java class
* All actors must extend AbstractActor
* The actor must implement an actor interface
* Only one actor implementation per actor interface is permitted
 
# Using The Actor
The final step to get a working example is for us to actually use the actor.

**Main.java**
```java
package cloud.orbit.samples.helloworld;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;

public class Main
{
    public static void main(String[] args) throws Exception
    {
        // Create and bind to an orbit stage
        Stage stage = new Stage.Builder().clusterName("orbit-helloworld-cluster").build();
        stage.start().join();
        stage.bind();

        // Send a message to the actor
        final String response = Actor.getReference(Hello.class, "0").sayHello("Welcome to orbit").join();
        System.out.println(response);

        // Shut down the stage
        stage.stop().join();
    }
}
```
* We create an orbit execution environment known as a stage
* We get a reference to the actor with id "0".
* The framework will handle the activation of the actor.
* You can communicate with the actor without knowing it's status.

# Running
You should now be able to run the project using Main as your mainClass.

If everything has gone well, you should see output similar to the following:

```
-------------------------------------------------------------------
GMS: address=orbit-helloworld-cluster, cluster=orbit-helloworld-cluster, physical address=192.168.1.74:60386
-------------------------------------------------------------------
Here: Welcome to orbit
You said: 'Welcome to orbit', I say: Hello from 1184389119 !
```

Congratulations, you have created your first Orbit application!