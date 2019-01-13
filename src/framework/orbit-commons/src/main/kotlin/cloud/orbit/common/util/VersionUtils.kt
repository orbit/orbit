/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.common.util

/**
 * Represents the versions of the major components in an Orbit deployment.
 */
data class VersionInfo internal constructor(
    /**
     * The exact version of Orbit.
     */
    val orbitVersion: String,
    /**
     * The Orbit specification version.
     */
    val orbitSpecVersion: String,
    /**
     * The codename for this Orbit build.
     */
    val orbitCodename: String,
    /**
     * The version of the JVM this Orbit build is currently running on.
     */
    val jvmVersion: String,
    /**
     * The build information of the JVM this Orbit build is currently running on.
     */
    val jvmBuild: String,
    /**
     * The version of Kotlin this version of Orbit is currently running on.
     */
    val kotlinVersion: String
)

/**
 * Utility for retrieving versioning information.
 */
object VersionUtils {

    /**
     * Retrieves versioning info about the current Orbit environment.
     */
    @JvmStatic
    fun getVersionInfo() = VersionInfo(
        orbitVersion = javaClass.`package`.implementationVersion ?: "dev",
        orbitSpecVersion = javaClass.`package`.specificationVersion ?: "dev",
        orbitCodename = javaClass.`package`.implementationTitle ?: "Orbit",
        jvmVersion = System.getProperty("java.version"),
        jvmBuild = "${System.getProperty("java.vm.vendor")} ${System.getProperty("java.vm.name")} ${System.getProperty("java.vm.version")}",
        kotlinVersion = KotlinVersion.CURRENT.toString()
    )

}