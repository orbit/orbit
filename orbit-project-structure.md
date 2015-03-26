---
layout : page
title : "Orbit : Project Structure"
breadCrumb : "[Orbit](index.html) / [Public Documentation](orbit-public-documentation.html)"
next : "orbit-actors.html"
previous: "orbit-building-orbit.html"
---
{% include JB/setup %}



-  [Source Structure](#ProjectStructure-SourceStructure)
-  [Maven Structure](#ProjectStructure-MavenStructure)



Source Structure {#ProjectStructure-SourceStructure}
----------


Orbit is organized into a set of folders which contain the high level systems


<table>
<tr><th> Component </th><th> Path </th><th> Purpose </th></tr>
<tr><td> Actors </td><td> /actors </td><td> Orbit Actors is a framework to write distributed systems using virtual actors. It abstracts much of the work that programmers are usually required to perform in order to work with distributed actors such state management, actor addressability and actor lifetime.  </td></tr>
<tr><td> Container </td><td> /container </td><td>

Orbit Container is a minimal inversion of control container designed to make writing and managing applications easier by simplifying object injection, service location, application configuration and dependency management. It abstracts the implementation details of the underlying technology away from programmers and operations engineers who are able to develop and maintain different technologies with a unified interface. 

 </td></tr>
<tr><td> Web </td><td> /web </td><td> Orbit Web is a basic implementation of a web service container for Orbit Applications, it uses Jetty and Jersey and offers HTTP and WebSocket endpoints. </td></tr>
<tr><td> Commons </td><td> /commons </td><td> Orbit Commons contains common helper and utility classes which are used across multiple Orbit modules. </td></tr>
<tr><td> Samples </td><td> /samples </td><td> Samples contains the high level samples which leverage the entire Orbit stack. </td></tr>
</table>


 


Maven Structure {#ProjectStructure-MavenStructure}
----------


Orbit is hosted on the maven central repository.


<table>
<tr><th> Group </th><th> Artifact </th><th> Purpose </th></tr>
<tr><td> com.ea.orbit </td><td> orbit-actors-all </td><td> Contains the full actor framework </td></tr>
<tr><td> com.ea.orbit </td><td> orbit-container </td><td> Contains the orbit container system </td></tr>
<tr><td> com.ea.orbit </td><td> orbit-web </td><td> Contains the orbit web system </td></tr>
<tr><td> com.ea.orbit </td><td> orbit-commons </td><td> Contains the common utils </td></tr>
<tr><td> com.ea.orbit </td><td> orbit-actors-mongodb </td><td> Contains the orbit actors MongoDB storage system </td></tr>
<tr><td> com.ea.orbit </td><td> orbit-actors-* </td><td> Contains the various individual actor systems </td></tr>
<tr><td> com.ea.orbit.samples </td><td> orbit-* </td><td> Contains the orbit samples </td></tr>
</table>


 


 

