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

package cloud.orbit.actors.extensions;

import cloud.orbit.concurrent.Task;

import java.lang.reflect.Method;
import java.util.Map;

public interface InvocationHandlerExtension extends ActorExtension
{
    /**
     * Called before an invocation is performed.
     * Intended to allow generic actions to be performed before an invocation.
     * Throwing an exception will prevent the invocation from occurring and be returned to the original caller.
     *
     * @param startTimeNanos The time in nanoseconds when the task started
     * @param targetObject The object instance the invocation will be performed on
     * @param targetMethod The method that will be invokved
     * @param params The parameters for the invocation
     * @return completed void task
     */
    Task<Void> beforeInvoke(long startTimeNanos, Object targetObject, Method targetMethod, Object[] params, Map<?, ?> invocationHeaders);

    /**
     * Called after an invocation is performed.
     * Intended to allow generic actions to be performed after an invocation.
     * Throwing an exception will return an exception to the original caller.
     *
     * @param startTimeNanos The time in nanoseconds when the task started
     * @param targetObject The object instance the invocation will be performed on
     * @param targetMethod The method that will be invokved
     * @param params The parameters for the invocation
     * @return completed void task
     */
    Task<Void> afterInvoke(long startTimeNanos, Object targetObject, Method targetMethod, Object[] params, Map<?, ?> invocationHeaders);

    /**
     * Called after an invocation chain has completed.
     * Intended to allow generic actions to be performed after an invocation chain.
     * Throwing an exception will return an exception to the original caller.
     *
     * @param startTimeNanos The time in nanoseconds when the task started
     * @param targetObject The object instance the invocation will be performed on
     * @param targetMethod The method that will be invokved
     * @param params The parameters for the invocation
     * @return completed void task
     */
    Task<Void> afterInvokeChain(long startTimeNanos, Object targetObject, Method targetMethod, Object[] params, Map<?, ?> invocationHeaders);
}
