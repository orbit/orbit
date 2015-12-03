package com.ea.orbit.actors.test.streams;


import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.client.ClientPeer;
import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.actors.streams.AsyncStream;
import com.ea.orbit.actors.test.ActorBaseTest;
import com.ea.orbit.concurrent.Task;

import org.junit.Test;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;

import static com.ea.orbit.async.Await.await;
import static org.junit.Assert.assertEquals;

public class StreamReferenceTest extends ActorBaseTest
{
    public interface Hello extends Actor
    {
        Task<AsyncStream<String>> getStreamReference();

        Task<Void> sayHello(final String greeting);
    }

    public static class HelloActor extends AbstractActor implements Hello
    {
        @Override
        public Task<AsyncStream<String>> getStreamReference()
        {
            return Task.fromValue(AsyncStream.getStream(String.class, "hello:" + actorIdentity()));
        }

        @Override
        public Task<Void> sayHello(final String greeting)
        {
            return await(getStreamReference()).publish(greeting);
        }
    }

    @Test(timeout = 30_000L)
    public void testStreamReferenceWithRemoteClient() throws InterruptedException
    {
        final Stage stage1 = createStage();
        Hello hello = Actor.getReference(Hello.class, "0");


        final ClientPeer client = createRemoteClient(stage1);

        AsyncStream<String> testStream = hello.getStreamReference().join();
        BlockingDeque<Object> received = fakeSync.deque("received");

        testStream.subscribe((d, t) -> {
            received.add(d);
            return Task.done();
        }).join();

        stage1.bind();
        hello.sayHello("hello2").join();

        assertEquals("hello2", received.poll(20, TimeUnit.SECONDS));
        dumpMessages();
    }

    @Test(timeout = 30_000L)
    public void testStreamReferenceWithRemoteStage() throws InterruptedException
    {
        final Stage stage1 = createStage();
        Hello hello1 = Actor.getReference(Hello.class, "0");
        // ensure the actor gets instantiated in the first stage
        hello1.sayHello("hello1");

        final Stage stage2 = createStage();
        Hello hello2 = Actor.getReference(Hello.class, "0");


        BlockingDeque<Object> received = fakeSync.deque("received");

        AsyncStream<String> testStream = hello2.getStreamReference().join();

        testStream.subscribe((d, t) -> {
            received.add(d);
            return Task.done();
        }).join();


        stage1.bind();
        hello1.sayHello("hello2").join();

        assertEquals("hello2", received.poll(20, TimeUnit.SECONDS));
        dumpMessages();
    }
}
