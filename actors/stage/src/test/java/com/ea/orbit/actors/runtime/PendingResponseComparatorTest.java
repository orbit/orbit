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

package com.ea.orbit.actors.runtime;

import com.ea.orbit.actors.runtime.Messaging.PendingResponseComparator;

import junit.framework.TestCase;

import java.util.concurrent.PriorityBlockingQueue;

import static com.ea.orbit.actors.runtime.Messaging.*;

/**
 * Test for {@link PendingResponseComparator}.
 *
 * @author Johno Crawford (johno@sulake.com)
 */
public class PendingResponseComparatorTest extends TestCase
{
    private static final PendingResponseComparator PENDING_RESPONSE_COMPARATOR = new PendingResponseComparator();

    private static final int BEFORE = -1;
    private static final int EQUAL = 0;
    private static final int AFTER = 1;

    public void testMessageTimeout() throws Exception
    {
        assertEquals(BEFORE, PENDING_RESPONSE_COMPARATOR.compare(new PendingResponse(1, 1), new PendingResponse(1, 30)));
        assertEquals(AFTER, PENDING_RESPONSE_COMPARATOR.compare(new PendingResponse(1, 30), new PendingResponse(1, 1)));
        assertEquals(EQUAL, PENDING_RESPONSE_COMPARATOR.compare(new PendingResponse(1, 1), new PendingResponse(1, 1)));
    }

    public void testMessageId() throws Exception
    {
        assertEquals(AFTER, PENDING_RESPONSE_COMPARATOR.compare(new PendingResponse(2, 1), new PendingResponse(1, 1)));
        assertEquals(BEFORE, PENDING_RESPONSE_COMPARATOR.compare(new PendingResponse(1, 1), new PendingResponse(2, 1)));
        assertEquals(EQUAL, PENDING_RESPONSE_COMPARATOR.compare(new PendingResponse(1, 1), new PendingResponse(1, 1)));
    }

    public void testQueue() throws Exception
    {
        PriorityBlockingQueue<Messaging.PendingResponse> pendingResponsesQueue = new PriorityBlockingQueue<>(10, new PendingResponseComparator());
        pendingResponsesQueue.add(new PendingResponse(3, 60));
        pendingResponsesQueue.add(new PendingResponse(1, 30));
        pendingResponsesQueue.add(new PendingResponse(2, 30));
        assertEquals(1, pendingResponsesQueue.poll().messageId);
        assertEquals(2, pendingResponsesQueue.poll().messageId);
        assertEquals(3, pendingResponsesQueue.poll().messageId);
    }
}
