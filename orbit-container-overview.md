---
layout : page
title : "Orbit : Container Overview"
breadCrumb : "[Orbit](index.html) / [Public Documentation](orbit-public-documentation.html) / [Container](orbit-container.html)"
next : "orbit-samples.html"
previous: "orbit-container.html"
---
{% include JB/setup %}



-  [Brief](#ContainerOverview-Brief)
-  [The ideal application](#ContainerOverview-Theidealapplication)
-  [Orbit container life cycle](#ContainerOverview-Orbitcontainerlifecycle)
    -  [Container created](#ContainerOverview-Containercreated)
    -  [Optional manual inclusion or substitution of application components and modules:](#ContainerOverview-Optionalmanualinclusionorsubstitutionofapplicationcomponentsandmodules_)
    -  [Container start() called.](#ContainerOverview-Containerstart__called_)
    -  [Container stop() called](#ContainerOverview-Containerstop__called)
-  [FAQ](#ContainerOverview-FAQ)
    -  [What doesn't it do?](#ContainerOverview-Whatdoesn_titdo_)
    -  [Why not use another IoC / DI framework?](#ContainerOverview-WhynotuseanotherIoC_DIframework_)
-  [References](#ContainerOverview-References)



Brief {#ContainerOverview-Brief}
----------


Orbit container defines a unified way to start/stop/configure orbit applications. It was developed by [BioWare](http://www.bioware.com/), a division of [Electronic Arts](http://www.ea.com/).


Application components can be loaded and configured through the definition of modules.


Internally it uses a minimal inversion of control ([IoC](http://en.wikipedia.org/wiki/Inversion_of_control)) container to wire application objects together.


It aims to solve:


-  managing the application life cycle
-  injecting configuration
-  wiring objects through dependency injection

 


The ideal application {#ContainerOverview-Theidealapplication}
----------


The ideal orbit application is:


-  a single jar (composed of several jars bundled together) or a collection of jars,
-  a collection of module classes defining which application classes will be instantiated
-  a configuration file.

Enabling a new module is equal to adding a dependency and some configuration.


Disabling a module should require only editing the configuration.


 


Orbit container life cycle {#ContainerOverview-Orbitcontainerlifecycle}
----------


###Container created {#ContainerOverview-Containercreated}


{% highlight xml %}
OrbitContainer container = new OrbitContainer();
{% endhighlight %}

-  automatically locate modules from the classpath META-INF/orbit/modules
-  locate the ConfigurationProvider and enable/disable modules  

###Optional manual inclusion or substitution of application components and modules: {#ContainerOverview-Optionalmanualinclusionorsubstitutionofapplicationcomponentsandmodules_}


{% highlight xml %}
container.addComponent(HelloWorld.class);
container.addModule(JettyRestModule.class);
container.addComponent(IClusterPeer.class, JGroupsPeer.class);
{% endhighlight %}

-  useful for testing  

###Container start() called. {#ContainerOverview-Containerstart__called_}


{% highlight xml %}
container.start();
{% endhighlight %}

-  instantiates all singletons
-  injects the @Config fields
-  process the @Inject annotations
-  calls the methods annotated with @PostConstruct
-  calls start() on all Startable singletons.  

###Container stop() called {#ContainerOverview-Containerstop__called}


{% highlight xml %}
container.stop();
{% endhighlight %}

-  calls stop() on all Startable singletons.
-  calls the methods annotated with @PreDestroy

 


FAQ {#ContainerOverview-FAQ}
----------


###What doesn't it do? {#ContainerOverview-Whatdoesn_titdo_}


-  It is not web centric or database centric. It's application agnostic.
-  It does not inject constructor parameters, because then it would have to solve dependency cycles.
-  It does not wrap the objects with proxies, nor does any type of AOP. Because clean stack traces are priceless.
-  It does not have scopes or named components. However it's possible to do scoping through composition.  

###Why not use another IoC / DI framework? {#ContainerOverview-WhynotuseanotherIoC_DIframework_}


Because even the simplest ones have a complex feature set that obscures what's happening. For instance proxies, bindings.


Orbit container is straight forward about what is possible and what isn't. It doesn't try to dictate how you should structure your application.


 


References {#ContainerOverview-References}
----------


The Orbit container is arguably based but much simpler and more limited than:


-  [Pico Container](http://picocontainer.codehaus.org/)
-  [HK2](https://hk2.java.net/) from GlassFish
-  [Guice](https://github.com/google/guice) from google.
-  [Spring](https://spring.io/) framework.
-  [Avalon](https://avalon.apache.org/closed.html) (defunct)
-  [SilkDI](http://www.silkdi.com/)
-  [NInject](http://www.ninject.org/) (C#)

References about IoC and DI:


-  [Comparison Pico, Guice, and Spring](http://stackoverflow.com/questions/2026016/google-guice-vs-picocontainer-for-dependency-injection)
-  [Guice intro video (50min)](https://www.youtube.com/watch?v=hBVJbzAagfs), old presentation. Explains why DI is interesting.
-  [Inversion of control](http://martinfowler.com/bliki/InversionOfControl.html) Marting Fowler's article from 2005.
