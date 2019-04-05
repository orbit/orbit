/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.runtime.serialization.kryo

import cloud.orbit.core.key.Key
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

internal class NoKeySerializer : Serializer<Key.NoKey>(true, true) {
    override fun write(kryo: Kryo, output: Output, obj: Key.NoKey?): Unit =
        if (obj != null) {
            output.writeBoolean(true)
        } else {
            output.writeBoolean(false)
        }

    override fun read(kryo: Kryo, input: Input, type: Class<out Key.NoKey>): Key.NoKey? =
        if (input.readBoolean()) {
            Key.NoKey
        } else {
            null
        }

    override fun copy(kryo: Kryo, original: Key.NoKey?): Key.NoKey? = original
}
