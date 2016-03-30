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

package cloud.orbit.actors.runtime;

import cloud.orbit.actors.streams.AsyncObserver;
import cloud.orbit.actors.streams.AsyncStream;
import cloud.orbit.actors.streams.StreamSequenceToken;
import cloud.orbit.actors.streams.StreamSubscriptionHandle;
import cloud.orbit.concurrent.Task;

import java.io.Serializable;

/**
 * Indirection to allow streams to be send from one node to another.
 *
 * @param <T>
 */
public class AsyncStreamReference<T> implements AsyncStream<T>, Serializable
{
    private String provider;
    private Class<T> dataClass;
    private String streamId;
    transient AsyncStream<T> actualStream;
    transient BasicRuntime runtime;

    public AsyncStreamReference()
    {

    }

    public AsyncStreamReference(final String provider, final Class<T> dataClass, final String streamId)
    {
        this.provider = provider;
        this.dataClass = dataClass;
        this.streamId = streamId;
    }

    public AsyncStreamReference(final String provider, final Class<T> dataClass, final String streamId, final AsyncStream<T> actualStream)
    {
        this.provider = provider;
        this.dataClass = dataClass;
        this.streamId = streamId;
        this.actualStream = actualStream;
    }

    public Task<Void> unsubscribe(StreamSubscriptionHandle<T> handle)
    {
        ensureStream();
        return actualStream.unsubscribe(handle);
    }


    public Task<StreamSubscriptionHandle<T>> subscribe(AsyncObserver<T> observer, StreamSequenceToken token)
    {
        ensureStream();
        return actualStream.subscribe(observer, token);
    }

    public Task<Void> publish(T data)
    {
        ensureStream();
        return actualStream.publish(data);
    }


    private void ensureStream()
    {
        if (actualStream == null)
        {
            if (runtime == null)
            {
                runtime = BasicRuntime.getRuntime();
            }
            if (runtime == null)
            {
                throw new IllegalStateException("Can't find the actor runtime");
            }
            AsyncStream<T> stream = runtime.getStream(provider, dataClass, streamId);
            if (stream instanceof AsyncStreamReference)
            {
                actualStream = ((AsyncStreamReference<T>) stream).actualStream;
            }
            else
            {
                actualStream = stream;
            }
        }
    }
}
