/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */
val grpcVersion = project.rootProject.ext["grpcVersion"]
val guavaVersion = project.rootProject.ext["guavaVersion"]

plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    implementation(project(":src:orbit-util"))
    implementation(project(":src:orbit-shared"))
    implementation(project(":src:orbit-proto"))

    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    implementation("com.google.guava:guava:$guavaVersion-jre")

}
