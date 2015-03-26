---
layout : page
title : "Orbit : Actor Tutorial - Crossing JVM Boundaries"
breadCrumb : "[Orbit](index.html) / [Public Documentation](orbit-public-documentation.html) / [Actors](orbit-actors.html) / [Actor Tutorials](orbit-actor-tutorials.html)"
next : "orbit-actor-tutorial-structuring-your-project.html"
previous: "orbit-actor-tutorial-hello-world.html"
---
{% include JB/setup %}



-  [Overview](#ActorTutorial-CrossingJVMBoundaries-Overview)
-  [Host](#ActorTutorial-CrossingJVMBoundaries-Host)
-  [Front End](#ActorTutorial-CrossingJVMBoundaries-FrontEnd)
-  [Expected Output](#ActorTutorial-CrossingJVMBoundaries-ExpectedOutput)



Overview {#ActorTutorial-CrossingJVMBoundaries-Overview}
----------


In this guide we'll cover how to adapt the [Hello World](orbit-actor-tutorial-hello-world.html) tutorial to run across multiple JVMs. This is a more standard configuration for production applications.  


Host {#ActorTutorial-CrossingJVMBoundaries-Host}
----------


First we will create a Main class for the Host Stage, this is the stage that will actually host actor activations.

**MainHost.java** 
{% highlight java %}
package com.ea.orbit.actors.samples.hello;

import com.ea.orbit.actors.OrbitStage;

public class MainHost
{
    public static void main(String[] args) throws Exception
    {
    	OrbitStage stage = new OrbitStage();
        stage.setClusterName("helloWorldCluster");
        stage.setMode(OrbitStage.StageMode.HOST);
        stage.start().join();

        IHello helloInt = stage.getReference(IHello.class, "0");

        System.out.println(helloInt.sayHello("Hi from host").get());
			
        System.out.println("press any key to exit");
        System.in.read();        
    }
}
{% endhighlight %}

 


Front End {#ActorTutorial-CrossingJVMBoundaries-FrontEnd}
----------


Secondly we will create a Main class for the Front End Stage, this is the stage that will communicate with the Host stage but will not host actor activations itself.

**MainHost.java** 
{% highlight java %}
package com.ea.orbit.actors.samples.hello;

import com.ea.orbit.actors.OrbitStage;

public class MainFrontend
{
    public static void main(String[] args) throws Exception
    {
        OrbitStage stage = new OrbitStage();
        stage.setClusterName("helloWorldCluster");
        stage.setMode(OrbitStage.StageMode.FRONT_END);
        stage.start().join();

        IHello helloInt = stage.getReference(IHello.class, "0");

        System.out.println(helloInt.sayHello("Hi from frontend").get());
    }
}
{% endhighlight %}

 


Expected Output {#ActorTutorial-CrossingJVMBoundaries-ExpectedOutput}
----------


You should first run the Host by starting with MainHost as your mainClass.

**Host Stage** 
{% highlight xml %}
You said: 'Hi from host', I say: Hello from 370750083 !
{% endhighlight %}

The Host stage will continue to run.


 


You can now run the Front End stage by running with MainFrontend as your mainClass.

**Frontend Stage** 
{% highlight xml %}
You said: 'Hi from frontend', I say: Hello from 370750083 !
{% endhighlight %}

Note that the "from" id is always the same.


The Front End stage will shutdown automatically. You should be able to run the Front End multiple times and always get the same output.

