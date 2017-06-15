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

import cloud.orbit.actors.extensions.ActorExtension;
import cloud.orbit.concurrent.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.ea.async.Async.await;

public abstract class InterceptableActorExtension<I, E extends ActorExtension> implements ActorExtension
{
    private final List<I> interceptors;
    protected final E extension;

    public InterceptableActorExtension(final E extension, final List<I> interceptors)
    {
        this.interceptors = interceptors;
        this.extension = extension;
    }

    public InterceptableActorExtension(final E extension)
    {
        this(extension, new ArrayList<>());
    }

    public void addInterceptor(final I interceptor)
    {
        interceptors.add(interceptor);
    }

    public void addInterceptors(final List<I> interceptors)
    {
        this.interceptors.addAll(interceptors);
    }

    protected void intercept(final Consumer<I> function)
    {
        for (I interceptor : interceptors)
        {
            function.accept(interceptor);
        }
    }

    protected Task<Void> interceptAsync(final Function<I, Task<?>> function)
    {
        for (I interceptor : interceptors)
        {
            await(function.apply(interceptor));
        }
        return Task.done();
    }

    protected boolean hasInterceptors()
    {
        return !interceptors.isEmpty();
    }
}
