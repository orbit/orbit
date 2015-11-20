package com.ea.orbit.actors.test.streams;


import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.streams.AsyncStream;
import com.ea.orbit.actors.test.ActorBaseTest;
import com.ea.orbit.concurrent.Task;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;

public class StreamTest extends ActorBaseTest
{
    @Test
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

    @Test
    public void test2Stages()
    {
        CompletableFuture<String> push = new Task<>();
        final Stage stage1 = createStage();
        {
            AsyncStream<String> test = AsyncStream.getStream(String.class, "test");
            test.subscribe(d -> {
                push.complete(d);
                return Task.done();
            }).join();
        }

        final Stage stage2 = createStage();
        {
            stage2.bind();
            AsyncStream<String> test2 = AsyncStream.getStream(String.class, "test");
            test2.post("hello");
        }
        assertEquals("hello", push.join());
        dumpMessages();
    }
}
