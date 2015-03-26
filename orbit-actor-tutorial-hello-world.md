---
layout : page
title : "Orbit : Actor Tutorial - Hello World"
breadCrumb : "[Orbit](index.html) / [Public Documentation](orbit-public-documentation.html) / [Actors](orbit-actors.html) / [Actor Tutorials](orbit-actor-tutorials.html)"
next : "orbit-actor-tutorial-crossing-jvm-boundaries.html"
previous: "orbit-actor-tutorials.html"
---
{% include JB/setup %}



-  [Overview](#ActorTutorial-HelloWorld-Overview)
-  [Maven Project](#ActorTutorial-HelloWorld-MavenProject)
-  [Actor Interface](#ActorTutorial-HelloWorld-ActorInterface)
-  [Actor Implementation](#ActorTutorial-HelloWorld-ActorImplementation)
-  [Using The Actor](#ActorTutorial-HelloWorld-UsingTheActor)
-  [Running](#ActorTutorial-HelloWorld-Running)



Overview {#ActorTutorial-HelloWorld-Overview}
----------


In this guide we'll cover how to get a very simple Orbit Actors application running in the form of "Hello World". It shows using a single-module in a single-process environment, often useful for development purposes.


This tutorial assumes that you have set up a development environment as described in the [prerequisites](orbit-prerequisites.html) document and have some familiarity with Maven based Java projects.


 


Maven Project {#ActorTutorial-HelloWorld-MavenProject}
----------


The first step is to set up a Maven project that is able to pull in the Orbit dependencies.


Replace [ORBIT-VERSION] with the latest version of Orbit (see [Release Notes](orbit-release-notes.html)).

**pom.xml** 
{% highlight xml %}
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<groupId>com.example.orbit</groupId>
	<artifactId>hello</artifactId>
	<version>0.1-SNAPSHOT</version>
	<dependencies>
		<dependency>
			<groupId>com.ea.orbit</groupId>
			<artifactId>orbit-actors-all</artifactId>
			<version>[ORBIT-VERSION]</version>
		</dependency>
	</dependencies>
</project>
{% endhighlight %}

 


Actor Interface {#ActorTutorial-HelloWorld-ActorInterface}
----------


In Orbit all actors must have an interface, below we'll create a very simple actor interface.

**IHello.java** 
{% highlight java %}
package com.example.orbit.hello;

import com.ea.orbit.actors.IActor;
import com.ea.orbit.concurrent.Task;
 
public interface IHello extends IActor
{
    Task<String> sayHello(String greeting);
}
{% endhighlight %}

Important notes:


-  Actor interfaces are standard Java interfaces with special constraints
-  All Actor interfaces must extend IActor
-  All interface methods must return a promise in the form of a Task.
-  The Future type (if any) must be serializable.

 


Actor Implementation {#ActorTutorial-HelloWorld-ActorImplementation}
----------


Once you have an actor interface in place, the final step to complete the actor is to create an actor implementation.

**HelloActor.java** 
{% highlight java %}
package com.example.orbit.hello;

import com.ea.orbit.actors.runtime.OrbitActor;
import com.ea.orbit.concurrent.Task;
 
public class HelloActor extends OrbitActor implements IHello
{
    public Task<String> sayHello(String greeting)
    {
        getLogger().info("Here: " + greeting);
        return Task.fromValue("You said: '" + greeting
                + "', I say: Hello from " + System.identityHashCode(this) + " !");
    }
}


{% endhighlight %}

Important notes:


-  An actor implementation is a standard Java class
-  All actors must extend OrbitActor
-  The actor must implement an actor interface
-  Only one actor implementation per actor interface is permitted
-  There is a built in logger that can be retrieved with getLogger()
-  The actor can return any valid Java future. There is no requirement to use an Orbit specific promise.

 


Using The Actor {#ActorTutorial-HelloWorld-UsingTheActor}
----------


The final step to get a working example is for us to actually use the actor.

**Main.java** 
{% highlight java %}
package com.example.orbit.hello;

import com.ea.orbit.actors.OrbitStage;

public class Main
{
    public static void main(String[] args) throws Exception
    {
        OrbitStage stage1 = initStage(clusterName, "stage1");
        OrbitStage stage2 = initStage(clusterName, "stage2");
 
        IHello helloFrom1 = stage1.getReference(IHello.class, "0");
        IHello helloFrom2 = stage2.getReference(IHello.class, "0");
 
        System.out.println(helloFrom1.sayHello("Hi from 01").get());
        System.out.println(helloFrom2.sayHello("Hi from 02").get());
     
    }
 
    public static OrbitStage initStage(String clusterId, String stageId) throws Exception
    {
        OrbitStage stage = new OrbitStage();
        stage.setClusterName(clusterId);
        stage.start().join();
        return stage;
    }
}
{% endhighlight %}

Important notes:


-  We create two orbit stages (actor execution environments) in the same cluster
-  We get a reference to the same actor on both stages by using stage.getReference and providing the same identifier.
-  The framework will handle the creation of the actor in one of the stages
-  You can communicate with the actor regardless of which stage it actually lives in.

Running {#ActorTutorial-HelloWorld-Running}
----------


You should now be able to run the project using Main as your mainClass.


If everything has gone well, you should see output similar to the following:


{% highlight xml %}
-------------------------------------------------------------------
GMS: address=helloWorldCluster.1424809950235, cluster=ISPN, physical address=10.0.11.51:58155
-------------------------------------------------------------------
-------------------------------------------------------------------
GMS: address=helloWorldCluster.1424809950235, cluster=ISPN, physical address=10.0.11.51:58156
-------------------------------------------------------------------
You said: 'Hi from 01', I say: Hello from 342158216 !
You said: 'Hi from 02', I say: Hello from 342158216 !
{% endhighlight %}

Important Notes:


-  Two nodes are created in the helloWorldCluster
-  A Hi message is sent from both stages
-  The print shows that both Hi messages were processed in the same stage

 


Congratulations, you have created your first Orbit Actors application!


 

