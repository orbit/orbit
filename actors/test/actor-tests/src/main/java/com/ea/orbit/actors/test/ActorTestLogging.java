package com.ea.orbit.actors.test;

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.Addressable;
import com.ea.orbit.actors.annotation.OneWay;
import com.ea.orbit.actors.extensions.DefaultLoggerExtension;
import com.ea.orbit.actors.extensions.LoggerExtension;
import com.ea.orbit.actors.extensions.PipelineExtension;
import com.ea.orbit.actors.net.HandlerContext;
import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.actors.runtime.ActorTaskContext;
import com.ea.orbit.actors.runtime.DefaultHandlers;
import com.ea.orbit.actors.runtime.Invocation;
import com.ea.orbit.actors.runtime.NodeCapabilities;
import com.ea.orbit.actors.runtime.ReminderController;
import com.ea.orbit.actors.runtime.RemoteReference;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.concurrent.TaskContext;

import org.slf4j.Logger;
import org.slf4j.Marker;

import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


class ActorTestLogging implements PipelineExtension, LoggerExtension
{
    private AtomicLong invocationId = new AtomicLong();

    private ActorBaseTest actorBaseTest;
    DefaultLoggerExtension defaultLogger = new DefaultLoggerExtension();

    public ActorTestLogging(final ActorBaseTest actorBaseTest)
    {
        this.actorBaseTest = actorBaseTest;
    }

    @Override
    public String getName()
    {
        return "test-logging";
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
        final Addressable toReference = invocation.getToReference();
        if (toReference instanceof NodeCapabilities)
        {
            return ctx.write(message);
        }
        final Method method = invocation.getMethod();
        if (toReference instanceof ReminderController && "ensureStart".equals(method.getName()))
        {
            return ctx.write(message);
        }
        String from = getFrom((RemoteReference) toReference, method);
        String to = RemoteReference.getInterfaceClass((RemoteReference) toReference).getSimpleName()
                + ":"
                + RemoteReference.getId((RemoteReference) toReference);

        String strParams;
        final Object[] params = invocation.getParams();
        if (params != null && params.length > 0)
        {
            try
            {
                strParams = Arrays.asList(params).stream().map(a -> toString(a)).collect(Collectors.joining(", ", "(", ")"));
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
        if (!method.isAnnotationPresent(OneWay.class))
        {
            final String msg = '"' + from + "\" -> \"" + to + "\" : [" + id + "] " + method.getName() + strParams
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
                            + timeStr + "us] (response to " + method.getName() + "): " + toString(r)
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
                            + "; " + timeStr + "us] (exception at " + method.getName() + "):\\n"
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
            final String msg = '"' + from + "\" -> \"" + to + "\" : [" + id + "] " + method.getName() + strParams;
            actorBaseTest.sequenceDiagram.add(msg);
            actorBaseTest.hiddenLog.info('"' + from + "\" -> \"" + to + "\" : [" + id + "] " + method.getName() + strParams);
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

    @Override
    public Logger getLogger(final Object object)
    {
        final Logger logger = defaultLogger.getLogger(object);

        String target;
        if (object instanceof Actor)
        {
            final RemoteReference reference = RemoteReference.from((Actor) object);
            target = (RemoteReference.getInterfaceClass(reference).getSimpleName()
                    + ":" + RemoteReference.getId(reference)).replaceAll("[\"\\t\\r\\n]", "");
        }
        else
        {
            target = logger.getName();
        }
        return new LogInterceptor(logger)
        {
            @Override
            protected void message(final String type, final Marker marker, final String format, final Object... arguments)
            {
                super.message(type, marker, format, arguments);
                final String message = (!"info".equalsIgnoreCase(type) ? type + ": " : "") +
                        String.format(format, arguments);
                String position = "over";
                note(position, target, message);
            }
        };
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

    public void note(final String position, final String message)
    {
        note(position, getFrom(null, null), message);
    }

    private void note(final String position, final String target, final String message)
    {
        final StringBuilder note = new StringBuilder("note ");
        note.append(position).append(" \"").append(target);
        if (message.contains("\n"))
        {
            note.append("\r\n");
            note.append(message);
            note.append("\r\n").append("end note");

        }
        else
        {
            note.append("\": ").append(message);
        }
        actorBaseTest.sequenceDiagram.add(note.toString());
    }
}
