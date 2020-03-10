/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

val kotlinLoggingVersion = "1.7.8"

plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    api("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    implementation("io.micrometer:micrometer-core:1.3.5")
}
