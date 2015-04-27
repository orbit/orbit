Orbit Async Maven Plugin
============

[Orbit Async](..) implements async-await methods in the JVM. It allows programmers to write asynchronous code in a sequential fashion. It was developed by [BioWare](http://www.bioware.com), a division of [Electronic Arts](http://www.ea.com).

The Orbit Async Maven Plugin executes compile time instrumentation of classes that use Orbit Async. 

A sample project can be found [here](src/test/project-to-test/pom.xml).

Documentation
=======
Documentation is located [here](http://orbit.bioware.com/).

License
=======
Orbit is licensed under the [BSD 3-Clause License](../LICENSE).

Usage
=======

Add the orbit-async dependency:

```xml
<dependency>
    <groupId>com.ea.orbit</groupId>
    <artifactId>orbit-async</artifactId>
    <version>${orbit.version}</version>
</dependency>
```

Add the build plugin that will instrument the uses of `await`

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.ea.orbit</groupId>
            <artifactId>orbit-async-maven-plugin</artifactId>
            <version>${orbit.version}</version>
            <executions>
                <execution>
                    <goals>
                        <goal>instrument</goal>
                        <goal>instrument-test</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

Advanced usage
=========

Make the orbit-async dependency optional. Requires using no methods from orbit-async other than `Await.await`.

This is possible because the instrumentation will replace all references to `await` with other bytecodes.

```xml
<dependency>
    <groupId>com.ea.orbit</groupId>
    <artifactId>orbit-async</artifactId>
    <version>${orbit.version}</version>
    <optional>true</optional>
</dependency>
```

