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

import com.ea.orbit.actors.Actor;
import com.ea.orbit.actors.Addressable;
import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.annotation.OneWay;
import com.ea.orbit.actors.extensions.InvocationContext;
import com.ea.orbit.actors.extensions.InvokeHookExtension;
import com.ea.orbit.actors.extensions.LifetimeExtension;
import com.ea.orbit.actors.runtime.AbstractActor;
import com.ea.orbit.actors.runtime.ActorFactoryGenerator;
import com.ea.orbit.actors.runtime.ActorReference;
import com.ea.orbit.actors.runtime.ActorTaskContext;
import com.ea.orbit.actors.runtime.Execution;
import com.ea.orbit.actors.runtime.ExecutionSerializer;
import com.ea.orbit.actors.runtime.NodeCapabilities;
import com.ea.orbit.actors.runtime.ReminderController;
import com.ea.orbit.actors.runtime.cloner.ExecutionObjectCloner;
import com.ea.orbit.actors.runtime.cloner.KryoCloner;
import com.ea.orbit.concurrent.ExecutorUtils;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.concurrent.TaskContext;
import com.ea.orbit.exception.UncheckedException;
import com.ea.orbit.injection.DependencyRegistry;

import org.apache.commons.logging.impl.SimpleLog;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.google.common.util.concurrent.ForwardingExecutorService;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.fail;

@SuppressWarnings("VisibilityModifierCheck")
public class ActorBaseTest
{
    protected String clusterName = "cluster." + Math.random() + "." + getClass().getSimpleName();
    protected FakeClock clock = new FakeClock();
    protected ConcurrentHashMap<Object, Object> fakeDatabase = new ConcurrentHashMap<>();

    protected static final ExecutorService commonPool = new ForwardingExecutorService()
    {
        ExecutorService delegate = ExecutorUtils.newScalingThreadPool(200);

        @Override
        protected ExecutorService delegate()
        {
            return delegate;
        }

        @Override
        public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException
        {
            // return immediately.
            return true;
        }

        @Override
        public void shutdown()
        {
            try
            {
                // Attention: intentionally not calling delegate.shutdown() to keep reusing it for other tests.
                delegate.awaitTermination(0, TimeUnit.SECONDS);
            }
            catch (InterruptedException e)
            {
                throw new UncheckedException(e);
            }
        }
    };

    protected FakeSync fakeSync = new FakeSync();
    private AtomicLong invocationId = new AtomicLong();
    protected final StringBuilder hiddenLogData = new StringBuilder();
    protected final List<String> messageSequence = Collections.synchronizedList(new LinkedList<>());

    protected final SimpleLog hiddenLog = new SimpleLog("orbit")
    {
        private static final long serialVersionUID = 1L;

        @Override
        protected synchronized void write(final StringBuffer buffer)
        {
            // truncating the log
            if (hiddenLogData.length() > 250e6)
            {
                hiddenLogData.setLength(64000);
                hiddenLogData.append("The log was truncated!").append("\r\n");
            }
            hiddenLogData.append(buffer).append("\r\n");
        }
    };

    private static final String TEST_NAME_PROP = ActorBaseTest.class.getName() + ".testName";

    @Rule
    public TestRule dumpLogs = new TestWatcher()
    {
        final ActorTaskContext taskContext = new ActorTaskContext();

        protected void starting(Description description)
        {
            taskContext.push();
            taskContext.setProperty(TEST_NAME_PROP, description.getMethodName());
            taskContext.setProperty(ActorBaseTest.class.getName(), description);
        }

        /**
         * Invoked when a test method finishes (whether passing or failing)
         */
        protected void finished(Description description)
        {
            try
            {
                taskContext.push();
            }
            catch (Exception ex)
            {
                // ignore
            }
        }


        /**
         * Invoked when a test succeeds
         */
        protected void succeeded(Description description)
        {
            messageSequence.clear();
            hiddenLogData.setLength(0);
            fakeDatabase.clear();
        }

        @Override
        protected void failed(final Throwable e, final Description description)
        {

            final PrintStream out = System.out;
            out.println(">>>>>>>>> Start");
            out.println(">>>>>>>>> Test Dump for " + description);
            out.println(">>>>>>>>> Error: " + e);
            out.println(hiddenLogData.toString());
            out.println(">>>>>>>>> Test Dump for " + description);
            out.print(">>>>>>>>> Error: ");
            e.printStackTrace(out);
            out.println(">>>>>>>>> End");
            dumpMessages(description);
            messageSequence.clear();
            hiddenLogData.setLength(0);
            fakeDatabase.clear();
        }
    };


    protected void clearMessages()
    {
        messageSequence.clear();
    }

    protected void dumpMessages()
    {
        final TaskContext taskContext = TaskContext.current();

        dumpMessages(taskContext != null ? (Description) taskContext.getProperty(ActorBaseTest.class.getName()) : null);
    }

