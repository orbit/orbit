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

package cloud.orbit.actors.test.actors;

import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.concurrent.Task;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("rawtypes")
public class StatelessThingActor extends AbstractActor implements StatelessThing
{
    private UUID uuid = UUID.randomUUID();
    static AtomicInteger activationCount = new AtomicInteger();
    private String activationId = String.valueOf(activationCount.incrementAndGet());


    @Override
    public Task<String> sayHello()
    {
        Thread.yield();
        return Task.fromValue("hello from " + getIdentity() + "/" + activationId);
    }

    @Override
    public Task<UUID> getUniqueActivationId()
    {
        // just to ensure some parallelism;
        Thread.yield();
        return Task.fromValue(uuid);
    }

    @Override
    public Task<UUID> getUniqueActivationId(final long sleepNanos)
    {
        // just to ensure some parallelism;
        Thread.yield();
        long start = System.nanoTime();
        if (sleepNanos >= 1000)
        {
            try
            {
                Thread.sleep(sleepNanos / 1000);
            }
            catch (InterruptedException e)
            {
                getLogger().error("Error sleeping", e);
            }
        }
        while (start + sleepNanos >= System.nanoTime())
        {
            // do nothing, waiting.
        }
        return Task.fromValue(uuid);
    }
}
