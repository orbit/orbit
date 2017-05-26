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

package cloud.orbit.actors.streams;


import cloud.orbit.actors.runtime.ActorRuntime;
import cloud.orbit.actors.runtime.Invocation;
import cloud.orbit.actors.server.ServerPeer;
import cloud.orbit.actors.util.IdUtils;
import cloud.orbit.concurrent.Task;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.ea.async.Async.await;

public class ClientStreamProxy
{
    private Map<String, StreamSubscription> observerMap = new LinkedHashMap<>();
    ActorRuntime runtime;
    ServerPeer peer;

    static class StreamSubscription
    {
        String provider;
        Class dataClass;
        String streamId;
        boolean valid;
        Task<StreamSubscriptionHandle> handle;
    }

    @SuppressWarnings("unchecked")
    public Task<String> subscribe(String provider, Class dataClass, String streamId)
    {
        String handle = IdUtils.urlSafeString(128);
        StreamSubscription subscription = new StreamSubscription();
        subscription.provider = provider;
        subscription.dataClass = dataClass;
        subscription.streamId = streamId;
        subscription.handle = runtime.getStream(provider, dataClass, streamId).subscribe(new AsyncObserver()
        {
            @Override
            public Task<Void> onNext(final Object data, final StreamSequenceToken sequenceToken)
            {
                if (subscription.valid && peer.getPipeline().isActive())
                {
                    return peer.getPipeline().write(new Invocation());
                }
                return Task.done();
            }
        }, null);
        observerMap.put(handle, subscription);
        await(subscription.handle);
        return Task.fromValue(handle);
    }

    @SuppressWarnings("unchecked")
    public Task<Void> unsubscribe(String handle)
    {
        StreamSubscription subscription = observerMap.remove(handle);
        // no more messages
        if (subscription != null && subscription.valid)
        {
            subscription.valid = false;
            await(subscription.handle);
            await(runtime.getStream(subscription.provider, subscription.dataClass, subscription.streamId)
                    .unsubscribe(subscription.handle.join()));
            observerMap.remove(handle);
        }
        return Task.done();
    }

}
