/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

val kotlinCoroutinesVersion = project.rootProject.ext["kotlinCoroutinesVersion"]
val kotlinLoggingVersion = "1.7.6"
val slf4jVersion = project.rootProject.ext["slf4jVersion"]


plugins {
    kotlin("multiplatform")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api("io.github.microutils:kotlin-logging-common:$kotlinLoggingVersion")

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-common:$kotlinCoroutinesVersion")
                implementation(kotlin("stdlib-common"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        jvm().compilations["main"].defaultSourceSet {
            dependencies {
                api("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinCoroutinesVersion")

                implementation(kotlin("stdlib-jdk8"))
            }
        }

        jvm().compilations["test"].defaultSourceSet {
            dependencies {
                implementation(kotlin("test-junit"))
                runtimeOnly("org.slf4j:slf4j-simple:$slf4jVersion")
            }
        }
    }
}