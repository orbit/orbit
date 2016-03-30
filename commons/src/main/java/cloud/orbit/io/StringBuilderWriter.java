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

package cloud.orbit.io;

import java.io.IOException;
import java.io.Writer;

/**
 * Fast non thread safe string writer
 */
public class StringBuilderWriter extends Writer
{
    private StringBuilder sb;

    public StringBuilderWriter()
    {
        this.sb = new StringBuilder();
    }

    public StringBuilderWriter(StringBuilder builder)
    {
        this.sb = builder;
    }

    @Override
    public void write(final char[] cbuf, final int off, final int len) throws IOException
    {
        sb.append(cbuf, off, len);
    }

    @Override
    public void write(final int c) throws IOException
    {
        sb.append((char) c);
    }

    @Override
    public void write(final char[] cbuf) throws IOException
    {
        sb.append(cbuf);
    }

    @Override
    public void write(final String str) throws IOException
    {
        sb.append(str);
    }

    @Override
    public void write(final String str, final int off, final int len) throws IOException
    {
        sb.append(str, off, len);
    }

    @Override
    public Writer append(final CharSequence csq) throws IOException
    {
        sb.append(csq);
        return this;
    }

    @Override
    public Writer append(final CharSequence csq, final int start, final int end) throws IOException
    {
        sb.append(csq, start, end);
        return this;
    }

    @Override
    public Writer append(final char c) throws IOException
    {
        sb.append(c);
        return this;
    }

    @Override
    public void flush() throws IOException
    {

    }

    @Override
    public void close() throws IOException
    {

    }

    @Override
    public String toString()
    {
        return sb.toString();
    }
}
