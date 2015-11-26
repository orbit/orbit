package com.ea.orbit.actors;

import com.ea.orbit.actors.concurrent.WaitFreeExecutionSerializer;
import com.ea.orbit.actors.concurrent.WaitFreeMultiExecutionSerializer;
import com.ea.orbit.concurrent.Task;

import org.junit.Test;

import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

import static org.jgroups.util.Util.assertEquals;

public class ExecutionSerializerTest
{
    @Test
    public void test()
    {
        WaitFreeExecutionSerializer executionSerializer = new WaitFreeExecutionSerializer(ForkJoinPool.commonPool());
        Task<String> hello = executionSerializer.executeSerialized(() -> Task.fromValue("hello"), 1000);
        assertEquals("hello", hello.join());
    }

    @Test(expected = RuntimeException.class)
    public void testException()
    {
        WaitFreeExecutionSerializer executionSerializer = new WaitFreeExecutionSerializer(ForkJoinPool.commonPool());
        Task<String> hello = executionSerializer.executeSerialized(() -> {
            throw new RuntimeException("Hello Exception");
        }, 1000);
        hello.join();
    }

    @Test
    public void testMulti()
    {
        WaitFreeMultiExecutionSerializer<String> executionSerializer = new WaitFreeMultiExecutionSerializer<>(ForkJoinPool.commonPool());
        Supplier<Task<String>> hello1 = () -> Task.fromValue("hello");
        Task<String> hello = executionSerializer.offerJob("aa", hello1, 1000);
        assertEquals("hello", hello.join());
    }

    @Test(expected = RuntimeException.class)
    public void testMultiException()
    {
        WaitFreeMultiExecutionSerializer<String> executionSerializer = new WaitFreeMultiExecutionSerializer<>(ForkJoinPool.commonPool());
        Supplier<Task<String>> taskSupplier = () -> {
            throw new RuntimeException("Hello Exception");
        };
        Task<String> hello = executionSerializer.offerJob("aa", taskSupplier, 1000);
        hello.join();
    }


}