    protected void dumpMessages(Description description)
    {
        final PrintStream out = System.out;
        if (messageSequence.size() > 0)
        {
            String name = ActorBaseTest.this.getClass().getName();
            if (description != null)
            {
                name += "-" + description.getMethodName();
            }

            final Path seqUml = Paths.get("target/surefire-reports/" + name + ".messages.puml");
            try
            {
                Files.createDirectories(seqUml.getParent());

                Files.write(seqUml,
                        Stream.concat(Stream.concat(
                                        Stream.of("@startuml"),
                                        messageSequence.stream()),
                                Stream.of("@enduml")
                        ).collect(Collectors.toList()));
                out.println("Message sequence diagram written to:");
                out.println(seqUml.toUri());
            }
            catch (Exception ex)
            {
                new IOException("error dumping messages: " + ex.getMessage(), ex).printStackTrace();
            }
        }
        else
        {
            out.println("No messages to dump");
        }
    }

    public Stage createClient() throws ExecutionException, InterruptedException
    {
        hiddenLog.info("Create Client");
        Stage client = new Stage();
        DependencyRegistry dr = new DependencyRegistry();
        dr.addSingleton(FakeSync.class, fakeSync);
        addLogging(client);
        client.addExtension(new LifetimeExtension()
        {
            @Override
            public Task<?> preActivation(final AbstractActor<?> actor)
            {
                dr.inject(actor);
                return Task.done();
            }
        });
        client.setMode(Stage.StageMode.FRONT_END);
        client.setExecutionPool(commonPool);
        client.setMessagingPool(commonPool);
        client.setClock(clock);
        client.setClusterName(clusterName);
        client.setClusterPeer(new FakeClusterPeer());
        client.start().join();
        client.bind();
        return client;
    }

    public Stage createStage() throws ExecutionException, InterruptedException
    {
        hiddenLog.info("Create Stage");
        Stage stage = new Stage();


        DependencyRegistry dr = new DependencyRegistry();
        dr.addSingleton(FakeSync.class, fakeSync);
        dr.addSingleton(Stage.class, stage);
        addLogging(stage);
        stage.addExtension(new LifetimeExtension()
        {
            @Override
            public Task<?> preActivation(final AbstractActor<?> actor)
            {
                dr.inject(actor);
                return Task.done();
            }
        });
        stage.setMode(Stage.StageMode.HOST);
        stage.setExecutionPool(commonPool);
        stage.setMessagingPool(commonPool);
        stage.setObjectCloner(getExecutionObjectCloner());
        stage.addExtension(new FakeStorageExtension(fakeDatabase));
        stage.setClock(clock);
        stage.setClusterName(clusterName);
        stage.setClusterPeer(new FakeClusterPeer());
        stage.start().join();

        // warm up
        ActorFactoryGenerator afg = new ActorFactoryGenerator();
        Stream.of(getClass().getClasses())
                .forEach(c -> {
                    if (Actor.class.isAssignableFrom(c) && c.isInterface())
                    {
                        afg.getFactoryFor(c);
                        stage.getHosting().canActivate(c.getName()).join();
                    }
                    if (AbstractActor.class.isAssignableFrom(c) && !Modifier.isAbstract(c.getModifiers()))
                    {
                        afg.getInvokerFor(c);
                    }
                });
        stage.bind();
        return stage;
    }

