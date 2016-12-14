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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.orbit.concurrent.Task;

import cloud.orbit.exception.MethodNotFoundException;

import java.lang.reflect.Method;

public abstract class ObjectInvoker<T>
{
    private static final Logger logger = LoggerFactory.getLogger(ObjectInvoker.class);

    public Task<?> invoke(T target, int methodId, Object[] params)
    {
        throw new MethodNotFoundException(target + " MethodId :" + methodId);
    }

    /**
     * Safely invokes a method, no exceptions ever thrown, and the returned Task is always non null.
     *
     * @param target   the target actor or observer implementation
     * @param methodId the generated methodId
     * @param params   array with the method parameters
     * @return a non null task.
     */
    @SuppressWarnings("PMD.AvoidCatchingThrowable")
    public final Task<?> safeInvoke(T target, int methodId, Object[] params)
    {
        try
        {
            final Task<?> task = invoke(target, methodId, params);
            return task != null ? task : Task.done();
        }
        catch (Throwable ex)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Failed invoking task", ex);
            }
            return Task.fromException(ex);
        }
    }

    public Method getMethod(final int methodId)
    {
        throw new MethodNotFoundException("MethodId :" + methodId);
    }

    public abstract Class<T> getInterface();

}
