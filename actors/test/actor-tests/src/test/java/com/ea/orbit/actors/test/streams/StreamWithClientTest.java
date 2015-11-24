package com.ea.orbit.actors.test.streams;


import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.client.ClientPeer;
import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.actors.streams.AsyncStream;
import com.ea.orbit.actors.test.ActorBaseTest;
import com.ea.orbit.concurrent.Task;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class StreamWithClientTest extends ActorBaseTest
{
    public interface Hello extends Actor
    {
        Task<Void> doPush(String streamId, final String message);

    }

    public static class HelloActor extends AbstractActor implements Hello
    {
        @Override
        public Task<Void> doPush(final String streamId, final String message)
        {
            return AsyncStream.getStream(String.class, streamId)
                    .post(message);
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
        testStream.subscribe(d -> {
            fakeSync.deque("received").add(d);
            return Task.done();
        }).join();


        stage1.bind();
        hello.doPush("testStream", "hello2").join();

        assertEquals("hello2", fakeSync.deque("received").poll(10, TimeUnit.SECONDS));
        dumpMessages();
    }
}
