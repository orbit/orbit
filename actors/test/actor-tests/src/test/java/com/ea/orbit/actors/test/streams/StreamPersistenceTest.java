package com.ea.orbit.actors.test.streams;


import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.annotation.PreferLocalPlacement;
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

public class StreamPersistenceTest extends ActorBaseTest
{
    @PreferLocalPlacement
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
                        testSync.deque("queue").add(d);
                        return Task.done();
                    }));
            return Task.done();
        }
    }


    @Test(timeout = 30_000L)
    public void testStreamPersistence() throws InterruptedException
    {
        final Stage stage1 = createStage();
        AsyncStream<String> test = AsyncStream.getStream(String.class, "test");
        // this will force the stream to be activated in stage 1
        test.publish("hello").join();

        final Stage stage2 = createStage();
        // since hello is @PreferLocalPlacement it will be activated in stage2
        Hello hello2 = Actor.getReference(Hello.class, "2");
        hello2.doSubscribe("test").join();


        BlockingQueue<Object> queue = fakeSync.deque("queue");
        test.publish("hello2");
        // stream in stage 1, actor in stage 2
        assertEquals("hello2", queue.poll(10, TimeUnit.SECONDS));

        // now stopping stage1 to force the stream deactivation
        // the actor will remain alive in stage 2
        stage1.stop().join();

        // the stream will be activated again in stage 2
        test.publish("hello3");
        assertEquals("hello3", queue.poll(10, TimeUnit.SECONDS));


        dumpMessages();
    }

}
