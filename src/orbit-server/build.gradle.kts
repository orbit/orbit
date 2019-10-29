/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */
val grpcVersion = project.rootProject.ext["grpcVersion"]
val guavaVersion = "28.1"

plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    implementation(project(":src:orbit-util"))
    implementation(project(":src:orbit-shared"))
    implementation(project(":src:orbit-proto"))

    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    implementation("org.jgrapht:jgrapht-core:1.3.0")

}
