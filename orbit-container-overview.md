---
layout : page
title : "Orbit : Container Overview"
breadCrumb : "[Orbit](index.html) / [Public Documentation](orbit-public-documentation.html) / [Container](orbit-container.html)"
next : "orbit-container-guide.html"
previous: "orbit-container.html"
---
{% include JB/setup %}



-  [Brief](#ContainerOverview-Brief)
-  [FAQs](#ContainerOverview-FAQs)
    -  [What doesn't it do?](#ContainerOverview-Whatdoesn_titdo_)
    -  [Why not use another IoC / DI framework?](#ContainerOverview-WhynotuseanotherIoC_DIframework_)
-  [References](#ContainerOverview-References)



Brief {#ContainerOverview-Brief}
----------


Orbit Container defines a unified way to start/stop/configure Orbit applications. It was developed by [BioWare](http://www.bioware.com/), a division of [Electronic Arts](http://www.ea.com/).


Application components can be loaded and configured through the definition of modules.


Internally it uses a minimal inversion of control ([IoC](http://en.wikipedia.org/wiki/Inversion_of_control)) container to wire application objects together.


It aims to solve:


-  managing the application life cycle
-  injecting configuration
-  wiring objects through dependency injection

 


FAQs {#ContainerOverview-FAQs}
----------


###What doesn't it do? {#ContainerOverview-Whatdoesn_titdo_}


-  It is not web centric or database centric. It's application agnostic.
-  It does not inject constructor parameters, because then it would have to solve dependency cycles.
-  It does not wrap the objects with proxies, nor does any type of AOP. Because clean stack traces are priceless.
-  It does not have scopes or named components. However it's possible to do scoping through composition.  

###Why not use another IoC / DI framework? {#ContainerOverview-WhynotuseanotherIoC_DIframework_}


Because even the simplest ones have a complex feature set that obscures what's happening. For instance proxies, bindings.


Container is straight forward about what is possible and what isn't. It doesn't try to dictate how you should structure your application.


 


References {#ContainerOverview-References}
----------


The Container is arguably based but much simpler and more limited than:


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
