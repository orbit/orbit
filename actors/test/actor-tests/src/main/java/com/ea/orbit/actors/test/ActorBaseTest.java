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


import com.ea.orbit.actors.OrbitStage;
import com.ea.orbit.actors.runtime.IReminderController;
import com.ea.orbit.concurrent.ExecutorUtils;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

@SuppressWarnings("VisibilityModifierCheck")
public class ActorBaseTest
{
    protected String clusterName = "cluster." + Math.random() + "." + getClass().getSimpleName();
    protected FakeClock clock = new FakeClock();
    protected ConcurrentHashMap fakeDatabase = new ConcurrentHashMap();
    protected static final ExecutorService commonPool = ExecutorUtils.newScalingThreadPool(200);

    public OrbitStage createClient() throws ExecutionException, InterruptedException
    {
        OrbitStage client = new OrbitStage();
        client.setMode(OrbitStage.StageMode.FRONT_END);
        client.setExecutionPool(commonPool);
        client.setMessagingPool(commonPool);
        client.setClock(clock);
        client.setClusterName(clusterName);
        client.setClusterPeer(new FakeClusterPeer());
        client.start().get();
        return client;
    }

    public OrbitStage createStage() throws ExecutionException, InterruptedException
    {
        OrbitStage stage = new OrbitStage();
        stage.setMode(OrbitStage.StageMode.HOST);
        stage.setExecutionPool(commonPool);
        stage.setMessagingPool(commonPool);
        stage.addProvider(new FakeStorageProvider(fakeDatabase));
        stage.setClock(clock);
        stage.addProvider("com.ea.orbit.*");
        stage.setClusterName(clusterName);
        stage.setClusterPeer(new FakeClusterPeer());
        stage.start().get();
        stage.getReference(IReminderController.class, "0").ensureStart();
        return stage;
    }

    @FunctionalInterface
    public interface Exceptional
    {
        @SuppressWarnings("Checkstyle:IllegalThrowsCheck")
        Object call() throws Throwable;
    }

    @SuppressWarnings("PMD.AvoidCatchingThrowable")
    public void expectException(Exceptional callable)
    {
        try
        {
            Object r = callable.call();
            if (r instanceof Future)
            {
                ((Future) r).get(60, TimeUnit.SECONDS);
            }
        }
        catch (Throwable ex)
        {
            // ok
            return;
        }
        fail("Was expecting some exception");
    }
}
