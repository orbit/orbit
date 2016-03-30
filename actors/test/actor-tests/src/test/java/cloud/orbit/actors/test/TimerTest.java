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

package cloud.orbit.actors.test;


import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.test.actors.SomeChatRoom;
import cloud.orbit.concurrent.Task;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unused")
public class TimerTest extends ActorBaseTest
{
    // At the moment the clock injected to the stage is not used by the timer subsystem.

    public static class SomeChatObserver implements cloud.orbit.actors.test.actors.SomeChatObserver
    {
        BlockingQueue<Pair<cloud.orbit.actors.test.actors.SomeChatObserver, String>> messagesReceived = new LinkedBlockingQueue<>();

        @Override
        public Task<Void> receiveMessage(final cloud.orbit.actors.test.actors.SomeChatObserver sender, final String message)
        {
            messagesReceived.add(Pair.of(sender, message));
            return Task.done();
        }
    }

    @Test
    public void timerTest() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        Stage frontend = createClient();

        SomeChatRoom chatRoom = Actor.getReference(SomeChatRoom.class, "1");
        SomeChatObserver observer = new SomeChatObserver();
        chatRoom.join(observer).get();

        long start = System.currentTimeMillis();
        chatRoom.startCountdown(5, "counting").get();

        assertNotNull("counting 5", observer.messagesReceived.poll(20, TimeUnit.SECONDS).getRight());
        assertNotNull("counting 4", observer.messagesReceived.poll(2000, TimeUnit.SECONDS).getRight());
        assertNotNull("counting 3", observer.messagesReceived.poll(5, TimeUnit.SECONDS).getRight());
        assertNotNull("counting 2", observer.messagesReceived.poll(5, TimeUnit.SECONDS).getRight());
        assertNotNull("counting 1", observer.messagesReceived.poll(5, TimeUnit.SECONDS).getRight());
        // some timer must have elapsed!
        assertTrue(System.currentTimeMillis() - start > 10);
        // ensures no new messages are received
        assertNull(observer.messagesReceived.poll(10, TimeUnit.MILLISECONDS));
    }

}
