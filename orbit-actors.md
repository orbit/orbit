---
layout : page
title : "Orbit : Actors"
breadCrumb : "[Orbit](index.html) / [Public Documentation](orbit-public-documentation.html)"
next : "orbit-actor-overview.html"
previous: "orbit-project-structure.html"
---
{% include JB/setup %}

[Actor Overview](orbit-actor-overview.html) {#Actors-ActorOverview}
----------


[Actor Concepts](orbit-actor-concepts.html) {#Actors-ActorConcepts}
----------


[Actor Tutorials](orbit-actor-tutorials.html) {#Actors-ActorTutorials}
----------


 

**Simple Actor Example** 
{% highlight java %}
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

HelloFactory.getReference("0").sayHello("Meep Meep");
{% endhighlight %}
