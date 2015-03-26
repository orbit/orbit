---
layout : page
title : "Orbit : Actor Tutorial - Cross Actor Communication"
breadCrumb : "[Orbit](index.html) / [Public Documentation](orbit-public-documentation.html) / [Actors](orbit-actors.html) / [Actor Tutorials](orbit-actor-tutorials.html)"
next : "orbit-container.html"
previous: "orbit-actor-tutorial-observers.html"
---
{% include JB/setup %}



-  [Overview](#ActorTutorial-CrossActorCommunication-Overview)
-  [Greeter Interface](#ActorTutorial-CrossActorCommunication-GreeterInterface)
-  [Greeter Implementation](#ActorTutorial-CrossActorCommunication-GreeterImplementation)
-  [Hello World Actor](#ActorTutorial-CrossActorCommunication-HelloWorldActor)
-  [Running](#ActorTutorial-CrossActorCommunication-Running)



Overview {#ActorTutorial-CrossActorCommunication-Overview}
----------


So far, whenever we've interacted with actors it has always been from an external piece of code via the Orbit stage.


In this example we'll look at how actors can communicate between themselves without involving the Orbit stage.


As with other samples, we'll be working from the base Hello World Example.




Greeter Interface {#ActorTutorial-CrossActorCommunication-GreeterInterface}
----------


The first thing we will do is introduce a new actor interface, the greeter.

**IGreeter.java** 
{% highlight java %}
package com.example.orbit.hello;
 

import com.ea.orbit.actors.IActor;
import com.ea.orbit.concurrent.Task;

public interface IGreeter extends IActor
{
    Task<String> getGreeting();
}

{% endhighlight %}

 


Greeter Implementation {#ActorTutorial-CrossActorCommunication-GreeterImplementation}
----------


Next we introduce an actor implementation for our newly created greeter.

**GreeterActor.java** 
{% highlight java %}
package com.example.orbit.hello;
 
import com.ea.orbit.actors.runtime.OrbitActor;
import com.ea.orbit.concurrent.Task;

import java.util.Random;

public class GreeterActor extends OrbitActor implements IGreeter
{
    public Task<String> getGreeting()
    {
        final String[] greetings = {"Hello", "Bonjour", "Hallo", "Hola", "Aloha"};
        return Task.fromValue(greetings[new Random().nextInt(greetings.length)]);
    }
}

{% endhighlight %}

Important Notes:


-  We simply use Task.fromValue to return a random entry from the greetings array.

 


Hello World Actor {#ActorTutorial-CrossActorCommunication-HelloWorldActor}
----------


Finally we'll make a simple change to the hello world actor to use the newly introduced greeting actor.

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

        IGreeter greeter = GreeterFactory.getReference("0");

        return greeter.getGreeting().thenApply(greetResponse -> "You said: '" + greeting
                + "', I say: " + greetResponse + " from " + System.identityHashCode(this) + " !");
    }
}

{% endhighlight %}

Important Notes:


-  We use a GreeterFactory to get a reference to our greeter actor. Factories are automatically generated from the actor interface
-  We request the greeting and then return a task which will be complete upon getting a response from the greeting actor.

 


Running {#ActorTutorial-CrossActorCommunication-Running}
----------


If everything has gone well, you should see output similar to the following:


{% highlight xml %}
-------------------------------------------------------------------
GMS: address=helloWorldCluster.1424809950235, cluster=ISPN, physical address=10.0.11.51:58155
-------------------------------------------------------------------
-------------------------------------------------------------------
GMS: address=helloWorldCluster.1424809950235, cluster=ISPN, physical address=10.0.11.51:58156
-------------------------------------------------------------------
You said: 'Hi from 01', I say: Bonjour from 353014605 !
You said: 'Hi from 02', I say: Aloha from 353014605 !
{% endhighlight %}
