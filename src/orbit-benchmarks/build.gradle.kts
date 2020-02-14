/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

plugins {
    kotlin("jvm")
    id("me.champeau.gradle.jmh") version "0.5.0"
}

dependencies {
    implementation(project(":src:orbit-server"))
    implementation(project(":src:orbit-client"))
}

jmh {
    resultFormat = "csv"
    resultsFile = file("build/reports/benchmarks.csv")
}