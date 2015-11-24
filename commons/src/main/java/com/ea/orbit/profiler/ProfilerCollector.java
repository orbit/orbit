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
package com.ea.orbit.profiler;

import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * This is a mini execution profiler that can seat in a server
 * collecting profile samples when triggered externally.
 * <p/>
 * It can be used to have online profile information about the worst performance bottlenecks.
 * <p/>
 * The profile collector is not thread safe.
 *
 * @author Daniel Sperry
 */
public class ProfilerCollector
{
    private Map<Object, ProfilerData> collectedData = new WeakHashMap<>();

    public ProfilerCollector()
    {
    }

    public void collect()
    {
        collect(Thread.getAllStackTraces().keySet());
    }

    public void collect(Collection<Thread> threads)
    {
        final Thread currentThread = Thread.currentThread();

        for (final Thread thread : threads)
        {
            if (currentThread != thread)
            {
                // lets the application decide what to collect and event o edit the stack trace
                // gets the stack trace again to minimize drift
                collect(thread);
            }
        }
    }

    public void collect(Thread thread)
    {
        // lets the application decide what to collect and event o edit the stack trace
        // gets the stack trace again to minimize drift
        collect(thread, thread.getStackTrace());
    }

    /**
     * Override this method for collecting on other criteria
     */
    public void collect(Thread thread, final StackTraceElement[] stackTrace)
    {
        // default implementation collects information for all threads.
        collectByKey(thread, stackTrace);
        collectByKey("global", stackTrace);
    }

    public void collectByKey(final Object key, final StackTraceElement[] stackTrace)
    {
        ProfilerData profilerData = collectedData.get(key);
        if (profilerData == null)
        {
            profilerData = new ProfilerData();
            collectedData.put(key, profilerData);
        }
        profilerData.collect(stackTrace);
    }

    public Map<Object, ProfilerData> getProfilerData()
    {
        return collectedData;
    }

}
