package com.ea.orbit.actors.runtime;

import com.ea.orbit.actors.IActor;
import com.ea.orbit.actors.providers.IActorClassFinder;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;
import com.ea.orbit.util.ClassPath;
import com.ea.orbit.util.IOUtils;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ActorClassFinder implements IActorClassFinder
{
    private static final ClassPathSearch search = new ClassPathSearch(IActor.class);

    private ConcurrentHashMap<Class<?>, Class<?>> cache = new ConcurrentHashMap<>();
    private Execution execution;

    public ActorClassFinder(Execution execution)
    {
        this.execution = execution;
    }

    public ActorClassFinder()
    {

    }

    @Override
    public <T extends IActor> Class<? extends T> findActorImplementation(Class<T> iActorInterface)
    {
        Class<?> r = cache.get(iActorInterface);
        return r != null ? (Class<? extends T>) r : search.findImplementation(iActorInterface);
    }

    @Override
    public Task<?> start()
    {
        if (execution == null)
        {
            return Task.done();
        }
        try
        {
            if (execution.getAutoDiscovery())
            {
                final List<ClassPath.ResourceInfo> actorInterfacesRes = ClassPath.get().getAllResources().stream()
                        .filter(r -> r.getResourceName().startsWith("META-INF/orbit/actors/interfaces")).collect(Collectors.toList());
                final List<ClassPath.ResourceInfo> actorClassesRes = ClassPath.get().getAllResources().stream()
                        .filter(r -> r.getResourceName().startsWith("META-INF/orbit/actors/classes")).collect(Collectors.toList());

                for (ClassPath.ResourceInfo irs : actorInterfacesRes)
                {
                    // pre register factories
                    String nameFactoryName = IOUtils.toString(irs.url().openStream());
                    ActorFactory<?> factory = (ActorFactory<?>) classForName(nameFactoryName).newInstance();
                    execution.registerFactory(factory);
                }

                for (ClassPath.ResourceInfo irs : actorClassesRes)
                {
                    String className = irs.getResourceName().substring("META-INF/orbit/actors/classes".length() + 1);
                    Class<?> actorClass = classForName(className);
                    cacheBestInterface(actorClass);
                }
            }
            List<Class<?>> actorClasses = execution.getActorClasses();
            if (actorClasses != null && !actorClasses.isEmpty())
            {
                for (Class<?> actorClass : actorClasses)
                {
                    cacheBestInterface(actorClass);
                }
            }
        }

        catch (Throwable e)
        {
            throw new UncheckedException(e);
        }
        return Task.done();
    }

    private void cacheBestInterface(Class<?> actorClass)
    {
        Class<?> bestInterface = null;
        for (Class<?> interfaceClass : actorClass.getInterfaces())
        {
            if (IActor.class.isAssignableFrom(interfaceClass) && interfaceClass != IActor.class)
            {
                bestInterface = (bestInterface == null) ? interfaceClass
                        : bestInterface.isAssignableFrom(interfaceClass) ? interfaceClass
                        : bestInterface;
            }
        }
        if (bestInterface != null)
        {
            cache.put(actorClass, bestInterface);
        }
    }

    private Class<?> classForName(String className)
    {
        try
        {
            return Class.forName(className);
        }
        catch (ClassNotFoundException e)
        {
            throw new UncheckedException(e);
        }
    }

}