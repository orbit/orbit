---
layout : page
title : "Orbit : Actor Tutorial - Using State"
breadCrumb : "[Orbit](index.html) / [Public Documentation](orbit-public-documentation.html) / [Actors](orbit-actors.html) / [Actor Tutorials](orbit-actor-tutorials.html)"
next : "orbit-actor-tutorial-observers.html"
previous: "orbit-actor-tutorial-structuring-your-project.html"
---
{% include JB/setup %}



-  [Overview](#ActorTutorial-UsingState-Overview)
-  [Choosing A Storage Provider](#ActorTutorial-UsingState-ChoosingAStorageProvider)
-  [Actor Interface](#ActorTutorial-UsingState-ActorInterface)
-  [Actor Implementation](#ActorTutorial-UsingState-ActorImplementation)
-  [Using The Actor](#ActorTutorial-UsingState-UsingTheActor)
-  [Running](#ActorTutorial-UsingState-Running)



Overview {#ActorTutorial-UsingState-Overview}
----------


In this guide, we'll show how to create stateful actors which persist state to storage. We'll be adapting the original [Hello World](orbit-actor-tutorial-hello-world.html) example.


We'll store the last message that was sent, and allow that to be retrieved.




Choosing A Storage Provider {#ActorTutorial-UsingState-ChoosingAStorageProvider}
----------


Orbit supports different storage providers and you are able to create one manually.


In this example we are going to use the primary storage provider provided by the Orbit team, MongoDB.


In order to use the MongoDB provider, you'll need to add the following to your Maven dependencies:


{% highlight xml %}
<dependency>
    <groupId>com.ea.orbit</groupId>
    <artifactId>orbit-actors-mongodb</artifactId>
    <version[ORBIT-VERSION]</version>
</dependency>
{% endhighlight %}



Actor Interface {#ActorTutorial-UsingState-ActorInterface}
----------


First we'll make a small change to the actor example to support getting the last message.

**IHello.java** 
{% highlight java %}
package com.example.orbit.hello;

import com.ea.orbit.actors.IActor;
import com.ea.orbit.concurrent.Task;
 
public interface IHello extends IActor
{
    Task<String> sayHello(String greeting);
    Task<String> getLastHello();
}
{% endhighlight %}

No other changes are required to the interface to support state. An actor interface does not know whether an actor is stateful or not.




Actor Implementation {#ActorTutorial-UsingState-ActorImplementation}
----------


Next we'll adapt the actor implementation to support state

**HelloActor.java** 
{% highlight java %}
package com.example.orbit.hello;

import com.ea.orbit.actors.runtime.OrbitActor;
import com.ea.orbit.concurrent.Task;
 
public class HelloActor extends OrbitActor<HelloActor.State> implements IHello
{
    public static class State
    {
        String lastMessage;
    }
 
    public Task<String> sayHello(String greeting)
    {
        getLogger().info("Here: " + greeting);
        String message = "You said: '" + greeting + "', I say: Hello from " + System.identityHashCode(this) + " !";
        state().lastMessage = message;
        return writeState().thenApply(x -> message);
    }
 
    public Task<String> getLastHello()
    {
        return Task.fromValue(state().lastMessage);
    }
}


{% endhighlight %}

Important notes:


-  Notice that in this example we're passing HelloActor.State as a generic into OrbitActor. This is what makes an actor stateful.
-  State can be accessed using the state() method
-  State will automatically be retrieved on actor activation, so there is no need to read the state manually
-  The return value is then chained so the Task will only be complete once the writeState has taken place.

 


Using The Actor {#ActorTutorial-UsingState-UsingTheActor}
----------


The final step to get a working example is for us to actually use the actor.

**Main.java** 
{% highlight java %}
package com.example.orbit.hello;

import com.ea.orbit.actors.OrbitStage;
import com.ea.orbit.actors.providers.mongodb.MongoDBStorageProvider;

public class Main
{
    public static void main(String[] args) throws Exception
    {
        final String clusterName = "helloWorldCluster." + System.currentTimeMillis();
               
        OrbitStage stage1 = initStage(clusterName, "stage1");
        OrbitStage stage2 = initStage(clusterName, "stage2");

        final MongoDBStorageProvider storageProvider = new MongoDBStorageProvider();
        storageProvider.setDatabase("database_name");

        stage1.addProvider(storageProvider);
        stage2.addProvider(storageProvider);

        IHello helloFrom1 = stage1.getReference(IHello.class, "0");
        IHello helloFrom2 = stage2.getReference(IHello.class, "0");

        System.out.println(helloFrom1.sayHello("Hi from 01").get());
        System.out.println("Last From 2: " + helloFrom2.getLastHello().get());
        System.out.println(helloFrom2.sayHello("Hi from 02").get());
        System.out.println("Last From 1: " + helloFrom1.getLastHello().get());
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

Important Notes:


-  The default storage provider is created as MongoDB, you can initialize any storage provider you like
-  Calls to getLastHello have been introduced

 


Running {#ActorTutorial-UsingState-Running}
----------


If all has gone well, you should get output similar to the following:


{% highlight xml %}
-------------------------------------------------------------------
GMS: address=helloWorldCluster.1425669031908, cluster=ISPN, physical address=10.0.11.51:62166
-------------------------------------------------------------------
-------------------------------------------------------------------
GMS: address=helloWorldCluster.1425669031908, cluster=ISPN, physical address=10.0.11.51:59722
-------------------------------------------------------------------
You said: 'Hi from 01', I say: Hello from 179642099 !
Last From 2: You said: 'Hi from 01', I say: Hello from 179642099 !
You said: 'Hi from 02', I say: Hello from 179642099 !
Last From 1: You said: 'Hi from 02', I say: Hello from 179642099 !
{% endhighlight %}


