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

import static com.ea.async.Async.await;
import static org.junit.Assert.assertEquals;

public class StatelessStateTest extends ActorBaseTest
{
    @StatelessWorker
    public interface StatelessHello extends Actor
    {
        Task<String> sayHello(String msg);
    }

    public static class StatelessHelloActor extends AbstractActor<StatelessHelloActor.State> implements StatelessHello
    {
        @Inject
        private FakeSync sync;

        public static class State
        {
            String lastMessage = "";
        }

        @Override
        public Task<String> sayHello(String msg)
        {
            final Task<String> value = Task.fromValue(state().lastMessage + msg);
            state().lastMessage = msg;
            await(writeState());
            return value;
        }


    }

    @Test
    public void testStatePersistence() throws ExecutionException, InterruptedException
    {
        // Stateless worker means that the actor can have multiple activations accessed concurrently,
        // we still allow the actor to have an state.
        // it's the application's problem to handle the concurrency issues

        final Stage stage1 = createStage();
        StatelessHello actor1 = Actor.getReference(StatelessHello.class, "1000");
        assertEquals("hello1!", actor1.sayHello("hello1!").join());
        assertEquals("hello1!hello2!", actor1.sayHello("hello2!").join());
        stage1.stop().join();

        createStage();
        assertEquals("hello2!hello3!", actor1.sayHello("hello3!").join());
        dumpMessages();
    }
}
