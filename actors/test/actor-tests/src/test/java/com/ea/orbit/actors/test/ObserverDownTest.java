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


import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.ActorObserver;
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.concurrent.Task;

import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.ea.orbit.async.Await.await;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("unused")
public class ObserverDownTest extends ActorBaseTest
{
    String clusterName = "cluster." + Math.random() + "." + getClass().getSimpleName();

    public interface Hello extends Actor
    {
        Task<Void> setObserver(SomeObserver observer);

        Task<String> sayHello(String greeting);
    }

    public static class HelloActor extends AbstractActor implements Hello
    {
        SomeObserver observer;

        @Override
        public Task<Void> setObserver(final SomeObserver observer)
        {
            this.observer = observer;
            return Task.done();
        }

        @Override
        public Task<String> sayHello(final String greeting)
        {
            if (observer != null)
            {
                await(observer.receiveMessage(greeting));
            }
            return Task.fromValue(greeting);
        }
    }

    public interface SomeObserver extends ActorObserver
    {
        Task<Void> receiveMessage(final String message);
    }

    public static class SomeObserverImpl implements SomeObserver
    {
        BlockingQueue<String> messagesReceived = new LinkedBlockingQueue<>();

        public Task<Void> receiveMessage(final String message)
        {
            messagesReceived.add(message);
            return Task.done();
        }
    }

    @Test
    @Ignore
    public void deadNodeTest() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        Hello hello = Actor.getReference(Hello.class, "0");
        hello.sayHello("hi 1");
        SomeObserverImpl observer1 = new SomeObserverImpl();
        Stage stage2 = createStage();
        SomeObserver observerRef = stage2.registerObserver(SomeObserver.class, observer1);
        hello.setObserver(observerRef).join();
        hello.sayHello("hi 2");
        assertEquals("hi 2", observer1.messagesReceived.poll(10, TimeUnit.SECONDS));
        stage2.stop().join();
        stage1.bind();
        Throwable bu = expectException(() -> hello.sayHello("bu").join());
        System.out.println(bu);
        bu.printStackTrace();
    }


}
