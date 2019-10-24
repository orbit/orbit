/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */
val slf4jVersion = project.rootProject.ext["slf4jVersion"]

val mainClass = "orbit.application.AppKt"

plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":src:orbit-server"))
    implementation(project(":src:orbit-server-etcd"))
    implementation(project(":src:orbit-shared"))
    implementation(project(":src:orbit-util"))

    compile("org.slf4j:slf4j-simple:$slf4jVersion")

}

application {
    mainClassName = mainClass
}

val fatJar = task("fatJar", type = Jar::class) {
    archiveBaseName.set("${project.name}-fat")
    manifest {
        attributes["Implementation-Version"] = project.version
        attributes["Main-Class"] = mainClass
    }
    from(configurations.runtimeClasspath.get().map {
        if (it.isDirectory) {
            it
        } else {
            zipTree(it)
        }
    })
    with(tasks["jar"] as CopySpec)
}