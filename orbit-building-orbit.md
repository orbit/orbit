---
layout : page
title : "Orbit : Building Orbit"
breadCrumb : "[Orbit](index.html) / [Public Documentation](orbit-public-documentation.html) / [Getting Started](orbit-getting-started.html)"
next : "orbit-project-structure.html"
previous: "orbit-prerequisites.html"
---
{% include JB/setup %}



-  [Notices](#BuildingOrbit-Notices)
-  [Scope](#BuildingOrbit-Scope)
-  [Getting The Code](#BuildingOrbit-GettingTheCode)
-  [Building The Code](#BuildingOrbit-BuildingTheCode)
-  [Common Failures](#BuildingOrbit-CommonFailures)
    -  [Maven/Java Misconfiguration](#BuildingOrbit-Maven_JavaMisconfiguration)
    -  [MongoDB Failures](#BuildingOrbit-MongoDBFailures)



Notices {#BuildingOrbit-Notices}
----------

**Warning**

Please ensure you have the required [prerequisites](orbit-prerequisites.html) and understand the [project structure ](orbit-project-structure.html)before following this guide.

**Info**

You are not required to build Orbit manually. This step is only required if you want to build the framework and samples manually. 


 


Scope {#BuildingOrbit-Scope}
----------


This guide assumes that you want to sync and build all projects in orbit-generic and their samples, the process is the same for other repositories.


Getting The Code {#BuildingOrbit-GettingTheCode}
----------


You are able to retrieve the Orbit Code from the Orbit GitHub project


To clone the Orbit repository you can use the follow Git command:


{% highlight xml %}
git clone https://github.com/electronicarts/orbit.git
{% endhighlight %}

Building The Code {#BuildingOrbit-BuildingTheCode}
----------


Building the code is very straightforward.


Navigate to the synced directory and build using maven as following:


{% highlight xml %}
mvn clean install
{% endhighlight %}

If everything worked correctly, you should get a "BUILD SUCCESS" message. 


If the build failed, you can check the common failures below.


 


Common Failures {#BuildingOrbit-CommonFailures}
----------


###Maven/Java Misconfiguration {#BuildingOrbit-Maven_JavaMisconfiguration}


To confirm that Java and Maven are configured correctly, you can run "mvn --v" and confirm the output is similar to below:


{% highlight xml %}
D:\Dev\orbit\orbit-generic> mvn --v

Apache Maven 3.2.5 (12a6b3acb947671f09b81f49094c53f426d8cea1; 2014-12-14T10:29:2
3-07:00)
Maven home: D:\Programs\Maven
Java version: 1.8.0_31, vendor: Oracle Corporation
Java home: D:\Programs\Java\jdk8\jre
Default locale: en_US, platform encoding: Cp1252
OS name: "windows 7", version: "6.1", arch: "amd64", family: "dos"
{% endhighlight %}

 


###MongoDB Failures {#BuildingOrbit-MongoDBFailures}


Currently MongoDB is required to build Orbit, it is often not installed or is configured incorrectly, error messages similar to the one below should make it obvious that the problem lies with MongoDB Configuration.


{% highlight xml %}
Tests in error:
  checkReads(com.ea.orbit.actors.mongodb.test.MongodbPersistenceTes
 after 10000 ms while waiting to connect. Client view of cluster st
Unknown, servers=[{address=127.0.0.1:27017, type=Unknown, state=Con
ption={com.mongodb.MongoException$Network: Exception opening the so
 by {java.net.ConnectException: Connection refused: connect}}]
{% endhighlight %}
