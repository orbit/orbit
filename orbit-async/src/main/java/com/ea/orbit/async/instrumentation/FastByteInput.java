/*
 Copyright (C) 2015 Electronic Arts Inc.  All rights reserved.

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

package com.ea.orbit.async.instrumentation;

import java.io.InputStream;

// fast byte input just for asm
class FastByteInput extends InputStream
{
    protected final byte[] buf;

    protected final int length;

    protected int pos = 0;

    public FastByteInput(byte[] buf)
    {
        this.buf = buf;
        this.length = buf.length;
    }

    public final int read()
    {
        return (pos < length) ? (buf[pos++] & 0xff) : -1;
    }

    public final int read(final byte[] b, final int off, final int len)
    {
        // not checking if length lesser than zero
        if ((pos + len) > length)
        {
            int len2 = (length - pos);
            System.arraycopy(buf, pos, b, off, len2);
            pos = length;
            return len2;
        }
        System.arraycopy(buf, pos, b, off, len);
        pos += len;
        return len;
    }

    public final long skip(long n)
    {
        // not checking if length lesser than zero
        if ((pos + n) > length)
        {
            int n2 = length - pos;
            pos = length;
            return n2;
        }
        if (n < 0)
        {
            return 0;
        }
        pos += n;
        return n;
    }

    public final int available()
    {
        return length - pos;
    }

}
