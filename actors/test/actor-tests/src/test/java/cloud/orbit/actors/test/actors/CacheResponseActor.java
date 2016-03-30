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

import cloud.orbit.actors.cache.ExecutionCacheFlushManager;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.test.dto.TestDto1;
import cloud.orbit.concurrent.Task;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("rawtypes")
public class CacheResponseActor extends AbstractActor implements CacheResponse
{
    public static int accessCount = 0;
    private Map<Integer, Long> indexTally = new HashMap<>();
    private TestDto1 testDto1;

    public Task<Long> getNow(String greeting)
    {
        accessCount++;
        return Task.fromValue(System.nanoTime());
    }

    public Task<Long> getIndexTally(int id)
    {
        long tally = indexTally.getOrDefault(id, (long) 0);
        indexTally.put(id, ++tally);
        return Task.fromValue(tally);
    }

    public Task<Void> setDto1(TestDto1 dto1)
    {
        testDto1 = dto1;
        return Task.done();
    }

    public Task<TestDto1> getDto1()
    {
        return Task.fromValue(testDto1);
    }

    public Task<Void> flush()
    {
        return ExecutionCacheFlushManager.flushAll(this);
    }
}
