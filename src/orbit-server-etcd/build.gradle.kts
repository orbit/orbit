/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */
val grpcVersion = project.rootProject.ext["grpcVersion"]
val jetcdVersion = "0.5.0"

plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    implementation(project(":src:orbit-util"))
    implementation(project(":src:orbit-shared"))
    implementation(project(":src:orbit-proto"))
    implementation(project(":src:orbit-server"))

    implementation("io.etcd:jetcd-all:$jetcdVersion")

    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
}
