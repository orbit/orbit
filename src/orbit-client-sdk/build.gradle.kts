/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */
val kotlinCoroutinesVersion = project.rootProject.ext["kotlinCoroutinesVersion"]
val slf4jVersion = project.rootProject.ext["slf4jVersion"]

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":src:orbit-common"))
    implementation(project(":src:orbit-shared"))
    implementation(project(":src:orbit-proto"))

    implementation(kotlin("stdlib-jdk8"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinCoroutinesVersion")

    testRuntimeOnly("org.slf4j:slf4j-simple:$slf4jVersion")
    testImplementation(kotlin("test-junit"))
}