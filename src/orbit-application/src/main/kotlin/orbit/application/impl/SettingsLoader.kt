/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.application.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import mu.KotlinLogging
import orbit.server.OrbitServerConfig
import java.nio.file.Files
import java.nio.file.Paths

internal class SettingsLoader {
    private val logger = KotlinLogging.logger { }
    private val validator: PolymorphicTypeValidator = BasicPolymorphicTypeValidator.builder()
        .allowIfBaseType(Any::class.java)
        .build()

    private val mapper = ObjectMapper(JsonInterpolatorParserFactory())
        .activateDefaultTyping(validator)
        .registerKotlinModule()
        .registerModule(Jdk8Module())
        .registerModule(JavaTimeModule())

    fun loadConfig(): OrbitServerConfig {
        logger.info("Searching for Orbit Settings...")

        val settingsEnv = System.getenv("ORBIT_SETTINGS_RAW")
        if (!settingsEnv.isNullOrBlank()) {
            return mapper.readValue(settingsEnv, OrbitServerConfig::class.java)
        }

        val settingsFileEnv = System.getenv("ORBIT_SETTINGS")
        if (!settingsFileEnv.isNullOrBlank()) {
            val path = Paths.get(settingsFileEnv)
            if (Files.exists(path)) {
                try {
                    String(Files.readAllBytes(path)).also {
                        return mapper.readValue(it, OrbitServerConfig::class.java)
                    }
                } finally {
                }
            }
        }

        val settingsFileProps = System.getProperty("orbit.settings")
        if (!settingsFileProps.isNullOrBlank()) {
            val path = Paths.get(settingsFileProps)
            if (Files.exists(path)) {
                try {
                    String(Files.readAllBytes(path)).also {
                        return mapper.readValue(it, OrbitServerConfig::class.java)
                    }
                } finally {
                }
            }
        }

        logger.info("No settings found. Using defaults.")
        return OrbitServerConfig()
    }
}