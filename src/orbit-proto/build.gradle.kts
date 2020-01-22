/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.ofSourceSet
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

val grpcVersion = project.rootProject.ext["grpcVersion"]
val grpcKotlinVersion = project.rootProject.ext["grpcKotlinVersion"]
val protobufVersion = project.rootProject.ext["protobufVersion"]
val kotlinCoroutinesVersion = project.rootProject.ext["kotlinCoroutinesVersion"]

plugins {
    kotlin("jvm")
    id("com.google.protobuf") version "0.8.10"
    `maven-publish`
}

dependencies {
    implementation(project(":src:orbit-shared"))
    implementation(project(":src:orbit-util"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:$kotlinCoroutinesVersion")

    compile("com.google.protobuf:protobuf-java:$protobufVersion")
    compile("io.grpc:grpc-protobuf:$grpcVersion")
    compile("io.grpc:grpc-stub:$grpcVersion")

    if (JavaVersion.current().isJava9Compatible) {
        compileOnly("javax.annotation:javax.annotation-api:1.3.1")
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }

    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }

        id("grpckotlin") {
            artifact = "io.rouz:grpc-kotlin-gen:$grpcKotlinVersion"
        }
    }

    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc")
                id("grpckotlin")
            }
        }
    }
}

sourceSets {
    main {
        java {
            srcDirs("build/generated/source/proto/main/java")
            srcDirs("build/generated/source/proto/main/grpc")
            srcDirs("build/generated/source/proto/main/grpckotlin")
        }
    }
}