    private void addLogging(final Stage stage)
    {
        stage.addExtension(new InvokeHookExtension()
        {
            String toString(Object obj)
            {
                if (obj instanceof String)
                {
                    return (String) obj;
                }
                if (obj instanceof AbstractActor)
                {
                    final ActorReference ref = ActorReference.from((AbstractActor) obj);
                    return ActorReference.getInterfaceClass(ref).getSimpleName() + ":" +
                            ActorReference.getId(ref);
                }
                if (obj instanceof ActorReference)
                {
                    return ActorReference.getInterfaceClass((ActorReference<?>) obj).getSimpleName() + ":" +
                            ActorReference.getId((ActorReference<?>) obj);
                }
                return String.valueOf(obj);
            }

            @Override
            public Task<?> invoke(final InvocationContext icontext, final Addressable toReference, final Method method, final int methodId, final Object[] params)
            {
                long id = invocationId.incrementAndGet();
                if (toReference instanceof NodeCapabilities)
                {
                    return icontext.invokeNext(toReference, method, methodId, params);
                }
                if (toReference instanceof ReminderController && "ensureStart".equals(method.getName()))
                {
                    return icontext.invokeNext(toReference, method, methodId, params);
                }
                final ActorTaskContext context = ActorTaskContext.current();
                String from;
                if (context != null && context.getActor() != null)
                {
                    final ActorReference reference = ActorReference.from(context.getActor());
                    from = ActorReference.getInterfaceClass(reference).getSimpleName()
                            + ":"
                            + ActorReference.getId(reference);
                }
                else
                {
                    if (ActorReference.getInterfaceClass((ActorReference) toReference) == NodeCapabilities.class
                            && method.getName().equals("canActivate"))
                    {
                        from = "Stage";
                    }
                    else
                    {
                        final TaskContext current = TaskContext.current();
                        if (current != null && current.getProperty(TEST_NAME_PROP) != null)
                        {
                            from = String.valueOf(current.getProperty(TEST_NAME_PROP));
                        }
                        else
                        {
                            from = "Thread:" + Thread.currentThread().getId();
                        }
                    }
                }
                String to = ActorReference.getInterfaceClass((ActorReference) toReference).getSimpleName()
                        + ":"
                        + ActorReference.getId((ActorReference) toReference);

                String strParams;
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
                    messageSequence.add(msg);
                    while (messageSequence.size() > 100)
                    {
                        messageSequence.remove(0);
                    }
                    hiddenLog.info(msg);
                    final long start = System.nanoTime();
                    return icontext.invokeNext(toReference, method, methodId, params)
                            .whenComplete((r, e) ->
                                    {
                                        final long timeUS = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start);
                                        final String timeStr = NumberFormat.getNumberInstance(Locale.US).format(timeUS);
                                        if (e == null)
                                        {
                                            final String resp = '"' + to + "\" --> \"" + from + "\" : [" + id + "; "
                                                    + timeStr + "us] (response to " + method.getName() + "): " + toString(r)
                                                    + "\r\n"
                                                    + "deactivate \"" + to + "\"";
                                            messageSequence.add(resp);
                                            hiddenLog.info(resp);
                                        }
                                        else
                                        {
                                            final Throwable throwable = unwrapException(e);
                                            final String resp = '"' + to + "\" --> \"" + from + "\" : [" + id
                                                    + "; " + timeStr + "us] (exception at " + method.getName() + "):\\n"
                                                    + throwable.getClass().getName()
                                                    + (throwable.getMessage() != null ? ": \\n" + throwable.getMessage() : "")
                                                    + "\r\n"
                                                    + "deactivate \"" + to + "\"";
                                            messageSequence.add(resp);
                                            hiddenLog.info(resp);
                                        }
                                    }
                            );

                }
                else
                {
                    final String msg = '"' + from + "\" -> \"" + to + "\" : [" + id + "] " + method.getName() + strParams;
                    messageSequence.add(msg);
                    hiddenLog.info('"' + from + "\" -> \"" + to + "\" : [" + id + "] " + method.getName() + strParams);
                    return icontext.invokeNext(toReference, method, methodId, params);
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
        });
    }

    protected ExecutionObjectCloner getExecutionObjectCloner()
    {
        return new KryoCloner();
    }

    @FunctionalInterface
    public interface Exceptional
    {
        @SuppressWarnings("Checkstyle:IllegalThrowsCheck")
        Object call() throws Throwable;
    }

    @SuppressWarnings("PMD.AvoidCatchingThrowable")
    public Throwable expectException(Exceptional callable)
    {
        try
        {
            Object r = callable.call();
            if (r instanceof Future)
            {
                ((Future<?>) r).get(60, TimeUnit.SECONDS);
            }
        }
        catch (Throwable ex)
        {
            // ok
            return ex;
        }
        fail("Was expecting some exception");
        return null;
    }

    private Object getField(Object target, Class<?> clazz, String name) throws IllegalAccessException, NoSuchFieldException
    {
        final Field f = clazz.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    /**
     * Spins waiting for a given condition to be true.
     *
     * @param condition a function that must eventually return true
     */
    protected void awaitFor(Supplier<Boolean> condition)
    {
        try
        {
            while (!condition.get())
            {
                Thread.sleep(5);
            }
        }
        catch (Exception e)
        {

            throw new UncheckedException(e);
        }
    }

    /**
     * Checks if the that stages' execution is done running tasks
     *
     * @return boolean if there are no task running.
     */
    protected boolean isIdle(Stage stage)
    {
        try
        {
            // this is very ad hoc, but should work for our tests, until execution changes.
            // for starters access to this map should be synchronized.
            Map running = (Map) getField(getField(getField(stage, Stage.class, "execution"), Execution.class,
                    "executionSerializer"), ExecutionSerializer.class, "running");

            return running.size() == 0;
        }
        catch (Exception e)
        {

            throw new UncheckedException(e);
        }
    }

    protected void eventually(final Runnable runnable)
    {
        final long start = System.currentTimeMillis();
        do
        {
            try
            {
                runnable.run();
                return;
            }
            catch (RuntimeException | Error ex)
            {
                if (System.currentTimeMillis() - start > 60_000)
                {
                    // weird that this compiles...
                    throw ex;
                }
                try
                {
                    Thread.sleep(Math.max(200, (System.currentTimeMillis() - start) / 4));
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        } while (true);

    }

}

