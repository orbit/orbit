---
title: "Server Configuration"
weight: 1
---

Orbit Server offers extensive configuration and an extension model to support many situations outside the defaults. The `OrbitServerConfig` class supplied to the `OrbitServer` constructor has overrides for lease timing, process management, addressable and node storage, and metrics.

The `OrbitServerConfig` class is well documented and will be the authoritative source for the latest options. Some settings, like the `pipelineBufferCount` is a simple Int type. Others, like the `addressableLeaseDuration` are more complex objects.

`ExternallyConfigured<T>` settings allow replacement of implementations of some classes, such as the Node Directory. They are settable from serialized JSON data in a configuration file. The configuration class must derive from ExternallyConfigured<T>, where T is the interface this implementation will replace.

A truncated example of the EtcdNodeDirectory implementation of Node Directory:
```kotlin
class EtcdNodeDirectory(config: EtcdNodeDirectoryConfig, private val clock: Clock) : NodeDirectory {
    data class EtcdNodeDirectoryConfig(
        val url: String
    ) : ExternallyConfigured<NodeDirectory> {
        override val instanceType: Class<out NodeDirectory> = EtcdNodeDirectory::class.java
    }
    ... EtcdNodeDirectory implementation
}
```

## Configuration File

Orbit Server is set up with sensible default values for a simple, single node service. The easiest way to customize an Orbit Server is to use a configuration file. Orbit Server by default uses a `SettingsLoader` class which loads from a JSON-formatted configuration file. This file is searched for in 3 ways:

### ORBIT_SETTINGS_RAW environment variable
Orbit will first attempt to read a raw JSON data from an environment variable `ORBIT_SETTINGS_RAW`.

### ORBIT_SETTINGS environment variable
Orbit will then attempt to load a configuration file found at the path specified in the `ORBIT_SETTINGS` environment variable, such as `/etc/orbit/orbit.json`.

### orbit.settings system property
Orbit will finally attempt to load a configuration file found at the path speciied in the 'orbit.settings` system property.

For ExternallyConfigured classes, configuration takes the form of a serialized class to be loaded from Jackson ObjectMapper:

```json
    "nodeDirectory": [
        "orbit.server.etcd.EtcdNodeDirectory$EtcdNodeDirectoryConfig",
        {
            "url": "http://orbit-node-directory:2379"
        }
    ],

```
