/*
 Copyright (C) 2017 Electronic Arts Inc.  All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1.  Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
 2.  Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
 3.  Neither the name of Electronic Arts, Inc. ("EA") nor the names of
     its contributors may be used to endorse or promote products derived
     from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY ELECTRONIC ARTS AND ITS CONTRIBUTORS "AS IS" AND ANY
 EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL ELECTRONIC ARTS OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package cloud.orbit.actors.util;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;

public class IdUtils
{
    private static AtomicLong nexLongId = new AtomicLong();

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
     * <p/>
     * This method uses SecureRandom, same as UUID. An 128 bit id generated with this function will be as unique as an UUID
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

    /**
     * Returns long id unique for this jvm instance.
     */
    public static long sequentialLongId()
    {
        return nexLongId.incrementAndGet();
    }

}
