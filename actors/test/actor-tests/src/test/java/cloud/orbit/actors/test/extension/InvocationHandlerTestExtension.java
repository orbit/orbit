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

package cloud.orbit.actors.test.extension;

import cloud.orbit.actors.extensions.InvocationHandlerExtension;
import cloud.orbit.actors.runtime.Invocation;
import cloud.orbit.actors.test.actors.InvocationHandlerActorImpl;
import cloud.orbit.concurrent.Task;
import cloud.orbit.exception.UncheckedException;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

public class InvocationHandlerTestExtension implements InvocationHandlerExtension
{
    private AtomicBoolean acceptCalls = new AtomicBoolean(true);
    private LongAdder callCount = new LongAdder();

    @Override
    public Task beforeInvoke(final long startTimeNanos, final Object targetObject, final Method targetMethod, final Object[] params, final Map<?, ?> invocationHeaders)
    {
        if(!(targetObject instanceof InvocationHandlerActorImpl)) return Task.done();

        if(!acceptCalls.get()) throw new UncheckedException("Not accepting calls");

        callCount.increment();

        return Task.done();
    }

    @Override
    public Task afterInvoke(final long startTimeNanos, final Object targetObject, final Method targetMethod, final Object[] params, final Map<?, ?> invocationHeaders)
    {
        if(!(targetObject instanceof InvocationHandlerActorImpl)) return Task.done();

        if(!acceptCalls.get()) throw new UncheckedException("Not accepting calls");

        callCount.increment();

        return Task.done();
    }

    @Override
    public Task afterInvokeChain(final long startTimeNanos, final Object targetObject, final Method targetMethod, final Object[] params, final Map<?, ?> invocationHeaders)
    {
        if(!(targetObject instanceof InvocationHandlerActorImpl)) return Task.done();

        if(!acceptCalls.get()) throw new UncheckedException("Not accepting calls");

        callCount.increment();

        return Task.done();
    }

    public void resetInvocationCount() {
        callCount.reset();
    }

    public long getInvocationCount() {
        return callCount.sum();
    }

    public void setAcceptCalls(final Boolean acceptCalls) {
        this.acceptCalls.set(acceptCalls);
    }
}
