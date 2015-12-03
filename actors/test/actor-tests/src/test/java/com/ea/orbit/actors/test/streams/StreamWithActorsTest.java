package com.ea.orbit.actors.test.streams;


import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.actors.streams.AsyncStream;
import com.ea.orbit.actors.streams.StreamSubscriptionHandle;
import com.ea.orbit.actors.test.ActorBaseTest;
import com.ea.orbit.actors.test.FakeSync;
import com.ea.orbit.concurrent.Task;

import org.junit.Ignore;
import org.junit.Test;

import javax.inject.Inject;

import static com.ea.orbit.async.Await.await;
import static org.junit.Assert.*;

public class StreamWithActorsTest extends ActorBaseTest
{
    public interface Hello extends Actor
    {
        Task<Void> doSubscribe(String streamId);

        Task<Void> doUnSubscribe(final String streamId);
    }

    public static class HelloActor extends AbstractActor<HelloActor.State> implements Hello
    {
        @Inject
        FakeSync testSync;
        private StreamSubscriptionHandle<String> handle;

        public static class State
        {
            String last;
        }

        @Override
        public Task<Void> doSubscribe(final String streamId)
        {
            getLogger().info("doSubscribe hash:" + hashCode());
            handle = await(AsyncStream.getStream(String.class, streamId)
                    .subscribe((d,t) -> {
                        state().last = d;
                        // test framework trick
                        testSync.put(actorIdentity() + "-last", d);
                        return Task.done();
                    }));
            return Task.done();
        }


        @Override
        public Task<Void> doUnSubscribe(final String streamId)
        {
            getLogger().info("doUnSubscribe hash:" + hashCode());
            await(AsyncStream.getStream(String.class, streamId).unsubscribe(handle));
            return Task.done();
        }

        @Override
        public Task<?> activateAsync()
        {
            getLogger().info("activateAsync");
            return super.activateAsync();
        }
    }

    @Test(timeout = 30_000L)
    public void test()
    {
        createStage();
        Actor.getReference(Hello.class, "0").doSubscribe("test").join();

        AsyncStream<String> test = AsyncStream.getStream(String.class, "test");
        test.publish("hello");
        assertEquals("hello", fakeSync.task("0-last").join());
        dumpMessages();
    }

    @Test(timeout = 30_000L)
    public void test2stages()
    {
        final Stage stage1 = createStage();
        Actor.getReference(Hello.class, "0").doSubscribe("test").join();

        final Stage stage2 = createStage();
        AsyncStream<String> test = AsyncStream.getStream(String.class, "test");
        test.publish("hello");
        assertEquals("hello", fakeSync.task("0-last").join());
        dumpMessages();
    }


    @Test(timeout = 30_000L)
    public void test2stagesWithDeliveryNotification()
    {
        final Stage stage1 = createStage();
        Actor.getReference(Hello.class, "0").doSubscribe("test").join();

        final Stage stage2 = createStage();
        AsyncStream<String> test = AsyncStream.getStream(String.class, "test");
        test.publish("hello").join();
        assertTrue(fakeSync.task("0-last").isDone());
        assertEquals("hello", fakeSync.get("0-last").join());
        dumpMessages();
    }


    @Test(timeout = 30_000L)
    public void testUnSubscribe()
    {
        final Stage stage1 = createStage();
        Hello hello = Actor.getReference(Hello.class, "0");
        hello.doSubscribe("test").join();
        AsyncStream<String> test = AsyncStream.getStream(String.class, "test");
        hello.doUnSubscribe("test").join();
        assertFalse(fakeSync.task("0-last").isDone());
        test.publish("hello").join();
        assertFalse(fakeSync.task("0-last").isDone());
        dumpMessages();
    }

    @Test(timeout = 30_000L)
    public void testUnSubscribe2()
    {
        final Stage stage1 = createStage();
        Hello hello = Actor.getReference(Hello.class, "0");
        hello.doSubscribe("test").join();

        final Stage stage2 = createStage();
        AsyncStream<String> test = AsyncStream.getStream(String.class, "test");

        hello.doUnSubscribe("test").join();
        assertFalse(fakeSync.task("0-last").isDone());
        test.publish("hello").join();
        assertFalse(fakeSync.task("0-last").isDone());
        dumpMessages();
    }

    @Test(timeout = 30_000L)
    public void test2SubscriberActors()
    {
        final Stage stage1 = createStage();
        Hello hello = Actor.getReference(Hello.class, "1");
        hello.doSubscribe("test").join();

        final Stage stage2 = createStage();
        Hello hello2 = Actor.getReference(Hello.class, "2");
        hello2.doSubscribe("test").join();


        final Stage stage3 = createStage();
        AsyncStream<String> test = AsyncStream.getStream(String.class, "test");

        //stage2.stop().join();

        test.publish("hello").join();

        assertTrue(fakeSync.task("1-last").isDone());
        assertTrue(fakeSync.task("2-last").isDone());
        dumpMessages();
    }

    @Test(timeout = 30_000L)
    @Ignore
    public void testDeactivate()
    {
        // todo this test needs rewriting.

        final Stage stage1 = createStage();
        Hello hello = Actor.getReference(Hello.class, "1");
        hello.doSubscribe("test").join();

        final Stage stage2 = createStage();
        Hello hello2 = Actor.getReference(Hello.class, "2");
        hello2.doSubscribe("test").join();


        final Stage stage3 = createStage();
        AsyncStream<String> test = AsyncStream.getStream(String.class, "test");

        stage2.stop().join();

        stage3.bind();
        test.publish("hello").join();

        assertTrue(fakeSync.task("1-last").isDone());
        assertFalse(fakeSync.task("2-last").isDone());
        dumpMessages();
    }

    // TODO test actor deactivation
    // TODO test actor unSubscription
    // TODO test stage crash (forceful removal from the network)
    // TODO test stream persistence

}
