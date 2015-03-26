---
layout : page
title : "Orbit : Sample - Chat"
breadCrumb : "[Orbit](index.html) / [Public Documentation](orbit-public-documentation.html) / [Samples](orbit-samples.html)"
next : "orbit-acknowledgements.html"
previous: "orbit-samples.html"
---
{% include JB/setup %}



-  [Overview](#Sample-Chat-Overview)
-  [Running the Sample](#Sample-Chat-RunningtheSample)
    -  [Command Line](#Sample-Chat-CommandLine)
    -  [Manually](#Sample-Chat-Manually)
        -  [Host (Backend)](#Sample-Chat-Host_Backend_)
        -  [Frontend](#Sample-Chat-Frontend)



Overview {#Sample-Chat-Overview}
----------


The Chat Sample is an example of a web application that provides real-time chat using Orbit. It leverages Orbit Actors, Orbit Container, Jetty, Jersey and WebSockets.


The chat sample will teach you:


-  How to separate Front-End and Host logic.
-  How to integrate Actors and Container.
-  How to leverage Actor observers.
-  How to work with the Actor persistent state system.
-  How to configure a Container.
-  How to use custom configurations in Container.
-  Integrating with an external frontend provider such as Jetty/Jersey

 


Running the Sample {#Sample-Chat-RunningtheSample}
----------


###Command Line {#Sample-Chat-CommandLine}


Running the sample is easy:


{% highlight xml %}
cd samples/chat
mvn install
start-cluster.bat
{% endhighlight %}

 


###Manually {#Sample-Chat-Manually}


If you want to start the sample manually, the entry points are as follows:


 


####Host (Backend) {#Sample-Chat-Host_Backend_}


Project: chat-actors


Entry Class: com.ea.orbit.samples.chat.ServerMain


You can run as many Hosts as you like, Orbit will automatically cluster them for you.


 


####Frontend {#Sample-Chat-Frontend}


Project: chat-frontend


Entry Class: com.ea.orbit.samples.chat.WebMain


You should only run one Frontend per machine as Orbit will bind to port 8080.


 

