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

package cloud.orbit.actors.test.reentrant;

import org.junit.Test;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.annotation.Reentrant;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.test.ActorBaseTest;
import cloud.orbit.actors.test.FakeSync;
import cloud.orbit.concurrent.Task;

import javax.inject.Inject;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static com.ea.async.Async.await;
import static org.junit.Assert.fail;

/**
 * @author Johno Crawford (johno@sulake.com)
 */

public class ReentrantTest extends ActorBaseTest
{
    public interface ReentrantActor extends Actor
    {
        Task<Void> doSomething();

        Task<Void> doSomething2(String goStr);

        Task<Void> doSomething3();
    }

    public static class ReentrantActorImpl extends AbstractActor implements ReentrantActor
    {
        @Inject
        FakeSync sync;

        @Override
        @Reentrant
        public Task<Void> doSomething()
        {
            sync.task("lock1").complete("blah");
            await(sync.task("lock2"));
            return Task.done();
        }

        @Override
        @Reentrant
        public Task<Void> doSomething2(String goStr)
        {
            sync.semaphore("all-in").release(1);
            await(sync.task(goStr));
            sync.semaphore("all-in-2").release(1);
            sync.semaphore("all-out-2").acquire(1);
            return Task.done();
        }

        @Override
        public Task<Void> doSomething3()
        {
            return Task.done();
        }
    }

    @Test(timeout = 10000)
    public void testReentrantMethod() throws Exception
    {
        Stage stage1 = createStage();
        try
        {
            ReentrantActor actor = Actor.getReference(ReentrantActor.class, "1");

            Task task = actor.doSomething();
            fakeSync.task("lock1").join();
            // interleaved operation while the first call is blocked
            actor.doSomething3().join();
            fakeSync.task("lock2").complete("blah2");
            task.join();
            dumpMessages();
        }
        finally
        {
            stage1.stop().join();
        }
    }

    @Test
    public void testMethodsRunningInParallel() throws Exception
    {
        Stage stage1 = createStage();
        try
        {
            ReentrantActor actor = Actor.getReference(ReentrantActor.class, "1");

            Task task1 = actor.doSomething2("go1");
            Task task2 = actor.doSomething2("go2");

            // waiting all calls to get to the reentrant part.
            fakeSync.semaphore("all-in").tryAcquire(2, 5, TimeUnit.SECONDS);

            // release the actors, in separated threads, at the same time
            ForkJoinPool.commonPool().execute(() -> fakeSync.task("go1").complete("blah"));
            ForkJoinPool.commonPool().execute(() -> fakeSync.task("go2").complete("blah"));

            try
            {
                // if the code is correct, this has to fail
                // the actor method calls should not run in parallel since one of the calls is blocking a thread due to the use of semaphores
                boolean acquired = fakeSync.semaphore("all-in-2").tryAcquire(2, 5, TimeUnit.SECONDS);
                if (acquired)
                {
                    fail("Error!!! The actor methods are running in parallel!");
                }
            }
            finally
            {
                fakeSync.semaphore("all-out-2").release(2);
            }
            task1.get(5, TimeUnit.SECONDS);
            task2.get(5, TimeUnit.SECONDS);
        }
        finally
        {
            stage1.stop().join();
        }
    }
}