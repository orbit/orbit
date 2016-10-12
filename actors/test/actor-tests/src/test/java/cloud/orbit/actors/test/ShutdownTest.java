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
import cloud.orbit.actors.runtime.ActorRuntime;
import cloud.orbit.actors.runtime.NodeCapabilities;
import cloud.orbit.concurrent.Task;

import org.junit.Test;

import javax.inject.Inject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ShutdownTest extends ActorBaseTest
{


    public interface Shut extends Actor
    {
        Task<Void> doSomething();

        Task<Void> doSomethingBlocking();
    }

    public static class ShutActor extends AbstractActor implements Shut
    {
        @Inject
        FakeSync fakeSync;

        public Task<Void> doSomething()
        {
            getLogger().info("doSomething: " + ActorRuntime.getRuntime());
            return Task.done();
        }

        public Task<Void> doSomethingBlocking()
        {
            getLogger().info("releasing executing");
            fakeSync.semaphore("executing").release();
            fakeSync.semaphore("canFinish").acquire(10, TimeUnit.SECONDS);
            return Task.done();
        }
    }

    @Test
    public void basicShutdownTest() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        logger.info("stage1: " + stage1);
        Stage client = createClient();
        logger.info("client1: " + client);

        Actor.getReference(Shut.class, "0").doSomething().join();
        Stage stage2 = createStage();
        logger.info("stage2: " + stage2);

        stage1.stop().join();

        stage2.bind();
        Actor.getReference(Shut.class, "0").doSomething().join();
    }


    @Test(timeout = 10_000L)
    public void asyncShutdownTest() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        Stage client = createClient();

        Task<Void> methodCall = Actor.getReference(Shut.class, "0").doSomethingBlocking();

        // wait the server start processing
        fakeSync.semaphore("executing").acquire();
        logger.info("released");
        Stage stage2 = createStage();
        assertFalse(methodCall.isDone());
        final Task<?> stopFuture = Task.runAsync(() -> stage1.stop().join());
        waitFor(() -> stage1.getState() == NodeCapabilities.NodeState.STOPPING);

        // release doSomethingTo finish
        logger.info("releasing canFinish");
        fakeSync.semaphore("canFinish").release();
        stopFuture.join();

        // tries to ensure that the current running methods finish before the shutdown.
        eventuallyTrue(() -> methodCall.isDone());

    }


}
