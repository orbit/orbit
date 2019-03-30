# Stage

## Introduction

The Orbit stage is a runtime execution container and is the primary way that developers interact with the fraamework.

A collection of stages is known as a cluster. Typically one stage will be used for each node in a cluster.

Stages can be added to or removed from a cluster dynamically.

## Starting a Stage

Before you can interact with a stage it must be created and started.

{% code-tabs %}
{% code-tabs-item title="Kotlin" %}
```kotlin
val stage = Stage()
runBlocking { stage.start().await() }
```
{% endcode-tabs-item %}
{% endcode-tabs %}

{% code-tabs %}
{% code-tabs-item title="Java" %}
```java
Stage stage = new Stage();
stage.start().join();
```
{% endcode-tabs-item %}
{% endcode-tabs %}

