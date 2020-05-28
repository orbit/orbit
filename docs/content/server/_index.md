---
title: "Server"
weight: 3
---

Orbit Server can be run right out of the box, with several easy ways to get started.

## Docker container

The simplest way to start an Orbit Server for development is in a Docker container from the Docker Hub [image](https://hub.docker.com/r/orbitframework/orbit).

```shell script
> docker run -it -p 50056:50056 orbitframework/orbit:{{< release >}}
```

## Kubernetes

Orbit Server comes packaged as a helm chart hosted on Github. Include Orbit Server as a dependency in your application's helm charts.

```yaml
dependencies:
- name: orbit
  version: {{< release >}}
  repository: https://www.orbit.cloud/orbit
  condition: enabled
```

To configure Orbit, set overrides in a values.yaml file or use the --setValues switches when running `helm install`.

```yaml
orbit:
  url: localhost
  node:
    replicas: 1
    containerPort: 50056
  addressableDirectory:
    replicas: 1
  nodeDirectory:
    replicas: 1
```

## Hosted
Most scenarios are supported with the turnkey methods above. For some advanced situations, it may be preferred to host the Orbit Server runtime within an existing server application.

For help on hosting Orbit Server, check out the [Hosting](/server/hosting) page.

The best example of hosting the Orbit Server components can be found in the [**orbit-application**](https://github.com/orbit/orbit/blob/master/src/orbit-application/src/main/kotlin/orbit/application/App.kt) module.

