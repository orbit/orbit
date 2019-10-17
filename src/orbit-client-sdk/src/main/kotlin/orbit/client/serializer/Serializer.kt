/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.client.serializer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator
import com.fasterxml.jackson.module.kotlin.registerKotlinModule


internal class Serializer {
    private val validator: PolymorphicTypeValidator = BasicPolymorphicTypeValidator.builder()
        .allowIfBaseType(Any::class.java)
        .build()

    private val mapper = ObjectMapper()
        .activateDefaultTyping(validator)
        .registerKotlinModule()

    fun serialize(obj: Any) =
        mapper.writeValueAsString(obj)

    fun serializeArgs(args: Array<Any?>) = serialize(args)

    fun deserializeArgs(args: String): Array<Any?> = mapper.readValue(args, Array<Any?>::class.java)
}