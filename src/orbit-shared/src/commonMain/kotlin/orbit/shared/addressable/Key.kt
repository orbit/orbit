/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.shared.addressable

/**
 * A key that represents an identity.
 */
sealed class Key {
    companion object {
        /**
         * Creates a key from a given value.
         *
         * @param value The value to convert to a key.
         * @return The key.
         * @throws IllegalArgumentException If the provided value can not be converted to a key.
         */
        fun of(value: Any): Key =
            when (value) {
                is String -> StringKey(value)
                is Int -> Int32Key(value)
                is Long -> Int64Key(value)
                is NoKey -> NoKey
                else -> error("No key type for '${value::class.simpleName}'")
            }

        /**
         * A key with type [NoKey].
         */
        fun none(): Key = NoKey
    }

    /**
     * A key that represents no value.
     */
    object NoKey : Key() {
        override fun toString(): String {
            return "${this::class.simpleName}()"
        }

        override fun equals(other: Any?): Boolean {
            if (other == null) return false
            if (NoKey::class.isInstance(other)) return true
            return false
        }

        override fun hashCode(): Int {
            return 194837
        }
    }

    /**
     * A key that uses a string identity.
     */
    data class StringKey(val key: String) : Key()

    /**
     * A key that uses an int32 identity.
     */
    data class Int32Key(val key: Int) : Key()

    /**
     * A key that uses an int64 identity.
     */
    data class Int64Key(val key: Long) : Key()
}