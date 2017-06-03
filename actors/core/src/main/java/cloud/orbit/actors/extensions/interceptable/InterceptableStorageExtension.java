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

import cloud.orbit.actors.extensions.StorageExtension;
import cloud.orbit.actors.extensions.interceptor.InterceptedValue;
import cloud.orbit.actors.extensions.interceptor.StorageInterceptor;
import cloud.orbit.actors.runtime.RemoteReference;
import cloud.orbit.concurrent.Task;

import java.util.List;

import static com.ea.async.Async.await;

public class InterceptableStorageExtension extends InterceptableActorExtension<StorageInterceptor, StorageExtension>
        implements StorageExtension
{
    public InterceptableStorageExtension(final StorageExtension extension, final List<StorageInterceptor> interceptors)
    {
        super(extension, interceptors);
    }

    public InterceptableStorageExtension(final StorageExtension extension)
    {
        super(extension);
    }

    @Override
    public String getName()
    {
        return extension.getName();
    }

    @Override
    public Task<Void> clearState(final RemoteReference<?> reference, final Object state)
    {
        if (!hasInterceptors())
        {
            return extension.clearState(reference, state);
        }
        InterceptedValue<RemoteReference<?>> interceptedReference = InterceptedValue.of(reference);
        InterceptedValue interceptedState = InterceptedValue.of(state);
        await(interceptAsync(interceptor -> interceptor.preClearState(interceptedReference, interceptedState)));
        await(extension.clearState(interceptedReference.get(), interceptedState.get()));
        return interceptAsync(interceptor -> interceptor.postClearState(interceptedReference, interceptedState));
    }

    @Override
    public Task<Boolean> readState(final RemoteReference<?> reference, final Object state)
    {
        if (!hasInterceptors())
        {
            return extension.readState(reference, state);
        }
        InterceptedValue<RemoteReference<?>> interceptedReference = InterceptedValue.of(reference);
        InterceptedValue interceptedState = InterceptedValue.of(state);
        await(interceptAsync(interceptor -> interceptor.preReadState(interceptedReference, interceptedState)));
        InterceptedValue<Boolean> interceptedReturnValue =
                InterceptedValue.of(await(extension.readState(reference, state)));
        await(interceptAsync(interceptor ->
                interceptor.postReadState(interceptedReference, interceptedReference, interceptedReturnValue)));
        return Task.fromValue(interceptedReturnValue.get());
    }

    @Override
    public Task<Void> writeState(final RemoteReference<?> reference, final Object state)
    {
        if (!hasInterceptors())
        {
            return extension.writeState(reference, state);
        }
        InterceptedValue<RemoteReference<?>> interceptedReference = InterceptedValue.of(reference);
        InterceptedValue interceptedState = InterceptedValue.of(state);
        await(interceptAsync(interceptor -> interceptor.preWriteState(interceptedReference, interceptedState)));
        await(extension.writeState(reference, state));
        await(interceptAsync(interceptor -> interceptor.postWriteState(interceptedReference, interceptedState)));
        return Task.done();
    }
}
