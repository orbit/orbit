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
import com.ea.orbit.actors.test.actors.ISomeChatObserver;
import com.ea.orbit.actors.test.actors.SomeChatRoom;
import com.ea.orbit.concurrent.Task;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@SuppressWarnings("unused")
public class ObserverTest extends ActorBaseTest
{
    String clusterName = "cluster." + Math.random() + "." + getClass().getSimpleName();

    public static class SomeChatObserver implements ISomeChatObserver
    {
        BlockingQueue<Pair<ISomeChatObserver, String>> messagesReceived = new LinkedBlockingQueue<>();

        @Override
        public Task<Void> receiveMessage(final ISomeChatObserver sender, final String message)
        {
            messagesReceived.add(Pair.of(sender, message));
            return Task.done();
        }
    }


    @Test
    public void avoidReinsertionTest() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        SomeChatObserver observer = new SomeChatObserver();
        ISomeChatObserver ref1 = stage1.getObserverReference(ISomeChatObserver.class, observer);
        assertNotNull(ref1);
        ISomeChatObserver ref2 = stage1.getObserverReference(ISomeChatObserver.class, observer);
        assertNotNull(ref2);
        assertSame(ref1, ref2);
    }

    @Test
    public void basicObserverTest() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        SomeChatRoom chatRoom = IActor.getReference(SomeChatRoom.class, "1");
        SomeChatObserver observer = new SomeChatObserver();
        final ISomeChatObserver observerReference = stage1.getObserverReference(ISomeChatObserver.class, observer);
        assertNotNull(observerReference);
        chatRoom.join(observerReference).get();
        chatRoom.sendMessage(observerReference, "bla").get();
        Pair<ISomeChatObserver, String> m = observer.messagesReceived.poll(5, TimeUnit.SECONDS);
        assertNotNull(m);
        assertEquals("bla", m.getRight());
    }

    @Test
    public void observerSerializationTest() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        SomeChatRoom chatRoom = IActor.getReference(SomeChatRoom.class, "1");
        SomeChatObserver observer = new SomeChatObserver();
        chatRoom.join(observer).get();
        chatRoom.sendMessage(observer, "bla").get();
        Pair<ISomeChatObserver, String> m = observer.messagesReceived.poll(5, TimeUnit.SECONDS);
        assertNotNull(m);
        assertEquals("bla", m.getRight());
    }

    @Test
    public void twoObserversTest() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        Stage stage2 = createStage();

        SomeChatObserver observer1 = new SomeChatObserver();
        final ISomeChatObserver observerReference1 = stage1.getObserverReference(ISomeChatObserver.class, observer1);

        SomeChatObserver observer2 = new SomeChatObserver();
        final ISomeChatObserver observerReference2 = stage2.getObserverReference(ISomeChatObserver.class, observer2);


        SomeChatRoom chatRoom = IActor.getReference(SomeChatRoom.class, "1");

        stage1.bind();
        chatRoom.join(observerReference1).join();
        chatRoom.join(observerReference2).join();
        chatRoom.sendMessage(observerReference1, "bla").join();

        Pair<ISomeChatObserver, String> m = observer1.messagesReceived.poll(5, TimeUnit.SECONDS);
        assertNotNull(m);
        assertEquals("bla", m.getRight());

        Pair<ISomeChatObserver, String> m2 = observer2.messagesReceived.poll(5, TimeUnit.SECONDS);
        assertNotNull(m2);
        assertEquals("bla", m2.getRight());
    }


    @Test
    public void twoObserversNoRefTest() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();
        Stage stage2 = createStage();

        SomeChatObserver observer1 = new SomeChatObserver();
        SomeChatObserver observer2 = new SomeChatObserver();

        SomeChatRoom chatRoom_s1 = IActor.getReference(SomeChatRoom.class, "1");
        SomeChatRoom chatRoom_s2 = IActor.getReference(SomeChatRoom.class, "1");

        stage1.bind();
        chatRoom_s1.join(observer1).join();
        stage2.bind();
        chatRoom_s2.join(observer2).join();
        stage1.bind();
        chatRoom_s1.sendMessage(observer1, "bla").join();

        Pair<ISomeChatObserver, String> m = observer1.messagesReceived.poll(5, TimeUnit.SECONDS);
        assertNotNull(m);
        assertEquals("bla", m.getRight());

        Pair<ISomeChatObserver, String> m2 = observer2.messagesReceived.poll(5, TimeUnit.SECONDS);
        assertNotNull(m2);
        assertEquals("bla", m2.getRight());
    }

    @Test
    public void observerGarbageCollection() throws ExecutionException, InterruptedException
    {
        Stage stage1 = createStage();

        SomeChatObserver observer1 = new SomeChatObserver();
        final ISomeChatObserver ref1 = stage1.getObserverReference(observer1);

        ref1.receiveMessage(null, "hello").join();
        observer1 = null;
        System.gc();
        // this fails because the garbage collector will have disposed of the observer

        expectException(() -> ref1.receiveMessage(null, "hello").join());
    }

}
