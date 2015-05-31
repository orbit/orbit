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

package com.ea.orbit.actors.extensions.memcached;

import com.schooner.MemCached.AbstractTransCoder;
import com.schooner.MemCached.TransCoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * {@link TransCoder} which writes and reads strings directly as bytes.
 * The MySQL memcached plugin strips the string length bytes for some reason..
 *
 * java.io.StreamCorruptedException: invalid stream header: 7B226F62
 * at java.io.ObjectInputStream.readStreamHeader(ObjectInputStream.java:806)
 * at java.io.ObjectInputStream.<init>(ObjectInputStream.java:299)
 * at com.schooner.MemCached.ObjectTransCoder.decode(Unknown Source)
 *
 * @author Johno Crawford (johno@sulake.com)
 */
public class StringTransCoder extends AbstractTransCoder
{

    private final Charset UTF8 = Charset.forName("UTF-8");

    @Override
    public void encode(OutputStream outputStream, Object object) throws IOException
    {
        outputStream.write(String.valueOf(object).getBytes(UTF8));
    }

    @Override
    public Object decode(InputStream inputStream) throws IOException
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        copy(inputStream, outputStream);
        return new String(outputStream.toByteArray(), UTF8);
    }

    private void copy(InputStream from, ByteArrayOutputStream to) throws IOException
    {
        byte[] buffer = new byte[4096];
        while (true)
        {
            int r = from.read(buffer);
            if (r == -1)
            {
                break;
            }
            to.write(buffer, 0, r);
        }
    }
}
