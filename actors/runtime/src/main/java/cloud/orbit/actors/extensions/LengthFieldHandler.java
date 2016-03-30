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
package cloud.orbit.actors.extensions;

import cloud.orbit.actors.net.Handler;
import cloud.orbit.actors.net.HandlerContext;
import cloud.orbit.actors.peer.PeerExtension;
import cloud.orbit.actors.runtime.DefaultHandlers;
import cloud.orbit.concurrent.Task;
import cloud.orbit.tuples.Pair;

import java.util.LinkedList;
import java.util.List;

/**
 * Prepends each message with a 4 byte big endian length field.
 * The the length indicates the data length.
 * <p/>
 * Example:
 * <pre>
 *     | length      | payload        |
 *     | 00 00 00 05 | 6f 72 62 69 74 | ____orbit
 * </pre>
 */
public class LengthFieldHandler extends NamedPipelineExtension implements Handler, PipelineExtension, PeerExtension
{
    public static final String LENGTH_FIELD_ENCODING = "length-field-encoding";

    private int length;
    private int position = -4;
    private byte[] currentMessage;
    private final Object mutex = new Object();

    public LengthFieldHandler()
    {
        super(LENGTH_FIELD_ENCODING, null, DefaultHandlers.SERIALIZATION);
    }

    public LengthFieldHandler(final String name, final String beforeHandlerName, final String afterHandlerName)
    {
        super(name, beforeHandlerName, afterHandlerName);
    }

    @Override
    public Task write(final HandlerContext ctx, final Object msg) throws Exception
    {
        if ((msg instanceof Pair))
        {
            return writeBytes(ctx, (Pair) msg);
        }
        else
        {
            return ctx.write(msg);
        }
    }

    private Task writeBytes(final HandlerContext ctx, final Pair pair)
    {
        byte[] bytes = (byte[]) pair.getRight();
        int length = bytes.length;
        final byte[] wrapping = new byte[length + 4];
        wrapping[0] = (byte) ((length >> 24) & 0xff);
        wrapping[1] = (byte) ((length >> 16) & 0xff);
        wrapping[2] = (byte) ((length >> 8) & 0xff);
        wrapping[3] = (byte) ((length) & 0xff);
        System.arraycopy(bytes, 0, wrapping, 4, length);
        return ctx.write(Pair.of(pair.getLeft(), wrapping));
    }


    @Override
    public void onRead(final HandlerContext ctx, final Object msg) throws Exception
    {
        // expecting the network to call this serially
        if (msg instanceof Pair)
        {
            onReceiveBytes(ctx, (Pair) msg);
        }
        else
        {
            ctx.fireRead(msg);
        }
    }

    private void onReceiveBytes(final HandlerContext ctx, final Pair pair)
    {
        // this must be called serially
        byte[] data = (byte[]) pair.getRight();
        List<Pair> reads = null;
        synchronized (mutex)
        {
            byte[] currentMessage = this.currentMessage;
            int position = this.position;
            int length = this.length;

            for (int offset = 0; offset < data.length; )
            {
                if (currentMessage == null)
                {
                    for (; position < 0 && offset < data.length; position++, offset++)
                    {
                        length = (length << 8) | (data[offset] & 0xff);
                    }
                    if (offset >= data.length)
                    {
                        break;
                    }
                    // new message arriving
                    position = 0;
                    currentMessage = new byte[length];
                }
                int take = Math.min(currentMessage.length - position, data.length - offset);
                System.arraycopy(data, offset, currentMessage, position, take);
                offset += take;
                position += take;
                if (position == currentMessage.length)
                {
                    if (reads == null)
                    {
                        reads = new LinkedList<>();
                    }
                    reads.add(Pair.of(pair.getLeft(), currentMessage));
                    currentMessage = null;
                    position = -4;
                    length = 0;
                }
            }
            this.currentMessage = currentMessage;
            this.position = position;
            this.length = length;
        }
        if (reads != null)
        {
            reads.forEach(ctx::fireRead);
        }
    }

}
