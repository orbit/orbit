package com.ea.orbit.actors.test.streams;


import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.client.ClientPeer;
import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.actors.streams.AsyncStream;
import com.ea.orbit.actors.streams.StreamSubscriptionHandle;
import com.ea.orbit.actors.test.ActorBaseTest;
import com.ea.orbit.concurrent.Task;

import org.junit.Test;

import java.io.Serializable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class StreamWithClientTest extends ActorBaseTest
{
    public interface Hello extends Actor
    {
        Task<Void> doPush(String streamId, final String message);

        Task<Void> doPushGeneric(final String streamId, final Object message);
    }

    public static class HelloActor extends AbstractActor implements Hello
    {
        @Override
        public Task<Void> doPush(final String streamId, final String message)
        {
            return AsyncStream.getStream(String.class, streamId)
                    .publish(message);
        }

        @Override
        public Task<Void> doPushGeneric(final String streamId, final Object message)
        {
            final Class aClass = message.getClass();
            return AsyncStream.getStream(aClass, streamId)
                    .publish(message);
        }
    }

    @Test(timeout = 30_000L)
    public void test() throws InterruptedException
    {
        final Stage stage1 = createStage();
        Hello hello = Actor.getReference(Hello.class, "0");
        hello.doPush("testStream", "hello").join();

        final ClientPeer client = createRemoteClient(stage1);

        AsyncStream<String> testStream = client.getStream(AsyncStream.DEFAULT_PROVIDER, String.class, "testStream");
        testStream.subscribe((d,t) -> {
            fakeSync.deque("received").add(d);
            return Task.done();
        }).join();


        stage1.bind();
        hello.doPush("testStream", "hello2").join();

        assertEquals("hello2", fakeSync.deque("received").poll(20, TimeUnit.SECONDS));
        dumpMessages();
    }


    @Test(timeout = 30_000L)
    public void testUnsubscribe() throws InterruptedException
    {
        final Stage stage1 = createStage();
        Hello hello = Actor.getReference(Hello.class, "0");

        // client subscribes
        logger.info("subscribing");
        final ClientPeer client = createRemoteClient(stage1);
        AsyncStream<String> testStream = client.getStream(AsyncStream.DEFAULT_PROVIDER, String.class, "testStream");
        BlockingQueue<Object> messagesReceived = fakeSync.deque("received");
        final StreamSubscriptionHandle<String> handle = testStream.subscribe((d,t) -> {
            messagesReceived.add(d);
            return Task.done();
        }).join();

        // first push
        hello.doPush("testStream", "hello").join();
        assertEquals("hello", messagesReceived.poll(20, TimeUnit.SECONDS));
        assertEquals(0, messagesReceived.size());

        // client unsubscribes
        logger.info("unsubscribing");
        testStream.unsubscribe(handle).join();
        assertEquals(0, messagesReceived.size());

        // second push
        hello.doPush("testStream", "hello2").join();
        // nothing should be sent

        // client subscribes again
        logger.info("subscribing again");
        testStream.subscribe((d,t) -> {
            messagesReceived.add(d);
            return Task.done();
        }).join();

        // another push
        hello.doPush("testStream", "hello3").join();
        assertEquals("hello3", messagesReceived.poll(10, TimeUnit.SECONDS));
        assertEquals(0, messagesReceived.size());

        dumpMessages();
    }


    public static class SomeData implements Serializable
    {
        int x;
        Object obj;

        public SomeData()
        {
            // required by the serializer
        }

        public SomeData(final int x, final Object obj)
        {
            this.x = x;
            this.obj = obj;
        }
    }

    @Test(timeout = 30_000L)
    public void testMessageClass() throws InterruptedException
    {
        final Stage stage1 = createStage();
        Hello hello = Actor.getReference(Hello.class, "0");
        hello.doPush("testStream", "hello").join();

        final ClientPeer client = createRemoteClient(stage1);

        AsyncStream<SomeData> testStream = client.getStream(AsyncStream.DEFAULT_PROVIDER, SomeData.class, "testStream");
        testStream.subscribe((d,t) -> {
            fakeSync.deque("received").add(d);
            return Task.done();
        }).join();


        stage1.bind();
        hello.doPushGeneric("testStream", new SomeData(5, null)).join();

        final SomeData received = fakeSync.<SomeData>deque("received").poll(20, TimeUnit.SECONDS);
        assertEquals(5, received.x);
        assertNull(received.obj);
        dumpMessages();
    }

    @Test(timeout = 30_000L)
    public void testMessageClassJsonTypeInfo() throws InterruptedException
    {
        final Stage stage1 = createStage();
        Hello hello = Actor.getReference(Hello.class, "0");
        hello.doPush("testStream", "hello").join();

        final ClientPeer client = createRemoteClient(stage1);

        AsyncStream<SomeData> testStream = client.getStream(AsyncStream.DEFAULT_PROVIDER, SomeData.class, "testStream");
        testStream.subscribe((d,t) -> {
            fakeSync.deque("received").add(d);
            return Task.done();
        }).join();


        stage1.bind();
        final SomeData someData = new SomeData(5, new Object[]{ new SomeData(6, null) });
        hello.doPushGeneric("testStream", someData).join();

        final SomeData received = fakeSync.<SomeData>deque("received").poll(20, TimeUnit.SECONDS);
        assertEquals(5, received.x);

        // tests the json type information.
        assertEquals(6, ((SomeData) ((Object[]) received.obj)[0]).x);
        dumpMessages();
    }
}
