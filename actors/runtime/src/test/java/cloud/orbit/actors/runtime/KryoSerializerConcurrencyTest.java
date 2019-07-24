/*
 Copyright (C) 2019 Electronic Arts Inc.  All rights reserved.

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

package cloud.orbit.actors.runtime;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KryoSerializerConcurrencyTest
{
    @Test
    public void testConcurrentDeserializations() throws Exception
    {
        KryoSerializer kryoSerializer = new KryoSerializer();
        BasicRuntime basicRuntime = Mockito.mock(BasicRuntime.class);
        ExecutorService executorService = Executors.newFixedThreadPool(16);
        byte[] bytes = kryoSerializer.serializeMessage(basicRuntime, new Message().withPayload(new MyDTO()));
        Throwable[] t = new Throwable[]{null};
        CountDownLatch latch = new CountDownLatch(160);
        for (int i = 0; i < 160; i++)
        {
            executorService.execute(() -> {
                try
                {
                    kryoSerializer.deserializeMessage(basicRuntime, bytes);
                }
                catch (Exception e)
                {
                    t[0] = e;
                    e.printStackTrace();
                }
                latch.countDown();
            });
        }
        latch.await();
        Assert.assertNull(t[0]);
    }

    public class MyDTO
    {
        int a;
    }
}
