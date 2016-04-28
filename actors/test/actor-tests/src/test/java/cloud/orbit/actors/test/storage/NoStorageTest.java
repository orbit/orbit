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

package cloud.orbit.actors.test.storage;


import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.extensions.json.InMemoryJSONStorageExtension;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.test.ActorBaseTest;
import cloud.orbit.concurrent.Task;

import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static com.ea.async.Async.await;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("unused")
public class NoStorageTest extends ActorBaseTest
{
    public interface Hello extends Actor
    {
        Task<String> sayHello(String greeting);
    }

    public static class HelloActor extends AbstractActor implements Hello
    {
        @Override
        public Task<String> sayHello(final String greeting)
        {
            return Task.fromValue(greeting);
        }
    }

    public interface HelloWithState extends Actor
    {
        Task<String> sayHello(String greeting);
    }

    public static class HelloWithStateActor extends AbstractActor<HelloWithStateActor.State> implements HelloWithState
    {
        public static class State
        {
            String state;
        }

        @Override
        public Task<String> sayHello(final String greeting)
        {
            await(writeState());
            return Task.fromValue(greeting);
        }
    }


    @Test
    public void testActorWithoutState() throws ExecutionException, InterruptedException
    {
        createStage();
        Hello hello = Actor.getReference(Hello.class, "0");
        assertEquals("hello", hello.sayHello("hello").join());
    }

    @Test
    public void testActorWithState() throws ExecutionException, InterruptedException
    {
        createStage();
        HelloWithState hello = Actor.getReference(HelloWithState.class, "0");
        expectException(() -> hello.sayHello("hello").join());
    }

    @Override
    protected void installExtensions(final Stage stage)
    {
        super.installExtensions(stage);
        stage.getExtensions().removeAll(stage.getAllExtensions(InMemoryJSONStorageExtension.class));
    }
}
