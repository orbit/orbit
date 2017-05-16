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

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;

import cloud.orbit.actors.Actor;
import cloud.orbit.actors.Stage;
import cloud.orbit.actors.client.ClientPeer;
import cloud.orbit.actors.cloner.ExecutionObjectCloner;
import cloud.orbit.actors.cloner.KryoCloner;
import cloud.orbit.actors.concurrent.MultiExecutionSerializer;
import cloud.orbit.actors.concurrent.WaitFreeExecutionSerializer;
import cloud.orbit.actors.extensions.LifetimeExtension;
import cloud.orbit.actors.extensions.json.InMemoryJSONStorageExtension;
import cloud.orbit.actors.extensions.json.JsonMessageSerializer;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.runtime.AbstractExecution;
import cloud.orbit.actors.runtime.ActorFactoryGenerator;
import cloud.orbit.actors.runtime.ActorTaskContext;
import cloud.orbit.actors.runtime.Execution;
import cloud.orbit.actors.runtime.NodeCapabilities;
import cloud.orbit.actors.server.ServerPeer;
import cloud.orbit.concurrent.ExecutorUtils;
import cloud.orbit.concurrent.ForwardingExecutorService;
import cloud.orbit.concurrent.Task;
import cloud.orbit.exception.UncheckedException;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.Assert.fail;

@SuppressWarnings("VisibilityModifierCheck")
public class ActorBaseTest
{
    static final String TEST_NAME_PROP = ActorBaseTest.class.getName() + ".testName";
    protected TestLogger loggerExtension = new TestLogger();
    protected Logger logger = loggerExtension.getLogger(this.getClass());
    protected String clusterName = "cluster." + Math.random() + "." + getClass().getSimpleName();
    protected FakeClock clock = new FakeClock()
    {
        @Override
        public long incrementTime(final long time, final TimeUnit timeUnit)
        {
            return super.incrementTime(time, timeUnit);
        }
    };
    protected ConcurrentHashMap<Object, Object> fakeDatabase = new ConcurrentHashMap<>();
    protected List<Stage> stages = new ArrayList<>();
    protected List<FakeClient> clients = new ArrayList<>();
    protected List<ServerPeer> serversConnections = new ArrayList<>();

    protected Description testDescription;

    protected ServiceLocator serviceLocator;

    public ActorBaseTest()
    {
        ServiceLocatorFactory factory = ServiceLocatorFactory.getInstance();
        serviceLocator = factory.create(UUID.randomUUID().toString());
        ServiceLocatorUtilities.addOneConstant(serviceLocator, fakeSync);
    }

    protected static final ExecutorService commonPool = new ForwardingExecutorService<ExecutorService>(ExecutorUtils.newScalingThreadPool(200)) {
        @Override
        public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException
        {
            // return immediately.
            return true;
        }

        @Override
        public void shutdown()
        {
            // Attention: intentionally not calling delegate.shutdown() to keep reusing it for other tests.
        }
    };

    protected FakeSync fakeSync = new FakeSync();

