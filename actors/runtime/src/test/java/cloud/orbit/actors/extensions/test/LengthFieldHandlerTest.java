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

package cloud.orbit.actors.extensions.test;


import cloud.orbit.actors.extensions.LengthFieldHandler;
import cloud.orbit.actors.net.DefaultHandlerContext;
import cloud.orbit.actors.net.HandlerContext;
import cloud.orbit.tuples.Pair;

import org.junit.Test;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;


public class LengthFieldHandlerTest
{

    @Test
    public void simpleTest() throws Exception
    {
        final LengthFieldHandler handler = new LengthFieldHandler();

        Queue<Pair<String, byte[]>> queue = new LinkedList<>();
        final HandlerContext context = new MockContext(queue);

        handler.onRead(context, Pair.of("nop", new byte[]{ 0, 0, 0, 1, 99 }));
        Pair<String, byte[]> msg = queue.remove();
        assertEquals(1, msg.getRight().length);
        assertEquals(99, msg.getRight()[0]);
    }

    @Test
    public void testPartialMessage() throws Exception
    {
        final LengthFieldHandler handler = new LengthFieldHandler();

        Queue<Pair<String, byte[]>> queue = new LinkedList<>();
        final HandlerContext context = new MockContext(queue);

        handler.onRead(context, Pair.of("nop", new byte[]{ 0, 0, 0, 2, 99 }));
        assertEquals(0, queue.size());

        handler.onRead(context, Pair.of("nop", new byte[]{ 100 }));
        assertArrayEquals(new byte[]{ 99, 100 }, queue.remove().getRight());
        assertEquals(0, queue.size());
    }

    @Test
    public void testCuttingTheLength() throws Exception
    {
        final LengthFieldHandler handler = new LengthFieldHandler();

        Queue<Pair<String, byte[]>> queue = new LinkedList<>();
        final HandlerContext context = new MockContext(queue);

        handler.onRead(context, Pair.of("nop", new byte[]{ 0, 0 }));
        assertEquals(0, queue.size());

        handler.onRead(context, Pair.of("nop", new byte[]{ 0, 2, 99, 100 }));
        assertArrayEquals(new byte[]{ 99, 100 }, queue.remove().getRight());
    }

    @Test
    public void testTwoMessages() throws Exception
    {
        final LengthFieldHandler handler = new LengthFieldHandler();

        Queue<Pair<String, byte[]>> queue = new LinkedList<>();
        final HandlerContext context = new MockContext(queue);

        handler.onRead(context, Pair.of("nop", new byte[]{ 0, 0, 0, 2, 99, 100, 0, 0, 0, 3, 101, 102, 103 }));
        assertArrayEquals(new byte[]{ 99, 100 }, queue.remove().getRight());
        assertArrayEquals(new byte[]{ 101, 102, 103 }, queue.remove().getRight());
        assertEquals(0, queue.size());
    }

    @Test
    public void randomMessages() throws Exception
    {
        final LengthFieldHandler handler = new LengthFieldHandler();

        Queue<Pair<String, byte[]>> messages = new LinkedList<>();
        final Random random = new Random();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (int i = 0; i < 50; i++)
        {
            // creating random messages
            int length = random.nextInt(1 << ((random.nextInt(3) + 1) * 8));
            byte[] bytes = new byte[length];
            random.nextBytes(bytes);

            // listing all the messages
            messages.add(Pair.of("nop", bytes));


            // joining all the bytes together.
            stream.write((length >> 24) & 0xff);
            stream.write((length >> 16) & 0xff);
            stream.write((length >> 8) & 0xff);
            stream.write((length) & 0xff);
            stream.write(bytes);

        }

        Queue<Pair<String, byte[]>> queue = new LinkedList<>();
        final HandlerContext context = new MockContext(queue);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(stream.toByteArray());
        while (inputStream.available() > 0)
        {
            // feeding the data randomly cut into pieces
            int length = random.nextInt(1 << ((random.nextInt(3) + 1) * 8));
            final byte[] bytes = new byte[length];
            int count = inputStream.read(bytes);
            handler.onRead(context, Pair.of("nop", Arrays.copyOf(bytes, count)));
        }

        assertEquals(messages.size(), queue.size());
        while (!queue.isEmpty())
        {
            assertArrayEquals(messages.remove().getRight(), queue.remove().getRight());
        }
    }


    private static class MockContext extends DefaultHandlerContext
    {
        private final Queue<Pair<String, byte[]>> queue;

        public MockContext(final Queue<Pair<String, byte[]>> queue)
        {
            super(null);
            this.queue = queue;
        }

        @SuppressWarnings("unchecked")
        @Override
        public HandlerContext fireRead(final Object msg)
        {
            queue.add((Pair<String, byte[]>) msg);
            return this;
        }
    }
}
