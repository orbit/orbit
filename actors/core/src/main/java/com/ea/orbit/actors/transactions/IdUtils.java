package com.ea.orbit.actors.transactions;

import java.security.SecureRandom;

public class IdUtils
{

    private static class Holder
    {
        static final SecureRandom numberGenerator = new SecureRandom();
    }

    private static final char[] base64URL = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '_'
    };


    /**
     * Generate a string using base64 url safe characters with numBits bits.
     *
     * @param numBits the number of random bits size of the data
     * @return a random string composed only of url safe characters where each characters represents up to 6 bits fo random data.¶
     */
    public static String urlSafeString(final int numBits)
    {
        SecureRandom ng = Holder.numberGenerator;
        StringBuilder sb = new StringBuilder(1 + (numBits / 6));
        for (int i = numBits; i > 0; i -= 6)
        {
            sb.append(base64URL[ng.nextInt(i > 6 ? 64 : (1 << i))]);
        }
        return sb.toString();
    }

}
