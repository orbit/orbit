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

package com.ea.orbit.async.test;

import com.ea.orbit.concurrent.Task;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.ea.orbit.async.Await.await;
import static org.junit.Assert.assertEquals;

public class FunctionCallTest extends BaseTest
{
    public static class TaskSomethingAsync
    {
        private List<CompletableFuture> blockers = new ArrayList<>();

        public Task<String> doSomething()
        {
            String res1 = await(blocker());
            return Task.fromValue(":" + res1);
        }

        private CompletableFuture<String> blocker()
        {
            final CompletableFuture<String> blocker = new CompletableFuture<>();
            blockers.add(blocker);
            return blocker;
        }

        public void completeBlockers()
        {
            for (int i = 0; i < blockers.size(); i++)
            {
                final CompletableFuture<String> fut = blockers.get(i);
                fut.complete("str " + i);
            }
        }
    }

    @Test
    public void testBlockingAndException() throws IllegalAccessException, InstantiationException
    {
        final TaskSomethingAsync a = new TaskSomethingAsync();

        Task<String> res = a.doSomething();
        a.completeBlockers();
        assertEquals(":str 0", res.join());
    }
}
