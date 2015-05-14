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

package com.ea.orbit.actors.test;


import com.ea.orbit.actors.IActor;
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.concurrent.Task;

import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unused")
public class MessagingTest extends ActorBaseTest
{

    public static interface IBlockingResponder extends IActor
    {
        Task<?> blockOnReceiving(final int semaphoreIndex);

        Task<String> receiveAndRespond();

        Task<String> justRespond();
    }

    static Semaphore semaphores[] = new Semaphore[]{new Semaphore(0), new Semaphore(0)};

    @SuppressWarnings("rawtypes")
    public static class BlockingResponder extends AbstractActor implements IBlockingResponder
    {
        public Task<?> blockOnReceiving(final int semaphoreIndex)
        {
            // blocking the message receiver thread.
            // If the system was correctly implemented this will not block other actors from receiving messages.
            return IActor.getReference(IBlockingResponder.class, "0").justRespond().thenRun(() ->
                    {
                        try
                        {
                            // blocking to try to use all the threads of the message receiver pool.
                            // this won't work very well if the same test is executed twice,
                            // not a concern with this maven project.
                            // using a different semaphore for each test.
                            semaphores[semaphoreIndex].acquire(1);
                        }
                        catch (InterruptedException ex)
                        {
                            // not relevant
                        }
                    }
            );
        }

        @Override
        public Task<String> receiveAndRespond()
        {
            // Calls another actor and returns that actor's response
            return IActor.getReference(IBlockingResponder.class, "0").justRespond().thenApply(
                    x ->
                    {
                        getLogger().debug("message received: " + x);
                        return x;
                    });

        }

        @Override
        public Task<String> justRespond()
        {
            try
            {
                Thread.sleep(5);
            }
            catch (InterruptedException ex)
            {
                // not relevant
            }
            return Task.fromValue("hello");
        }
    }

    /**
     * Ensures that the use of thenRun, thenCompose, whenDone, etc with a
     * response object won't block the reception of new messages.
     */
    @Test
    public void blockingReceptionTest() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        Stage client = createClient();

        IBlockingResponder blockingResponder = IActor.getReference(IBlockingResponder.class, "1");
        IBlockingResponder responder = IActor.getReference(IBlockingResponder.class, "free");
        final Task<?> blockedRes = blockingResponder.blockOnReceiving(0);
        final Task<?> res = responder.receiveAndRespond();
        assertEquals("hello", res.join());
        assertFalse(blockedRes.isDone());
        semaphores[0].release(1);
        blockedRes.join();
        assertFalse(blockedRes.isCompletedExceptionally());
    }


    /**
     * Ensures that the use of thenRun, thenCompose, whenDone, etc with a
     * response object won't block the reception of new messages.
     */
    @Test
    public void blockingReceptionTestWithABunch() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        Stage client = createClient();

        IBlockingResponder blockingResponder2 = IActor.getReference(IBlockingResponder.class, "free");
        ArrayList<Task<?>> blocked = new ArrayList<>();
        for (int i = 0; i < 20; i++)
        {
            IBlockingResponder blockingResponder1 = IActor.getReference(IBlockingResponder.class, "100" + i);
            blocked.add(blockingResponder1.blockOnReceiving(1));
        }
        // bad practice, but just to ensure that the other messages have get there before this last one.
        Thread.sleep(5);
        final Task<?> res2 = blockingResponder2.receiveAndRespond();
        long start = System.currentTimeMillis();
        assertEquals("hello", res2.join());
        assertTrue(System.currentTimeMillis() - start < 30000);
        final Task<Object> all = Task.anyOf(blocked);
        assertFalse(all.isDone());
        semaphores[1].release(20);
        all.join();
        assertTrue(all.isDone());
        assertFalse(all.isCompletedExceptionally());
    }

}
