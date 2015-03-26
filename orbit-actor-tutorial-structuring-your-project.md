---
layout : page
title : "Orbit : Actor Tutorial - Structuring Your Project"
breadCrumb : "[Orbit](index.html) / [Public Documentation](orbit-public-documentation.html) / [Actors](orbit-actors.html) / [Actor Tutorials](orbit-actor-tutorials.html)"
next : "orbit-actor-tutorial-using-state.html"
previous: "orbit-actor-tutorial-crossing-jvm-boundaries.html"
---
{% include JB/setup %}



-  [Overview](#ActorTutorial-StructuringYourProject-Overview)
-  [Structure](#ActorTutorial-StructuringYourProject-Structure)
    -  [Front End](#ActorTutorial-StructuringYourProject-FrontEnd)
    -  [Host](#ActorTutorial-StructuringYourProject-Host)
    -  [Actor Interfaces](#ActorTutorial-StructuringYourProject-ActorInterfaces)
    -  [Actor Implementation](#ActorTutorial-StructuringYourProject-ActorImplementation)
-  [Summary](#ActorTutorial-StructuringYourProject-Summary)



Overview {#ActorTutorial-StructuringYourProject-Overview}
----------


In this guide, we'll discuss some strategies for structuring large projects across multiple modules to make your project cleaner.


Structure {#ActorTutorial-StructuringYourProject-Structure}
----------


By default, Orbit offers two roles for structuring stages, these roles can help you make smart decisions about how to structure a project, [see more.](orbit-actor-concept-stages.html)


A typical project can easily be split into 4 distinct modules with a simple structure:


-  Front End
-  Host
-  Actor Implementation
-  Actor Interfaces

 


###Front End {#ActorTutorial-StructuringYourProject-FrontEnd}


In Orbit, a front end stage is designed to participate in the Orbit cluster, but it does not host actors. It only requires the actor interfaces to function correctly.


Typically the front end will offer your services to a public client via an endpoint such as a HTTP Server.


 


Dependencies:


-  Actor Interfaces
-  Orbit Client

 


###Host {#ActorTutorial-StructuringYourProject-Host}


In Orbit, a host participates fully in the cluster. Hosts are where actor activations actually live. It requires the full actor implementation.


Typically you can use an Orbit Container as a stage host, so it doesn't need to be a separate project with logic of it's own.


 


Dependencies:


-  Actor Implementation
-  Actor Interfaces
-  Orbit Full

 


###Actor Interfaces {#ActorTutorial-StructuringYourProject-ActorInterfaces}


Your actor interfaces project is a simple module that contains all of the interfaces which extend IActor 


Dependencies:


-  Orbit Client

 


###Actor Implementation {#ActorTutorial-StructuringYourProject-ActorImplementation}


Your actor implementation project is a simple module which contains all of the Orbit actor implementations which extend OrbitActor


Dependencies:


-  Actor Interfaces
-  Orbit Full

Summary {#ActorTutorial-StructuringYourProject-Summary}
----------


Orbit does not strictly define how projects should be structured, but it does provide the concept of roles to help you structure your project how you like.


This tutorial should have given you a good start on splitting up your application into multiple modules.


The Orbit [Chat Sample](orbit-sample-chat.html) provides a good example of how to structure a project in this way.

