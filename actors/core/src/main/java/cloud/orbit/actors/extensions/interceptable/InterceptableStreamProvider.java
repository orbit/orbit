/*
 Copyright (C) 2017 Electronic Arts Inc.  All rights reserved.

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

package cloud.orbit.actors.extensions.interceptable;

import cloud.orbit.actors.extensions.StreamProvider;
import cloud.orbit.actors.extensions.interceptor.InterceptedValue;
import cloud.orbit.actors.extensions.interceptor.StreamInterceptor;
import cloud.orbit.actors.streams.AsyncStream;

import java.util.List;

public class InterceptableStreamProvider extends InterceptableActorExtension<StreamInterceptor, StreamProvider>
        implements StreamProvider
{
    public InterceptableStreamProvider(final StreamProvider extension, final List<StreamInterceptor> interceptors)
    {
        super(extension, interceptors);
    }

    public InterceptableStreamProvider(final StreamProvider extension)
    {
        super(extension);
    }

    @Override
    public <T> AsyncStream<T> getStream(final Class<T> dataClass, final String id)
    {
        if (!hasInterceptors())
        {
            return extension.getStream(dataClass, id);
        }
        InterceptedValue<Class<T>> interceptedDataClass = InterceptedValue.of(dataClass);
        InterceptedValue<String> interceptedId = InterceptedValue.of(id);
        intercept(interceptor -> interceptor.preGetStream(interceptedDataClass, interceptedId));
        InterceptedValue<AsyncStream<T>> interceptedReturnValue =
                InterceptedValue.of(extension.getStream(interceptedDataClass.get(), interceptedId.get()));
        intercept(interceptor ->
                interceptor.postGetStream(interceptedDataClass, interceptedId, interceptedReturnValue));
        return interceptedReturnValue.get();
    }

    @Override
    public String getName()
    {
        return extension.getName();
    }
}
