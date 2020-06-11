/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.serializer

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

internal class Serializer {
    private val validator: PolymorphicTypeValidator = BasicPolymorphicTypeValidator.builder()
        .allowIfBaseType(Any::class.java)
        .build()

    private val mapper = ObjectMapper()
        .activateDefaultTyping(validator, ObjectMapper.DefaultTyping.EVERYTHING)
        .registerKotlinModule()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    fun <T : Any> serialize(obj: T?): String = mapper.writeValueAsString(obj)
    fun <T : Any> deserialize(str: String, clazz: Class<out T>): T = mapper.readValue(str, clazz)
    inline fun <reified T : Any> deserialize(str: String) = deserialize(str, T::class.java)
}