/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    implementation(project(":src:orbit-util"))

    implementation(kotlin("reflect"))
}