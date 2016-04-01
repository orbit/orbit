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


import cloud.orbit.actors.ActorObserver;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.cloner.KryoCloner;
import cloud.orbit.concurrent.Task;

import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertNotNull;

@SuppressWarnings("unused")
public class ObserverDownTest extends ActorBaseTest
{
    String clusterName = "cluster." + Math.random() + "." + getClass().getSimpleName();


    public interface SomeObserver extends ActorObserver
    {
        Task<Void> receiveMessage(final String message);
    }

    public static class SomeObserverImpl implements SomeObserver
    {
        public Task<Void> receiveMessage(final String message)
        {
            return Task.done();
        }
    }

    @Test
    public void deadNodeTest() throws ExecutionException, InterruptedException, TimeoutException
    {
        Stage stage1 = createStage();
        Stage stage2 = createStage();

        SomeObserverImpl observer1 = new SomeObserverImpl();
        final KryoCloner cloner = new KryoCloner();
        SomeObserver observerRef = cloner.clone(stage1.registerObserver(SomeObserver.class, observer1));

        // should respond immediately
        stage2.bind();
        observerRef.receiveMessage("bla").get(10, TimeUnit.SECONDS);

        // shutdown the stage.
        stage1.stop().join();


        // should fail immediately instead of timing out
        stage2.bind();
        final Throwable exception = observerRef.receiveMessage("bla").handle((r, e) -> e).get(10, TimeUnit.SECONDS);
        assertNotNull(exception);
    }


}
