---
layout : page
title : "Orbit : Advanced Topic - Cluster Configuration"
breadCrumb : "[Orbit](index.html) / [Public Documentation](orbit-public-documentation.html) / [Actors](orbit-actors.html) / [Actor Concepts](orbit-actor-concepts.html) / [Actor Concept - Advanced Topics](orbit-actor-concept-advanced-topics.html)"
next : "orbit-advanced-topic-lifetime-providers.html"
previous: "orbit-actor-concept-advanced-topics.html"
---
{% include JB/setup %}



-  [Overview](#AdvancedTopic-ClusterConfiguration-Overview)
-  [TCP Discovery](#AdvancedTopic-ClusterConfiguration-TCPDiscovery)
-  [Advanced Usage](#AdvancedTopic-ClusterConfiguration-AdvancedUsage)



Overview {#AdvancedTopic-ClusterConfiguration-Overview}
----------


Orbit leverages [JGroups ](http://www.jgroups.org)and [Infinispan ](http://www.infinispan.org)to cluster servers together.


The default Orbit configuration uses UDP Multicast based on the cluster name.


 


TCP Discovery {#AdvancedTopic-ClusterConfiguration-TCPDiscovery}
----------


Cloud providers (such as AWS) often do not provide UDP multicast support. In these scenarios [TCPPing ](http://www.jgroups.org/manual/html/user-advanced.html#d0e2494)is one possible alternative if the server addresses (IP or hostname) are known ahead of time.


Orbit exposes the JGroups configuration so developers can configure the clustering as they require.  




Advanced Usage {#AdvancedTopic-ClusterConfiguration-AdvancedUsage}
----------


To perform more advanced changes to the JGroups stack it is possible to write a custom JGroups file and add it the the classpath.

**JGroups Classpath** 
{% highlight xml %}
classpath:/conf/jgroups.xml
{% endhighlight %}

When using maven this will be: src/main/resources/conf/jgroups.xml

