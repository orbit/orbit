/*
 Copyright (C) 2016 Electronic Arts Inc.  All rights reserved.

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
package cloud.orbit.actors.test;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.cluster.JGroupsClusterPeer;
import cloud.orbit.actors.extensions.json.InMemoryJSONStorageExtension;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.runtime.ActorProfiler;
import cloud.orbit.actors.util.IdUtils;
import cloud.orbit.concurrent.Task;
import cloud.orbit.profiler.ProfileDump;
import cloud.orbit.profiler.ProfilerData;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Fork(5)
public class SingleNodeBenchmark
{

    private Stage stage;

    // actor profiling
    private ActorProfiler actorProfiler;
    private ScheduledFuture<?> scheduledFuture;
    private ScheduledThreadPoolExecutor scheduledExecutorService;
    int iteration = 0;
    int cpuCount = Runtime.getRuntime().availableProcessors();
    boolean profile = false;

    public interface Hello extends Actor
    {
        Task<String> sayHello(String greeting);
    }

    public static class HelloActor extends AbstractActor implements Hello
    {
        @Override
        public Task<String> sayHello(final String greeting)
        {
            return Task.fromValue(greeting);
        }
    }

    @Setup
    public void setup()
    {
        System.setProperty("java.net.preferIPv4Stack", "true");

        System.out.println("Create stage");
        stage = createStage();
        if (profile)
        {
            actorProfiler = new ActorProfiler();
            scheduledExecutorService = new ScheduledThreadPoolExecutor(2);
            scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(() -> {
                try
                {
                    actorProfiler.collect();
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }, 1, 1, TimeUnit.MILLISECONDS);
        }
    }

    @Setup(Level.Iteration)
    public void setupIteration()
    {
        if (profile)
        {
            iteration++;
            if (iteration < 5)
            {
                actorProfiler.clear();
            }
        }
    }

    @TearDown
    public void tearDown()
    {
        stage.stop().join();
        if (profile)
        {
            scheduledFuture.cancel(false);
            scheduledExecutorService.shutdownNow();
            final long collectionCount = actorProfiler.getCollectionCount();
            final Map<Object, ProfilerData> profilerSnapshot = actorProfiler.getProfilerSnapshot();
            System.out.printf("collectionCount " + collectionCount);
            for (Map.Entry<Object, ProfilerData> e : profilerSnapshot.entrySet())
            {
                System.out.println("%%%%%%%%%%%%%%%%%%%%");
                System.out.println(e.getKey());
                System.out.println("%%%%%%%%%%%%%%%%%%%%");
                System.out.println(ProfileDump.textMethodInfo(e.getValue(), collectionCount));
                System.out.println("%%%%%%%%%%%%%%%%%%%%");
                System.out.println(e.getKey());
                System.out.println("%%%%%%%%%%%%%%%%%%%%");
                System.out.println(ProfileDump.textDumpCallTree(e.getValue().getCallTree(), collectionCount));
                System.out.println();
            }
        }
    }

    static final int THROUGHPUT_BENCH_BATCH_SIZE = 500;

    @Benchmark()
    @Threads(-1)
    @BenchmarkMode({ Mode.Throughput })
    @OperationsPerInvocation(THROUGHPUT_BENCH_BATCH_SIZE)
    public void requestThroughput()
    {
        // use a different actor per thread, ideally one actor per core for this test
        Hello hello = Actor.getReference(Hello.class, "hello" + Thread.currentThread().getId());
        List<Task<String>> results = new ArrayList<>(THROUGHPUT_BENCH_BATCH_SIZE);

        // doing a batch of operations reduces latency since the worker threads don't stop processing requests.
        // if just a single message were sent then join invoked
        // we'd be measuring context switching more than anything else
        for (int i = 0; i < THROUGHPUT_BENCH_BATCH_SIZE; i++)
        {
            results.add(hello.sayHello("test"));
        }
        Task<Void> result = Task.allOf(results);
        result.join();
    }

    @Benchmark()
    @BenchmarkMode(Mode.AverageTime)
    // just one thread
    @Threads(1)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void avgRequestTime_singleThread()
    {

        // using a single actor minimizes the context switching between actor threads.
        Hello hello = Actor.getReference(Hello.class, "hello");
        Task<String> result = hello.sayHello("test");
        if (cpuCount > 2)
        {
            // this takes one cpu but reduces latency by reducing context switching
            while (!result.isDone())
            {

            }
        }
        result.join();
    }


    static final int REQ_TIME_BENCH_BATCH_SIZE = 500;

    @Benchmark()
    @BenchmarkMode(Mode.AverageTime)
    // just one thread
    @Threads(1)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @OperationsPerInvocation(REQ_TIME_BENCH_BATCH_SIZE)
    public void avgRequestTime_batched()
    {

        // using a single actor minimizes the context switching between actor threads.
        Hello hello = Actor.getReference(Hello.class, "hello");
        List<Task<String>> results = new ArrayList<>(REQ_TIME_BENCH_BATCH_SIZE);

        // doing a batch of operations reduces latency since the worker threads don't stop processing requests.
        // context switching plays a big role in the result when a single message is sent.
        for (int i = 0; i < REQ_TIME_BENCH_BATCH_SIZE; i++)
        {
            results.add(hello.sayHello("test"));
        }
        Task<Void> result = Task.allOf(results);
        result.join();
    }

    private Stage createStage()
    {
        ConcurrentHashMap<Object, Object> fakeDatabase = new ConcurrentHashMap<>();

        // decreases context switching
        final int defaultPoolSize = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);

        Stage stage = new Stage.Builder()
                .extensions(new InMemoryJSONStorageExtension(fakeDatabase))
                .mode(Stage.StageMode.HOST)
                .clusterName(IdUtils.urlSafeString(32))
                .enableMessageLoopback(false)
                .build();

        stage.setExecutionPoolSize(defaultPoolSize);

        stage.start().join();

        stage.bind();
        return stage;
    }


}
