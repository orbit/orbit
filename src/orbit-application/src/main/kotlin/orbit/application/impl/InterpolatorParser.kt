/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.application.impl

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.io.IOContext
import com.fasterxml.jackson.core.util.JsonParserDelegate
import com.fasterxml.jackson.databind.MappingJsonFactory
import org.apache.commons.text.StringSubstitutor
import org.apache.commons.text.lookup.StringLookup
import java.io.Reader

private class EnvLookUp : StringLookup {
    override fun lookup(key: String): String? =
        System.getenv(key)
}


internal class JsonInterpolatorParser(d: JsonParser?) : JsonParserDelegate(d) {
    private val strSub = StringSubstitutor(EnvLookUp())

    override fun getText(): String? {
        val value = super.getText()
        return value?.let { interpolateString(it) } ?: value
    }

    override fun getValueAsString(): String? {
        return getValueAsString(null)
    }

    override fun getValueAsString(defaultValue: String?): String? {
        val value = super.getValueAsString(defaultValue)
        return value?.let { interpolateString(it) }
    }

    private fun interpolateString(string: String?): String? {
        return strSub.replace(string)
    }
}

internal class JsonInterpolatorParserFactory : MappingJsonFactory() {
    override fun _createParser(
        data: CharArray,
        offset: Int,
        len: Int,
        ctxt: IOContext,
        recyclable: Boolean
    ): JsonParser {
        return JsonInterpolatorParser(super._createParser(data, offset, len, ctxt, recyclable))
    }

    override fun _createParser(r: Reader?, ctxt: IOContext?): JsonParser {
        return JsonInterpolatorParser(super._createParser(r, ctxt))
    }
}