/*
 Copyright (C) 2015 Electronic Arts Inc.  All rights reserved.

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
package com.ea.orbit.actors.test;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.extensions.LifetimeExtension;
import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.actors.transactions.IdUtils;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.injection.DependencyRegistry;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


@State(Scope.Benchmark)
@Fork(5)
public class SingleNodeBenchmark
{

    private Stage stage;

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
        System.out.println("Create stage");
        stage = createStage();
    }

    @TearDown
    public void tearDown()
    {
        stage.stop().join();
    }

    @Benchmark()
    // should use as many threads as possible, or issue concurrent requests
    @Threads(100)
    @BenchmarkMode(Mode.Throughput)
    public void requestThroughput()
    {
        // todo keep a list of tasks and issue x concurrent requests
        // use a different actor per thread
        Hello hello = Actor.getReference(Hello.class, "hello" + Thread.currentThread().getId());
        hello.sayHello("test").join();
    }

    @Benchmark()
    @BenchmarkMode(Mode.AverageTime)
    // just one thread to really measure the best possible latency time
    @Threads(1)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void requestLatency()
    {
        Hello hello = Actor.getReference(Hello.class, "hello" + Thread.currentThread().getId());
        hello.sayHello("test").join();
    }

    public Stage createStage()
    {
        ConcurrentHashMap<Object, Object> fakeDatabase = new ConcurrentHashMap<>();

        DependencyRegistry dr = new DependencyRegistry();

        LifetimeExtension lifetimeExtension = new LifetimeExtension()
        {
            @Override
            public Task<?> preActivation(final AbstractActor<?> actor)
            {
                dr.inject(actor);
                return Task.done();
            }
        };

        Stage stage = new Stage.Builder()
                .extensions(lifetimeExtension, new FakeStorageExtension(fakeDatabase))
                .mode(Stage.StageMode.HOST)
                .clusterName(IdUtils.urlSafeString(32))
                // uncomment this to remove jgroups from the equation
                //.clusterPeer(new FakeClusterPeer())
                .build();

        dr.addSingleton(Stage.class, stage);

        stage.start().join();

        stage.bind();
        return stage;
    }


}
