---
layout : page
title : "Orbit : Advanced Topic - Lifetime Providers"
breadCrumb : "[Orbit](index.html) / [Public Documentation](orbit-public-documentation.html) / [Actors](orbit-actors.html) / [Actor Concepts](orbit-actor-concepts.html) / [Actor Concept - Advanced Topics](orbit-actor-concept-advanced-topics.html)"
next : "orbit-actor-tutorials.html"
previous: "orbit-advanced-topic-cluster-configuration.html"
---
{% include JB/setup %}



-  [Overview](#AdvancedTopic-LifetimeProviders-Overview)
-  [Working With Lifetime Providers](#AdvancedTopic-LifetimeProviders-WorkingWithLifetimeProviders)
    -  [Lifetime Provider Interface](#AdvancedTopic-LifetimeProviders-LifetimeProviderInterface)
    -  [Registering The Provider](#AdvancedTopic-LifetimeProviders-RegisteringTheProvider)



Overview {#AdvancedTopic-LifetimeProviders-Overview}
----------


When integrating Orbit with other frameworks it is often useful to get notifications about lifetime events happening within the framework.


To meet this need, Orbit allows developers to implement a Lifetime Provider. Lifetime providers allow a developer to implement an interface and get notified about framework events.


This is particularly useful for Dependency Injection frameworks as developers get an opportunity to wire an actor into the framework before it is used by Orbit.


 


Working With Lifetime Providers {#AdvancedTopic-LifetimeProviders-WorkingWithLifetimeProviders}
----------


###Lifetime Provider Interface {#AdvancedTopic-LifetimeProviders-LifetimeProviderInterface}


The lifetime provider interface is very simple.

**Lifetime Provider** 
{% highlight java %}
Task preActivation(OrbitActor actor);
Task postActivation(OrbitActor actor);
Task preDeactivation(OrbitActor actor);
Task postDeactivation(OrbitActor actor);
{% endhighlight %}

 


###Registering The Provider {#AdvancedTopic-LifetimeProviders-RegisteringTheProvider}


The quickest and easiest way to register a provider is to add it to the Orbit Stage before startup.

**Register Provider** 
{% highlight java %}
OrbitStage stage = new OrbitStage();
stage.setClusterName(clusterId);
 
stage.addProvider(new ILifetimeProvider() {
    @Override
    public Task preActivation(OrbitActor orbitActor) {
        return Task.done();
    }
    @Override
    public Task postActivation(OrbitActor orbitActor) {
        return Task.done();
    }
    @Override
    public Task preDeactivation(OrbitActor orbitActor) {
        return Task.done();
    }
    @Override
    public Task postDeactivation(OrbitActor orbitActor) {
        return Task.done();
    }
});
 
stage.start().join();
{% endhighlight %}
