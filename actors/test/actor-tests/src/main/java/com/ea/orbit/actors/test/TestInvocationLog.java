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

package com.ea.orbit.actors.test;

import com.ea.orbit.actors.extensions.PipelineExtension;
import com.ea.orbit.actors.net.HandlerContext;
import com.ea.orbit.actors.peer.PeerExtension;
import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.actors.runtime.ActorTaskContext;
import com.ea.orbit.actors.runtime.DefaultDescriptorFactory;
import com.ea.orbit.actors.runtime.DefaultHandlers;
import com.ea.orbit.actors.runtime.Invocation;
import com.ea.orbit.actors.runtime.NodeCapabilities;
import com.ea.orbit.actors.runtime.ReminderController;
import com.ea.orbit.actors.runtime.RemoteReference;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.concurrent.TaskContext;

import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.ea.orbit.actors.test.TestLogger.wrap;


class TestInvocationLog implements PipelineExtension, PeerExtension
{
    private AtomicLong invocationId = new AtomicLong();

    private ActorBaseTest actorBaseTest;

    public TestInvocationLog(final ActorBaseTest actorBaseTest)
    {
        this.actorBaseTest = actorBaseTest;
    }

    @Override
    public String getName()
    {
        return "test-invocation-logging";
    }

    @Override
    public String afterHandlerName()
    {
        return DefaultHandlers.HEAD;
    }

    String toString(Object obj)
    {
        if (obj instanceof String)
        {
            return (String) obj;
        }
        if (obj instanceof AbstractActor)
        {
            final RemoteReference ref = RemoteReference.from((AbstractActor) obj);
            return RemoteReference.getInterfaceClass(ref).getSimpleName() + ":" +
                    RemoteReference.getId(ref);
        }
        if (obj instanceof RemoteReference)
        {
            return RemoteReference.getInterfaceClass((RemoteReference<?>) obj).getSimpleName() + ":" +
                    RemoteReference.getId((RemoteReference<?>) obj);
        }
        return String.valueOf(obj);
    }

    @Override
    public Task<?> write(HandlerContext ctx, Object message)
    {
        if (!(message instanceof Invocation))
        {
            return ctx.write(message);
        }
        final Invocation invocation = (Invocation) message;
        long id = invocationId.incrementAndGet();
        final RemoteReference toReference = invocation.getToReference();
        if (toReference instanceof NodeCapabilities)
        {
            return ctx.write(message);
        }
        Method method = invocation.getMethod();
        if (method == null)
        {
            method = DefaultDescriptorFactory.get().getInvoker(RemoteReference.getInterfaceClass(toReference)).getMethod(invocation.getMethodId());
        }
        final String methodName = method.getName();
        if (toReference instanceof ReminderController && "ensureStart".equals(methodName))
        {
            return ctx.write(message);
        }
        String from = wrap(getFrom(toReference, method), 32, "\\n", false);
        String to = wrap(RemoteReference.getInterfaceClass(toReference).getSimpleName()
                + ":"
                + RemoteReference.getId(toReference), 32, "\\n", false);

        String strParams;
        final Object[] params = invocation.getParams();
        if (params != null && params.length > 0)
        {
            try
            {
                strParams = Arrays.asList(params).stream().map(a -> toString(a)).collect(Collectors.joining(", ", "(", ")"));
                strParams = wrap(strParams, 30, "\\n", true);
            }
            catch (Exception ex)
            {
                strParams = "(can't show parameters)";
            }
        }
        else
        {
            strParams = "";
        }
        if (invocation.getHeaders() != null)
        {
            strParams += " " + invocation.getHeaders();
        }
        if (!invocation.isOneWay())
        {

            final String msg = '"' + from + "\" -> \"" + to + "\" : [" + id + "] " + methodName + strParams
                    + "\r\n"
                    + "activate \"" + to + "\"";
            actorBaseTest.sequenceDiagram.add(msg);
            while (actorBaseTest.sequenceDiagram.size() > 100)
            {
                actorBaseTest.sequenceDiagram.remove(0);
            }
            actorBaseTest.hiddenLog.info(msg);
            final long start = System.nanoTime();

            @SuppressWarnings("unchecked")
            final Task<Object> write = (Task) ctx.write(message);
            return write.whenComplete((r, e) ->
            {
                final long timeUS = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start);
                final String timeStr = NumberFormat.getNumberInstance(Locale.US).format(timeUS);
                if (e == null)
                {
                    final String resp = '"' + to + "\" --> \"" + from + "\" : [" + id + "; "
                            + timeStr + "us] " + wrap("(response to " + methodName + "): " + toString(r), 32, "\\n", true)
                            + "\r\n"
                            + "deactivate \"" + to + "\"";
                    actorBaseTest.sequenceDiagram.add(resp);
                    actorBaseTest.hiddenLog.info(resp);
                }
                else
                {
                    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
                    final Throwable throwable = unwrapException(e);
                    final String resp = '"' + to + "\" --> \"" + from + "\" : [" + id
                            + "; " + timeStr + "us] (exception at " + methodName + "):\\n"
                            + throwable.getClass().getName()
                            + (throwable.getMessage() != null ? ": \\n" + throwable.getMessage() : "")
                            + "\r\n"
                            + "deactivate \"" + to + "\"";
                    actorBaseTest.sequenceDiagram.add(resp);
                    actorBaseTest.hiddenLog.info(resp);
                }
            });

        }
        else
        {
            final String msg = '"' + from + "\" -> \"" + to + "\" : [" + id + "] " + methodName + strParams;
            actorBaseTest.sequenceDiagram.add(msg);
            actorBaseTest.hiddenLog.info('"' + from + "\" -> \"" + to + "\" : [" + id + "] " + methodName + strParams);
            return ctx.write(message);
        }
    }


    private Throwable unwrapException(final Throwable e)
    {
        Throwable ex = e;
        while (ex.getCause() != null && (ex instanceof CompletionException))
        {
            ex = ex.getCause();
        }
        return ex;
    }


    private String getFrom(final RemoteReference reference, final Method method)
    {
        final ActorTaskContext context = ActorTaskContext.current();
        String from;
        if (context != null && context.getActor() != null)
        {
            final RemoteReference contextReference = RemoteReference.from(context.getActor());
            from = RemoteReference.getInterfaceClass(contextReference).getSimpleName()
                    + ":"
                    + RemoteReference.getId(contextReference);
        }
        else
        {
            if (reference != null && RemoteReference.getInterfaceClass(reference) == NodeCapabilities.class
                    && method.getName().equals("canActivate"))
            {
                from = "Stage";
            }
            else
            {
                final TaskContext current = TaskContext.current();
                if (current != null && current.getProperty(ActorBaseTest.TEST_NAME_PROP) != null)
                {
                    from = String.valueOf(current.getProperty(ActorBaseTest.TEST_NAME_PROP));
                }
                else
                {
                    from = "Thread:" + Thread.currentThread().getId();
                }
            }
        }
        return from;
    }

}
