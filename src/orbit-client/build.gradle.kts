/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val kotlinCoroutinesVersion = project.rootProject.ext["kotlinCoroutinesVersion"]
val slf4jVersion = project.rootProject.ext["slf4jVersion"]
val jacksonVersion = project.rootProject.ext["jacksonVersion"]

plugins {
    kotlin("jvm")
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "5.2.0"

}

dependencies {
    api(project(":src:orbit-util"))
    api(project(":src:orbit-shared"))
    implementation(project(":src:orbit-proto"))


    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:$kotlinCoroutinesVersion")

    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    implementation("io.github.classgraph:classgraph:4.8.60")


    testImplementation(project(":src:orbit-server"))
}

tasks.withType<ShadowJar>() {
    classifier = "shaded"

    relocate("com.fasterxml", "shaded.fasterxml")
    relocate("com.google", "shaded.google")
    relocate("google", "shaded.google")
    relocate("grpc", "shaded.grpc")
    relocate("io.grpc", "shaded.grpc")
    relocate("io.rouz", "shaded.rouz")
}
tasks.build.get().finalizedBy("shadowJar")

plugins.withType<MavenPublishPlugin> {
    extensions.configure<PublishingExtension> {
        publications {
            getByName<MavenPublication>("default") {
                artifact(tasks.getByName("shadowJar"))
            }
        }
    }
}