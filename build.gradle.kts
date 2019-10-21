/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

val grpcVersion by extra("1.23.0")
val grpcKotlinVersion by extra("0.1.1")
val protobufVersion by extra("3.9.1")
val kotlinCoroutinesVersion by extra("1.3.1")
val slf4jVersion by extra("1.7.26")
val jetcdVersion by extra("0.3.1-SNAPSHOT")
val guavaVersion by extra("28.1")

plugins {
    val kotlinVersion = "1.3.50"

    base
    kotlin("multiplatform") version kotlinVersion apply false
}

allprojects {
    repositories {
        mavenCentral()
        jcenter()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

subprojects {
    tasks.withType<KotlinJvmCompile>().all {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}

