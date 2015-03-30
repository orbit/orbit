---
layout : page
title : "Orbit : Container Guide"
breadCrumb : "[Orbit](index.html) / [Public Documentation](orbit-public-documentation.html) / [Container](orbit-container.html)"
next : "orbit-samples.html"
previous: "orbit-container-overview.html"
---
{% include JB/setup %}



-  [Modules](#ContainerGuide-Modules)
    -  [About Modules](#ContainerGuide-AboutModules)
    -  [Creating a Module](#ContainerGuide-CreatingaModule)
-  [Singletons](#ContainerGuide-Singletons)
    -  [About Singletons](#ContainerGuide-AboutSingletons)
    -  [Creating a Singleton](#ContainerGuide-CreatingaSingleton)
-  [Container Lifecycle](#ContainerGuide-ContainerLifecycle)
    -  [Creating a Container](#ContainerGuide-CreatingaContainer)
    -  [Optional Configuration](#ContainerGuide-OptionalConfiguration)
    -  [Starting a Container](#ContainerGuide-StartingaContainer)
    -  [Stopping a Container](#ContainerGuide-StoppingaContainer)
-  [Injection](#ContainerGuide-Injection)
    -  [Basic Injection](#ContainerGuide-BasicInjection)
    -  [Configuration Injection](#ContainerGuide-ConfigurationInjection)
    -  [Wiring an External Instance](#ContainerGuide-WiringanExternalInstance)
-  [Application Structure](#ContainerGuide-ApplicationStructure)



Modules {#ContainerGuide-Modules}
----------


###About Modules {#ContainerGuide-AboutModules}


The basic building block of the Orbit Container system are known as modules.


Enabling a new module is equal to adding a dependency and some configuration.


Disabling a module should require only editing the configuration.


###Creating a Module {#ContainerGuide-CreatingaModule}


Creating a module is very simple


{% highlight java %}
public class MyModule extends Module
{
}
{% endhighlight %}

 


Singletons {#ContainerGuide-Singletons}
----------


###About Singletons {#ContainerGuide-AboutSingletons}


Singletons are the basic way to add functionality to a Container.


Their lifetime will be managed by the framework.


###Creating a Singleton {#ContainerGuide-CreatingaSingleton}


{% highlight java %}
@Singleton
public class MySingleton implements Startable
{
    public Task start()
    {
        return Task.done();
    }
 
    public Task stop()
    {
        return Task.done();
    }
}
{% endhighlight %}

 


Container Lifecycle {#ContainerGuide-ContainerLifecycle}
----------


###Creating a Container {#ContainerGuide-CreatingaContainer}


You can start a container manually


{% highlight java %}
OrbitContainer container = new OrbitContainer();
{% endhighlight %}

It is also possible to create and start a container using the default container bootstrap


{% highlight xml %}
com.ea.orbit.container.Bootstrap
{% endhighlight %}

-  automatically locate modules from the classpath META-INF/orbit/modules
-  locate the ConfigurationProvider and enable/disable modules  

###Optional Configuration {#ContainerGuide-OptionalConfiguration}


It is possible to manually include or substitute application components and modules


{% highlight java %}
container.addComponent(HelloWorld.class);
container.addModule(JettyRestModule.class);
container.addComponent(IClusterPeer.class, JGroupsPeer.class);
{% endhighlight %}

-  useful for testing  

###Starting a Container {#ContainerGuide-StartingaContainer}


{% highlight java %}
container.start();
{% endhighlight %}

-  instantiates all singletons
-  injects the @Config fields
-  process the @Inject annotations
-  calls the methods annotated with @PostConstruct
-  calls start() on all Startable singletons.  

###Stopping a Container {#ContainerGuide-StoppingaContainer}


{% highlight java %}
container.stop();
{% endhighlight %}

-  calls stop() on all Startable singletons.
-  calls the methods annotated with @PreDestroy

 


Injection {#ContainerGuide-Injection}
----------


Container includes a basic dependency injection framework.


###Basic Injection {#ContainerGuide-BasicInjection}


Basic injection of components is achieved using the standard Inject annotation.


{% highlight java %}
@Inject
OrbitContainer container;
{% endhighlight %}

###Configuration Injection {#ContainerGuide-ConfigurationInjection}


Container also supports injection of configurations


{% highlight java %}
@Config("orbit.configs.example")
String example = "defaultValue";
{% endhighlight %}

These can be overridden in the Orbit config file


{% highlight xml %}
orbit.configs.example: SomeString
{% endhighlight %}

###Wiring an External Instance {#ContainerGuide-WiringanExternalInstance}


If you have an object that's lifetime is managed outside of container, you can still have the field and config injection performed on that class.


{% highlight java %}
@Inject
OrbitContainer container;
 
void exampleInject()
{
    SomeThing thing = new SomeThing();
    container.inject(thing);
}
{% endhighlight %}

 


Application Structure {#ContainerGuide-ApplicationStructure}
----------


The ideal application structure is:


-  a single jar (composed of several jars bundled together) or a collection of jars,
-  a collection of module classes defining which application classes will be instantiated
-  a configuration file.
