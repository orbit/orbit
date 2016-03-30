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

package cloud.orbit.actors.test.stateless;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.annotation.StatelessWorker;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.test.ActorBaseTest;
import cloud.orbit.actors.test.FakeSync;
import cloud.orbit.concurrent.Task;

import org.junit.Test;

import javax.inject.Inject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class StatelessDeactivationTest extends ActorBaseTest
{
    @StatelessWorker
    public interface StatelessHello extends Actor
    {
        Task<String> sayHello(String msg);
    }

    public static class StatelessHelloActor extends AbstractActor implements StatelessHello
    {
        @Inject
        private FakeSync sync;

        @Override
        public Task<String> sayHello(String msg)
        {
            return Task.fromValue(msg);
        }

        @Override
        public Task<?> deactivateAsync()
        {
            sync.semaphore("deactivation").release();
            return super.deactivateAsync();
        }
    }

    @Test
    public void deactivationOnStageStop() throws ExecutionException, InterruptedException
    {
        final Stage stage1 = createStage();
        StatelessHello actor1 = Actor.getReference(StatelessHello.class, "1000");
        assertEquals("hello", actor1.sayHello("hello").join());
        stage1.stop().join();
        fakeSync.semaphore("deactivation").acquire(1, TimeUnit.SECONDS);
        dumpMessages();
    }


    @Test
    public void deactivationOnTimeOut() throws ExecutionException, InterruptedException
    {
        clock.stop();
        final Stage stage1 = createStage();
        StatelessHello actor1 = Actor.getReference(StatelessHello.class, "1000");
        assertEquals("hello", actor1.sayHello("hello").join());
        clock.incrementTime(60, TimeUnit.MINUTES);
        stage1.cleanup().join();
        fakeSync.semaphore("deactivation").acquire(1, TimeUnit.SECONDS);
        dumpMessages();
    }


}
