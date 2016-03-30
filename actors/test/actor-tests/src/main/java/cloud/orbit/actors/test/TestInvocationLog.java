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

package cloud.orbit.actors.test;

import cloud.orbit.actors.extensions.PipelineExtension;
import cloud.orbit.actors.net.HandlerContext;
import cloud.orbit.actors.peer.PeerExtension;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.runtime.ActorTaskContext;
import cloud.orbit.actors.runtime.DefaultDescriptorFactory;
import cloud.orbit.actors.runtime.DefaultHandlers;
import cloud.orbit.actors.runtime.Invocation;
import cloud.orbit.actors.runtime.NodeCapabilities;
import cloud.orbit.actors.runtime.ReminderController;
import cloud.orbit.actors.runtime.RemoteReference;
import cloud.orbit.concurrent.Task;
import cloud.orbit.concurrent.TaskContext;

import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


public class TestInvocationLog implements PipelineExtension, PeerExtension
{
    private AtomicLong invocationId = new AtomicLong();

    private TestLogger logger;
    private String name;

    public TestInvocationLog(final TestLogger logger)
    {
        this(logger, "");
    }

    public TestInvocationLog(final TestLogger logger, String name)
    {
        this.logger = logger;
        this.name = name;
    }

    @Override
    public String getName()
    {
        return "test-invocation-logging";
    }

    @Override
    public String getAfterHandlerName()
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
        String from = TestLogger.wrap(getFrom(toReference, method), 32, "\\n", false);
        String to = TestLogger.wrap(RemoteReference.getInterfaceClass(toReference).getSimpleName()
                + ":"
                + RemoteReference.getId(toReference), 32, "\\n", false);

        String strParams;
        final Object[] params = invocation.getParams();
        if (params != null && params.length > 0)
        {
            try
            {
                strParams = Arrays.asList(params).stream().map(a -> toString(a)).collect(Collectors.joining(", ", "(", ")"));
                strParams = TestLogger.wrap(strParams, 30, "\\n", true);
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

            final String msg = '"' + from + "\" -> \"" + to + "\" : [" + name + ":m" + id + "] " + methodName + strParams
                    + "\r\n"
                    + "activate \"" + to + "\"";
            logger.sequenceDiagram.add(msg);
            while (logger.sequenceDiagram.size() > 100)
            {
                logger.sequenceDiagram.remove(0);
            }
            logger.write(msg);
            final long start = System.nanoTime();

            @SuppressWarnings("unchecked")
            final Task<Object> write = (Task) ctx.write(message);
            return write.whenComplete((r, e) ->
            {
                final long timeUS = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start);
                final String timeStr = NumberFormat.getNumberInstance(Locale.US).format(timeUS);
                if (e == null)
                {
                    final String resp = '"' + to + "\" --> \"" + from + "\" : [" + name + ":r" + id + "; "
                            + timeStr + "us] " + TestLogger.wrap("(response to " + methodName + "): " + toString(r), 32, "\\n", true)
                            + "\r\n"
                            + "deactivate \"" + to + "\"";
                    logger.sequenceDiagram.add(resp);
                    logger.write(resp);
                }
                else
                {
                    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
                    final Throwable throwable = unwrapException(e);
                    final String resp = '"' + to + "\" --> \"" + from + "\" : [" + name + ":r" + id
                            + "; " + timeStr + "us] (exception at " + methodName + "):\\n"
                            + throwable.getClass().getName()
                            + (throwable.getMessage() != null ? ": \\n" + throwable.getMessage() : "")
                            + "\r\n"
                            + "deactivate \"" + to + "\"";
                    logger.sequenceDiagram.add(resp);
                    logger.write(resp);
                }
            });

        }
        else
        {
            final String msg = '"' + from + "\" -> \"" + to + "\" : [" + id + "] " + methodName + strParams;
            logger.sequenceDiagram.add(msg);
            logger.write('"' + from + "\" -> \"" + to + "\" : [" + id + "] " + methodName + strParams);
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
