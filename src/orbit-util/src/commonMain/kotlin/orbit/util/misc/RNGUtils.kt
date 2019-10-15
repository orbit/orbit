/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.util.misc

import kotlin.random.Random

/**
 * Utilities for generating random values.
 */
object RNGUtils {
    private val allowedChars = charArrayOf(
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    )

    /**
     * Creates a random string.
     *
     * @param numChars The number of characters to generate.
     * @return The random string.
     */
    fun randomString(numChars: Int = 16, random: Random = Random.Default): String {
        if (numChars <= 0) throw IllegalArgumentException("numCharacters must be > 0")

        val targetString = StringBuilder(numChars)

        val allowedCharsCount = allowedChars.size
        repeat(numChars) {
            targetString.append(allowedChars[random.nextInt(allowedCharsCount)])
        }
        return targetString.toString()
    }
}