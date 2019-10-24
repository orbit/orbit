/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */
val kotlinCoroutinesVersion = project.rootProject.ext["kotlinCoroutinesVersion"]
val slf4jVersion = project.rootProject.ext["slf4jVersion"]

plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    implementation(project(":src:orbit-util"))
    implementation(project(":src:orbit-shared"))
    implementation(project(":src:orbit-proto"))


    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:$kotlinCoroutinesVersion")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.10.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.0")

    implementation("io.github.classgraph:classgraph:4.8.47")


    testImplementation(project(":src:orbit-server"))
}