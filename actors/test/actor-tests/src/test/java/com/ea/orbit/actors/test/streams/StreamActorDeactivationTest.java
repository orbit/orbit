package com.ea.orbit.actors.test.streams;


import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.actors.streams.AsyncStream;
import com.ea.orbit.actors.streams.StreamSubscriptionHandle;
import com.ea.orbit.actors.test.ActorBaseTest;
import com.ea.orbit.actors.test.FakeSync;
import com.ea.orbit.concurrent.Task;

import org.junit.Test;

import javax.inject.Inject;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.ea.async.Async.await;
import static org.jgroups.util.Util.assertEquals;

public class StreamActorDeactivationTest extends ActorBaseTest
{
    public interface Hello extends Actor
    {
        Task<Void> doSubscribe(String streamId);
    }

    public static class HelloActor extends AbstractActor implements Hello
    {
        @Inject
        FakeSync testSync;

        private StreamSubscriptionHandle<String> handle;


        @Override
        public Task<Void> doSubscribe(final String streamId)
        {
            getLogger().info("doSubscribe");
            handle = await(AsyncStream.getStream(String.class, streamId)
                    .subscribe((d, t) -> {
                        // test framework trick
                        testSync.deque(actorIdentity() + "-last").add(d);
                        return Task.done();
                    }));
            return Task.done();
        }
    }

    @Test(timeout = 30_000L)
    public void testDeactivate() throws InterruptedException
    {
        final Stage stage1 = createStage();
        clock.stop();
        Hello hello = Actor.getReference(Hello.class, "1");
        hello.doSubscribe("test").join();

        AsyncStream<String> test = AsyncStream.getStream(String.class, "test");
        test.publish("hello").join();
        BlockingQueue<Object> queue = fakeSync.deque("1-last");

        assertEquals("hello", queue.poll(10, TimeUnit.SECONDS));
        clock.incrementTime(20, TimeUnit.MINUTES);
        // actors get deactivated
        stage1.cleanup().join();

        // the actor has been deactivated so it won't receive this message
        test.publish("hello2").join();
        // subscribing again
        hello.doSubscribe("test").join();

        test.publish("hello3").join();
        // the message hello2 can't have being processed.
        assertEquals("hello3", queue.poll(10, TimeUnit.SECONDS));

        dumpMessages();
    }

}
