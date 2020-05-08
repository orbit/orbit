# Overview
The main Orbit project is split into two primary dependencies. orbit-runtime and orbit-core.

# Core
The orbit-core project provides the necessary interfaces and implementations for projects which want to develop their own actors or extensions. However, it does not contain the necessary implementations to run an orbit stage.
The orbit-core project is typically included from abstract actor projects or extension projects where there is no requirement to run an orbit stage itself. It is often also marked as scope "provided" with the expectation that the actual implementation will be provided in the final classpath by another project.

```xml
<dependency>
     <groupId>cloud.orbit</groupId>
     <artifactId>orbit-core</artifactId>
     <version>[ORBIT-VERSION]</version>
</dependency>
```

# Runtime
The orbit-runtime project builds on orbit-core and additionally includes the required implementations to run an orbit stage. This project is typically included in the classpath by the launcher project or container and often provides the real orbit implementation to abstract actor or extension projects.

```xml
<dependency>
     <groupId>cloud.orbit</groupId>
     <artifactId>orbit-runtime</artifactId>
     <version>[ORBIT-VERSION]</version>
</dependency>
```