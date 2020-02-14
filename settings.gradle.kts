/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

rootProject.name = "orbit"

include(":src:orbit-util")
include(":src:orbit-proto")
include(":src:orbit-shared")

include(":src:orbit-server")
include(":src:orbit-server-etcd")
include(":src:orbit-application")
include(":src:orbit-prometheus")

include(":src:orbit-client")

include(":src:orbit-benchmarks")
