package com.ea.orbit.actors.test.streams;


import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.streams.AsyncStream;
import com.ea.orbit.actors.streams.StreamSubscriptionHandle;
import com.ea.orbit.actors.test.ActorBaseTest;
import com.ea.orbit.concurrent.Task;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StreamTest extends ActorBaseTest
{
    @SuppressWarnings("Duplicates")
    @Test(timeout = 30_000L)
    public void test()
    {
        createStage();
        CompletableFuture<String> push = new Task<>();
        AsyncStream<String> test = AsyncStream.getStream(String.class, "test");
        test.subscribe(d -> {
            push.complete(d);
            return Task.done();
        }).join();
        test.post("hello");
        assertEquals("hello", push.join());
        dumpMessages();
    }

    @Test(timeout = 30_000L)
    public void test2Stages()
    {
        CompletableFuture<String> push1 = new Task<>();
        final Stage stage1 = createStage();
        {
            AsyncStream<String> test = AsyncStream.getStream(String.class, "test");
            test.subscribe(d -> {
                push1.complete(d);
                return Task.done();
            }).join();
        }

        final Stage stage2 = createStage();
        {
            stage2.bind();
            AsyncStream<String> test2 = AsyncStream.getStream(String.class, "test");
            test2.post("hello");
        }
        assertEquals("hello", push1.join());
        dumpMessages();
    }


    @Test(timeout = 30_000L)
    public void test3Stages()
    {
        CompletableFuture<String> push1 = new Task<>();
        final Stage stage1 = createStage();
        final Stage stage2 = createStage();
        final Stage stage3 = createStage();
        {
            AsyncStream<String> test = AsyncStream.getStream(String.class, "test");
            test.subscribe(d -> {
                push1.complete(d);
                return Task.done();
            }).join();
        }
        CompletableFuture<String> push2 = new Task<>();
        {
            AsyncStream<String> test2 = AsyncStream.getStream(String.class, "test");
            test2.subscribe(d -> {
                push2.complete(d);
                return Task.done();
            }).join();
        }

        {
            stage3.bind();
            AsyncStream<String> test3 = AsyncStream.getStream(String.class, "test");
            test3.post("hello");
        }
        assertEquals("hello", push1.join());
        assertEquals("hello", push2.join());
        dumpMessages();
    }


    @Test(timeout = 30_000L)
    public void test3StagesAndGc()
    {
        CompletableFuture<String> push1 = new Task<>();
        final Stage stage1 = createStage();
        {
            AsyncStream<String> test = AsyncStream.getStream(String.class, "test");
            test.subscribe(d -> {
                push1.complete(d);
                return Task.done();
            }).join();
        }
        CompletableFuture<String> push2 = new Task<>();
        final Stage stage2 = createStage();
        {
            AsyncStream<String> test2 = AsyncStream.getStream(String.class, "test");
            test2.subscribe(d -> {
                push2.complete(d);
                return Task.done();
            }).join();
        }

        System.gc();

        final Stage stage3 = createStage();
        {
            stage3.bind();
            AsyncStream<String> test3 = AsyncStream.getStream(String.class, "test");
            test3.post("hello");
        }
        assertEquals("hello", push1.join());
        assertEquals("hello", push2.join());
        dumpMessages();
    }


    @Test(timeout = 30_000L)
    public void testUnSubscription()
    {
        CompletableFuture<String> push1 = new Task<>();
        final Stage stage1 = createStage();
        final AsyncStream<String> test = AsyncStream.getStream(String.class, "test");
        final StreamSubscriptionHandle<String> handle1 = test.subscribe(d -> {
            push1.complete(d);
            return Task.done();
        }).join();

        final CompletableFuture<String> push2 = new Task<>();
        final Stage stage2 = createStage();
        System.gc();
        final Stage stage3 = createStage();
        stage3.bind();
        final AsyncStream<String> test3 = AsyncStream.getStream(String.class, "test");

        stage2.bind();
        final AsyncStream<String> test2 = AsyncStream.getStream(String.class, "test");
        test2.subscribe(d -> {
            push2.complete(d);
            return Task.done();
        }).join();
        test.unsubscribe(handle1).join();

        test3.post("hello2").join();

        assertEquals("hello2", push2.join());
        assertFalse(push1.isDone());
        dumpMessages();
    }
}
