/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.common.util

import java.security.SecureRandom
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicLong

/**
 * Utilities for generating random values.
 */
object RandomUtils {
    private val allowedChars = charArrayOf(
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    )

    private val secureRNG = SecureRandom()
    private val pseudoRNG get() = ThreadLocalRandom.current()

    private val sequentialLongId = AtomicLong(0)

    private fun generateRandomString(numCharacters: Int, random: Random): String {
        if (numCharacters <= 0) throw IllegalArgumentException("numCharacters must be > 0")

        val targetString = StringBuilder(numCharacters)

        val allowedCharsCount = allowedChars.size
        repeat(numCharacters) {
            targetString.append(allowedChars[random.nextInt(allowedCharsCount)])
        }
        return targetString.toString()
    }

    /**
     * Creates a random string which is cryptographically secure.
     *
     * @param numChars The number of characters to generate.
     * @return The random string.
     */
    fun secureRandomString(numChars: Int = 16) = generateRandomString(numChars, secureRNG)

    /**
     * Creates a random string which is not cryptographically secure.
     *
     * @param numChars The number of characters to generate.
     * @return The random string.
     */
    fun pseudoRandomString(numChars: Int = 16) = generateRandomString(numChars, pseudoRNG)

    /**
     * Gives an atomic number which is guaranteed unique in this JVM.
     *
     * @return The number.
     */
    fun sequentialId() = sequentialLongId.getAndIncrement()
}