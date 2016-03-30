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
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.concurrent.Task;

import org.junit.Test;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

@SuppressWarnings("unused")
public class MessagingTest extends ActorBaseTest
{

    public static interface BlockingResponder extends Actor
    {
        Task<?> blockOnReceiving(final int semaphoreIndex);

        Task<String> receiveAndRespond();

        Task<String> justRespond();
    }


    @SuppressWarnings("rawtypes")
    public static class BlockingResponderActor extends AbstractActor implements BlockingResponder
    {
        @Inject
        FakeSync sync;

        public Task<?> blockOnReceiving(final int semaphoreIndex)
        {
            // blocking the message receiver thread.
            // If the system was correctly implemented this will not block other actors from receiving messages.
            return Actor.getReference(BlockingResponder.class, "0")
                    .justRespond()
                    .thenRun(() ->
                            {
                                // blocking to try to use all the threads of the message receiver pool.
                                // this won't work very well if the same test is executed twice,
                                // not a concern with this maven project.
                                // using a different semaphore for each test.
                                sync.getBlockedFuture().join();
                            }
                    );
        }

        @Override
        public Task<String> receiveAndRespond()
        {
            // Calls another actor and returns that actor's response
            return Actor.getReference(BlockingResponder.class, "0").justRespond().thenApply(
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
    @Test(timeout = 10_000L)
    public void blockingReceptionTest() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        Stage client = createClient();

        BlockingResponder blockingResponder = Actor.getReference(BlockingResponder.class, "1");
        BlockingResponder responder = Actor.getReference(BlockingResponder.class, "free");
        final Task<?> blockedRes = blockingResponder.blockOnReceiving(0);
        final Task<?> res = responder.receiveAndRespond();
        eventuallyTrue(() -> fakeSync.blockedFutureCount() == 1);
        assertEquals("hello", res.join());
        assertFalse(blockedRes.isDone());
        fakeSync.completeFutures();
        blockedRes.join();
        assertFalse(blockedRes.isCompletedExceptionally());
    }


    /**
     * Ensures that the use of thenRun, thenCompose, whenDone, etc with a
     * response object won't block the reception of new messages.
     *
     * Can happen if thenRun is executed in the clusterPeer's thread pool.
     *
     * This is possible when Messaging is processing responses.
     * then the Task.complete() method may trigger the execution of actor code.
     */
    @Test(timeout = 10_000L)
    public void blockingReceptionTestWithABunch() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        Stage client = createClient();

        BlockingResponder blockingResponder2 = Actor.getReference(BlockingResponder.class, "free");
        ArrayList<Task<?>> blocked = new ArrayList<>();

        // this number must be greater than the number of threads available to the cluster peer.
        for (int i = 0; i < 20; i++)
        {
            BlockingResponder blockingResponder1 = Actor.getReference(BlockingResponder.class, "100" + i);
            blocked.add(blockingResponder1.blockOnReceiving(1));
        }
        // ensure that the other messages got there before this last one.
        eventuallyTrue(() -> fakeSync.blockedFutureCount() == 20);
        // this will call "0" but shouldn't be stopped
        final Task<?> res2 = blockingResponder2.receiveAndRespond();
        long start = System.currentTimeMillis();
        assertEquals("hello", res2.join());
        assertTrue(System.currentTimeMillis() - start < 30000);
        final Task<Object> all = Task.anyOf(blocked);
        assertFalse(all.isDone());
        fakeSync.completeFutures();
        all.join();
        assertTrue(all.isDone());
        assertFalse(all.isCompletedExceptionally());
    }

}
