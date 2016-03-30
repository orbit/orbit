/*
 Copyright (C) 2016 Electronic Arts Inc.  All rights reserved.

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

package cloud.orbit.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;

public class IOUtils
{
    private IOUtils()
    {
        // Placating HideUtilityClassConstructorCheck
    }

    public static String toString(final InputStream input)
            throws IOException
    {
        return toString(input, Charset.defaultCharset());
    }

    public static String toString(final InputStream input, final Charset encoding)
            throws IOException
    {
        return toString(new InputStreamReader(input, encoding));
    }

    public static String toString(final Reader input)
            throws IOException
    {
        final StringBuilder sb = new StringBuilder();
        final char[] buffer = new char[4096];
        for (int charsRead; -1 != (charsRead = input.read(buffer)); )
        {
            sb.append(buffer, 0, charsRead);
        }
        return sb.toString();
    }

    public static void copy(final Reader input, final Writer output) throws IOException
    {
        final char[] buffer = new char[4096];

        for (int charsRead; -1 != (charsRead = input.read(buffer)); )
        {
            output.write(buffer, 0, charsRead);
        }
    }

    public static void copy(final Reader input, final File output) throws IOException
    {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(output), Charset.defaultCharset()))
        {
            copy(input, writer);
        }
    }


    public static byte[] toByteArray(InputStream input) throws IOException
    {
        byte[] buffer = new byte[Math.max(1024, input.available())];
        int offset = 0;
        for (int bytesRead; -1 != (bytesRead = input.read(buffer, offset, buffer.length - offset)); )
        {
            offset += bytesRead;
            if (offset == buffer.length)
            {
                buffer = Arrays.copyOf(buffer, buffer.length + Math.max(input.available(), buffer.length >> 1));
            }
        }
        return (offset == buffer.length) ? buffer : Arrays.copyOf(buffer, offset);
    }

    /**
     * Closes given {@link Closeable}, suppressing all exceptions.
     *
     * @param toClose {@code Closeable} to close, can be {@code null}
     */
    public static void silentlyClose(Closeable toClose)
    {
        if (toClose != null)
        {
            try
            {
                toClose.close();
            }
            catch (IOException ignored)
            {
                // ignore
            }
        }
    }
}