    @Rule
    public TestRule dumpLogs = new TestWatcher()
    {
        final ActorTaskContext taskContext = new ActorTaskContext();

        protected void starting(Description description)
        {
            logger = loggerExtension.getLogger(description.getMethodName());
            taskContext.push();
            taskContext.setProperty(TEST_NAME_PROP, description.getMethodName());
            taskContext.setProperty(ActorBaseTest.class.getName(), description);
            testDescription = description;
        }

        /**
         * Invoked when a test method finishes (whether passing or failing)
         */
        protected void finished(Description description)
        {
            try
            {
                taskContext.pop();
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
            loggerExtension.clear();
            fakeDatabase.clear();
        }

        @Override
        protected void failed(final Throwable e, final Description description)
        {

            final PrintStream out = System.out;
            out.println(">>>>>>>>> Start");
            out.println(">>>>>>>>> Test Dump for " + description);
            out.println(">>>>>>>>> Error: " + e);
            out.println(loggerExtension.getLogText());
            out.println(">>>>>>>>> Test Dump for " + description);
            out.print(">>>>>>>>> Error: ");
            e.printStackTrace(out);
            out.println(">>>>>>>>> Stages: " + stages.size());
            stages.forEach(s -> out.println("    " + s));
            out.println(">>>>>>>>> Clients: " + clients.size());
            clients.forEach(s -> out.println("    " + s));
            out.println(">>>>>>>>> Server Connections: " + serversConnections.size());
            serversConnections.forEach(s -> out.println("    " + s));
            out.println(">>>>>>>>> End");
            String name = description.getClassName();
            if (description != null && description.getMethodName() != null)
            {
                name += "-" + description.getMethodName();
            }
            out.println("Message sequence diagram written to:");
            loggerExtension.dumpMessages("target/surefire-reports/" + name + "-error.messages.puml");
            loggerExtension.clear();
            fakeDatabase.clear();
        }
    };

    @After
    public void after()
    {
        stages.clear();
    }


    protected void clearMessages()
    {
        loggerExtension.sequenceDiagram.clear();
    }

    protected void dumpMessages()
    {
        String name = this.getClass().getName();
        if (testDescription != null && testDescription.getMethodName() != null)
        {
            name += "-" + testDescription.getMethodName();

            final StackTraceElement trace = Stream.of(new Exception().getStackTrace()).filter(x -> Objects.equals(x.getClassName(), testDescription.getClassName()))
                    .findFirst().orElse(null);
            final PrintStream out = System.out;
            if (trace != null)
            {
                out.println("Message sequence diagram for " + trace);
            }
            else
            {
                out.println("Message sequence diagram written to:");
            }
        }
        loggerExtension.dumpMessages("target/surefire-reports/" + name + ".messages.puml");
    }

    public ClientPeer createRemoteClient(Stage stage)
    {
        final JsonMessageSerializer serializer = new JsonMessageSerializer();
        final ShortCircuitHandler network = new ShortCircuitHandler();
        network.setExecutor(new WaitFreeExecutionSerializer(commonPool));

        int connectionId = clients.size();
        final FakeServerPeer serverPeer = new FakeServerPeer();
        serverPeer.setNetworkHandler(network);
        serverPeer.setClock(clock);
        serverPeer.setStage(stage);
        serverPeer.setMessageSerializer(serializer);
        serverPeer.addExtension(new TestLogger(loggerExtension, "sc" + connectionId));
        serverPeer.addExtension(new TestInvocationLog(loggerExtension, "sc" + connectionId));
        serversConnections.add(serverPeer);

        final FakeClient fakeClient = new FakeClient();
        clients.add(fakeClient);

        fakeClient.setNetworkHandler(network);
        fakeClient.setClock(clock);
        fakeClient.setMessageSerializer(serializer);
        fakeClient.addExtension(new TestLogger(loggerExtension, "cc" + connectionId));
        fakeClient.addExtension(new TestInvocationLog(loggerExtension, "cc" + connectionId));

        serverPeer.start();
        fakeClient.start();

        return fakeClient;
    }

    public Stage createClient()
    {
        loggerExtension.write("Create Client");

        LifetimeExtension lifetimeExtension = new LifetimeExtension()
        {
            @Override
            public Task<?> preActivation(final AbstractActor<?> actor)
            {
                serviceLocator.inject(actor);
                return Task.done();
            }
        };

        Stage client = new Stage.Builder()
                .mode(Stage.StageMode.CLIENT)
                .executionPool(commonPool)
                .clock(clock)
                .clusterName(clusterName)
                .clusterPeer(new FakeClusterPeer())
                .extensions(lifetimeExtension)
                .build();

        installExtensions(client);

        client.start().join();
        client.bind();
        return client;
    }

    public Stage createStage()
    {
        loggerExtension.write("Create Stage");

        LifetimeExtension lifetimeExtension = new LifetimeExtension()
        {
            @Override
            public Task<?> preActivation(final AbstractActor<?> actor)
            {
                serviceLocator.inject(actor);
                return Task.done();
            }
        };

        Stage stage = new Stage.Builder()
                .extensions(lifetimeExtension, new InMemoryJSONStorageExtension(fakeDatabase))
                .mode(Stage.StageMode.HOST)
                .executionPool(commonPool)
                .objectCloner(getExecutionObjectCloner())
                .clock(clock)
                .clusterName(clusterName)
                .clusterPeer(new FakeClusterPeer())
                .build();


        stages.add(stage);
        installExtensions(stage);

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

    protected void installExtensions(final Stage stage)
    {
        stage.addExtension(new TestLogger(loggerExtension, "s" + stages.size()));
        //stage.addExtension(new TestMessageLog(this, stage));
        stage.addExtension(new TestInvocationLog(loggerExtension, "s" + stages.size()));
        stage.addExtension(new TestLifecycleLog(loggerExtension, "s" + stages.size()));
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

    @SuppressWarnings("unchecked")
    private <T> T getField(Object target, Class<?> targetClazz, String name) throws IllegalAccessException, NoSuchFieldException
    {
        final Field f = targetClazz.getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(target);
    }

    /**
     * Spins waiting for a given condition to be true.
     *
     * @param condition a function that must eventually return true
     */
    protected void waitFor(Supplier<Boolean> condition)
    {
        try
        {
            while (!condition.get())
            {
                Thread.sleep(20);
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
            Execution execution = getField(stage, Stage.class, "execution");
            MultiExecutionSerializer executionSerializer = getField(execution, AbstractExecution.class, "executionSerializer");

            return !executionSerializer.isBusy();
        }
        catch (Exception e)
        {

            throw new UncheckedException(e);
        }
    }

    protected void eventually(final Runnable runnable)
    {
        eventually(60_000, runnable);
    }

    protected void eventually(long timeoutMillis, Runnable runnable)
    {
        eventuallyTrue(timeoutMillis, () -> {
            try
            {
                runnable.run();
                return true;
            }
            catch (RuntimeException | Error ex)
            {
                return false;
            }
        });
    }

    protected void eventuallyTrue(final Callable<Boolean> callable)
    {
        eventuallyTrue(60_000, callable);
    }

    protected void eventuallyTrue(long timeoutMillis, final Callable<Boolean> callable)
    {
        final long start = System.currentTimeMillis();
        do
        {
            try
            {
                if (Boolean.TRUE.equals(callable.call()))
                {
                    return;
                }
            }
            catch (Exception ex)
            {
                if (System.currentTimeMillis() - start > timeoutMillis)
                {
                    throw new UncheckedException(ex);
                }
            }
            try
            {
                Thread.sleep(Math.max(200, (System.currentTimeMillis() - start) / 2));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        } while (true);

    }

    @After
    public void tearDown()
    {
        Task.runAsync(() -> stages.stream()
                .filter(s -> s.getState() == NodeCapabilities.NodeState.RUNNING)
                .forEach(s -> {
                    try
                    {
                        s.stop();
                    }
                    catch (Throwable t)
                    {
                        // ignore
                    }
                }));
    }
}
