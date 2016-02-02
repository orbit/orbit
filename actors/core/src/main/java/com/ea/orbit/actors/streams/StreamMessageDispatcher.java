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

package com.ea.orbit.actors.streams;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dispatches invocations to matching {@link StreamMessageHandler}.
 *
 * @author Johno Crawford (johno@sulake.com)
 */
public class StreamMessageDispatcher
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(StreamMessageDispatcher.class);

    private final class HandlerMethodInvoker
    {
        private final Object handler;
        private final Method method;
        private final Class<?> messageType;
        private final boolean takesSequenceToken;

        private HandlerMethodInvoker(Object handler, Method method)
        {
            this.handler = handler;
            this.method = method;

            if (!method.getReturnType().equals(Void.TYPE))
            {
                throw new IllegalArgumentException(method.toGenericString() + ": must be void");
            }

            if (method.getParameterTypes().length == 1)
            {
                takesSequenceToken = false;
                messageType = method.getParameterTypes()[0];
            }
            else if (method.getParameterTypes().length == 2) {
                takesSequenceToken = true;
                messageType = method.getParameterTypes()[0];
                if (!method.getParameterTypes()[1].isAssignableFrom(StreamSequenceToken.class)) {
                    throw new IllegalArgumentException(method.toGenericString() + ": second parameter must be subclass of "
                            + StreamSequenceToken.class.getName());
                }
            }
            else {
                throw new IllegalArgumentException(method.toGenericString() + ": must take 1 or 2 parameters");
            }
        }

        private void invokeHandlingMethod(Object message, StreamSequenceToken sequenceToken)
        {
            Throwable error;
            try
            {
                if (takesSequenceToken) {
                    method.invoke(handler, message, sequenceToken);
                }
                else {
                    method.invoke(handler, message);
                }
                return;
            }
            catch (IllegalArgumentException | IllegalAccessException e)
            {
                error = e;
            }
            catch (InvocationTargetException e)
            {
                error = e.getCause() != null ? e.getCause() : e;
            }
            logger.error("Unexpected exception handling " + message, error);
        }

    }

    private final Map<Class<?>, List<HandlerMethodInvoker>> invokersByMessageClass = new HashMap<>();

    public StreamMessageDispatcher(final Collection<?> handlers)
    {
        if (handlers == null)
        {
            throw new IllegalStateException("Handlers must be specified");
        }
        for (Object handler : handlers)
        {
            if (!addHandler(handler))
            {
                throw new IllegalArgumentException("No idea how to dispatch requests to " + handler);
            }
        }
    }

    /**
     * Parses add adds given handler to internal mapping. Sub-classes CAN
     * override this to add support for custom mappings, but MUST always call
     * super implementation and MUST return {@code false} only if handler
     * type is not supported both by sub-class and super-class.
     *
     * @param handler handler object
     * @return {@code true} if handler has been successfully added to
     * internal mappings; {@code false} if handler type is not
     * supported
     */
    protected boolean addHandler(Object handler)
    {
        boolean supported = false;

        for (Method method : handler.getClass().getMethods())
        {
            if (!method.isAnnotationPresent(StreamMessageHandler.class))
            {
                continue;
            }

            HandlerMethodInvoker invoker = new HandlerMethodInvoker(handler, method);
            List<HandlerMethodInvoker> invokersForMessageType = invokersByMessageClass.get(invoker.messageType);
            if (invokersForMessageType == null)
            {
                invokersForMessageType = new ArrayList<>();
                invokersByMessageClass.put(invoker.messageType, invokersForMessageType);
            }
            invokersForMessageType.add(invoker);

            supported = true;
        }

        return supported;
    }

    /**
     * Dispatches message to handlers that are interested in it.
     *
     * @param message message to dispatch
     * @param sequenceToken optional stream sequence token
     * @return {@code true} if message was dispatched; {@code false}
     * if there are no handlers for it
     */
    public boolean dispatchMessage(Object message, StreamSequenceToken sequenceToken)
    {
        Collection<HandlerMethodInvoker> invokers = invokersByMessageClass.get(message.getClass());
        if (invokers == null || invokers.isEmpty())
        {
            return false;
        }

        for (HandlerMethodInvoker invoker : invokers)
        {
            invoker.invokeHandlingMethod(message, sequenceToken);
        }
        return true;
    }

    public void dispose()
    {
        invokersByMessageClass.clear();
    }
}
