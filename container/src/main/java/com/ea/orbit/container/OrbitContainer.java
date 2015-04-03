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

package com.ea.orbit.container;

import com.ea.orbit.annotation.Config;
import com.ea.orbit.annotation.Nullable;
import com.ea.orbit.annotation.Wired;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.configuration.OrbitProperties;
import com.ea.orbit.configuration.OrbitPropertiesImpl;
import com.ea.orbit.configuration.Secret;
import com.ea.orbit.configuration.SecretManager;
import com.ea.orbit.exception.UncheckedException;
import com.ea.orbit.injection.DependencyRegistry;
import com.ea.orbit.reflect.ClassCache;
import com.ea.orbit.reflect.FieldDescriptor;
import com.ea.orbit.util.ClassPath;
import com.ea.orbit.util.ClassPath.ResourceInfo;

import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class OrbitContainer
{
    private static final String ORBIT_PROVIDERS_ = "orbit.providers.";
    private static final String ORBIT_SERVICE_PREFIX = "META-INF/services/orbit/";
    private OrbitProperties properties;
    private Map<String, ComponentState> components = new ConcurrentHashMap<>();

    private CompletableFuture stopFuture = new CompletableFuture();

    /**
     * Defines if orbit should discover services by looking as META-INF/services/orbit/ to find service components
     */
    @Config("orbit.locateServices")
    private boolean locateServices = true;

    @Config("orbit.providers")
    private List<Object> providers = new ArrayList<>();

    private final DependencyRegistry registry = new DependencyRegistry()
    {

        protected WeakHashMap<Class, Boolean> notHere = new WeakHashMap();

        @Override
        protected void injectField(final Object o, final java.lang.reflect.Field f) throws IllegalArgumentException, IllegalAccessException
        {
            super.injectField(o, f);
            injectConfig(o, f);
        }

        @Override
        public <T> T getSingleton(final Class<T> clazz)
        {
            if (!notHere.containsKey(clazz))
            {
                ComponentState state = getState(clazz);
                if (state != null && state.enabled)
                {
                    if (state.instance == null && state.isSingleton)
                    {
                        state.instance = createNew(state.implClass);
                        //throw new NotAvailableHere("This service was not instantiated at this node: " + clazz.getName());
                    }
                    return (T) state.instance;
                }
                notHere.put(clazz, Boolean.TRUE);
            }
            return super.getSingleton(clazz);
        }

        @Override
        protected void postInject(final Object o)
        {
            super.postInject(o);
            try
            {
                OrbitContainer.this.injectConfig(o);
            }
            catch (IllegalAccessException e)
            {
                throw new UncheckedException(e);
            }
        }
    };

    public List<Class<?>> getClasses()
    {
        return components.values().stream().map(c -> c.implClass).collect(Collectors.toList());
    }

    public void join() throws ExecutionException, InterruptedException
    {
        stopFuture.get();
    }

    protected static class ComponentState
    {
        boolean enabled = true;
        boolean initialized;
        String implClassName;
        Class<?> implClass;
        List<ComponentState> dependsOn = new ArrayList<>();

        Object instance;
        boolean isSingleton;
    }

    public void setLocateServices(final boolean locateServices)
    {
        this.locateServices = locateServices;
    }

    public boolean isLocateServices()
    {
        return locateServices;
    }

    protected enum ContainerState
    {
        CREATED,
        STARTING,
        STARTED,
        STOPPING,
        STOPPED
    }

    private ContainerState state = ContainerState.CREATED;

    public void add(final Class<?> componentClass)
    {
        ComponentState state = components.get(componentClass.getName());
        if (state == null)
        {
            state = new ComponentState();
            components.put(componentClass.getName(), state);
            state.isSingleton = componentClass.isAnnotationPresent(Singleton.class);
        }
        state.implClass = componentClass;
    }

    public <T> void addInstance(T instance)
    {
        ComponentState state = components.get(instance.getClass().getName());
        if (state == null)
        {
            state = new ComponentState();
            components.put(instance.getClass().getName(), state);
        }
        if (state.implClass == null)
        {
            state.implClass = instance.getClass();
        }
        state.instance = instance;
    }

    public void setProperties(final Map<String, Object> properties)
    {
        if (this.properties == null)
        {
            this.properties = new OrbitPropertiesImpl();
            this.properties.putAll(System.getProperties());
            addInstance(this.properties);
        }
        this.properties.putAll(properties);
    }

    @SuppressWarnings("unchecked")
    protected void injectConfig(Object o, java.lang.reflect.Field f) throws IllegalAccessException
    {
        final Config config = f.getAnnotation(Config.class);
        if (config != null)
        {
            if (Modifier.isFinal(f.getModifiers()))
            {
                throw new RuntimeException("Configurable fields should never be final: " + f);
            }

            f.setAccessible(true);

            if (f.getType() == Integer.TYPE || f.getType() == Integer.class)
            {
                f.set(o, properties.getAsInt(config.value(), (Integer) f.get(o)));
            }
            else if (f.getType() == Boolean.TYPE || f.getType() == Boolean.class)
            {
                f.set(o, properties.getAsBoolean(config.value(), (Boolean) f.get(o)));
            }
            else if (f.getType() == String.class)
            {
                f.set(o, properties.getAsString(config.value(), (String) f.get(o)));
            }
            else if (f.getType() == Secret.class)
            {
                final Secret secret = get(SecretManager.class).decrypt((String) f.get(o));
                f.set(o, secret);
            }
            else if (f.getType().isEnum())
            {
                final String enumValue = properties.getAsString(config.value(), null);
                if (enumValue != null)
                {
                    f.set(o, Enum.valueOf((Class<Enum>) f.getType(), enumValue));
                }
            }
            else if (properties.getAll().get(config.value()) != null)
            {
                final Object val = properties.getAll().get(config.value());
                f.set(o, val);
            }
            else if (List.class.isAssignableFrom(f.getType()))
            {
                if ((properties.getAll().get(config.value()) != null))
                {
                    final Object val = properties.getAll().get(config.value());
                    f.set(o, val);
                }
            }
            else
            {
                throw new UncheckedException("Field type not supported for configuration injection: " + f);
            }
        }
    }

    protected void injectConfig(Object o) throws IllegalAccessException
    {
        for (FieldDescriptor fd : ClassCache.shared.getClass(o.getClass()).getAllInstanceFields())
        {
            injectConfig(o, fd.getField());
        }
    }

    protected void injectField(ComponentState state, Field f) throws IllegalArgumentException, IllegalAccessException
    {
        if (f.isAnnotationPresent(Inject.class) || f.isAnnotationPresent(Wired.class))
        {
            f.setAccessible(true);
            ComponentState locatedComponent = getState(f.getType());
            if (locatedComponent != null)
            {
                state.dependsOn.add(locatedComponent);
                f.set(state.instance, locatedComponent.instance);
                return;
            }
            if ((f.getType() == List.class || f.getType() == Collection.class)
                    && f.getGenericType() instanceof ParameterizedType)
            {
                Type compType = ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0];
                if (compType instanceof Class)
                {
                    List locatedList = components((Class) compType);
                    if (locatedList != null)
                    {
                        f.set(state.instance, locatedList);
                        return;
                    }
                }
            }
            if (!f.isAnnotationPresent(Nullable.class))
            {
                throw new UncheckedException("Can't find value to inject on " + f);
            }
        }
    }

    public void inject(Object object)
    {
        registry.inject(object);
    }

    protected void injectState(ComponentState state)
    {
        for (FieldDescriptor fd : ClassCache.shared.getClass(state.implClass).getAllInstanceFields())
        {
            try
            {
                injectConfig(state.instance, fd.getField());
                injectField(state, fd.getField());
            }
            catch (IllegalAccessException e)
            {
                throw new UncheckedException(e);
            }
        }
    }

    public void postConstruct(final Object o)
    {
        for (Class<?> cl = o.getClass(); cl != null && cl != Object.class; cl = cl.getSuperclass())
        {
            for (final Method m : cl.getDeclaredMethods())
            {
                if (m.isAnnotationPresent(PostConstruct.class))
                {
                    try
                    {
                        m.setAccessible(true);
                        m.invoke(o);
                        return;
                    }
                    catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
                    {
                        throw new UncheckedException(e);
                    }
                }
            }
        }
    }

    /**
     * <ol><li>Compiles a list of classes that will be stored inside this container:
     * <ul>
     * <li>Reads the configuration from the properties</li>
     * <li>Looks for component declarations in the classpath: META-INF/services/orbit/*</li>
     * </ul>
     * </li>
     * <li>Instantiates all the components</li>
     * <li>Wires the components together by injecting fields and the configuration </li>
     * <li>Calls &#64;PostConstructor on all components</li>
     * <li>Calls start on all components that implement Startable</li>
     * </ol>
     */
    public void start()
    {
        if (state != ContainerState.CREATED)
        {
            throw new IllegalStateException(state.toString());
        }
        state = ContainerState.STARTING;
        try
        {
            if (properties == null)
            {
                URL res = getClass().getResource("/conf/orbit.yaml");
                if (res != null)
                {

                    Yaml yaml = new Yaml();
                    final Iterable<Object> iter = yaml.loadAll(res.openStream());
                    setProperties((Map) iter.iterator().next());
                }
                else
                {
                    setProperties(Collections.emptyMap());
                }
            }
            addInstance(this);
            injectConfig(this);
            addInstance(registry);

            if (locateServices)
            {
                for (ResourceInfo r : ClassPath.get().getAllResources())
                {
                    if (r.getResourceName().startsWith(ORBIT_SERVICE_PREFIX))
                    {
                        // TODO: use logger
                        System.out.println(r.getResourceName());
                        final String providesName = r.getResourceName().replaceAll(".*/", "");
                        ComponentState state = components.get(providesName);
                        if (state == null)
                        {
                            state = new ComponentState();
                            components.put(providesName, state);
                        }
                        state.implClassName = com.ea.orbit.util.IOUtils.toString(r.url().openStream());
                    }
                }
            }
            if (providers != null)
            {
                for (final Object service : providers)
                {
                    // TODO: use logger
                    System.out.println(service);
                    add((service instanceof Class) ? (Class<?>) service : Class.forName(String.valueOf(service)));
                }
            }

            Set<String> providersConfig = properties.getAll().keySet().stream().filter(e -> e.startsWith(ORBIT_PROVIDERS_)).collect(Collectors.toSet());
            for (String providerConfig : providersConfig)
            {
                String value = properties.getAsString(providerConfig, "enabled");
                String key = providerConfig.substring(ORBIT_PROVIDERS_.length());
                ComponentState state = components.get(key);
                if (state == null)
                {
                    state = new ComponentState();
                    state.implClassName = key;
                    components.put(key, state);
                }
                switch (value)
                {
                    case "enabled":
                        state.enabled = true;
                        break;
                    default:
                        state.enabled = false;
                }
            }

            // figuring out what needs to be a proxy
            for (ComponentState state : components.values())
            {
                if (state.enabled)
                {
                    if (state.implClass == null)
                    {
                        state.implClass = Class.forName(state.implClassName);
                    }
                }
            }


            Set<Class<?>> newComps = new LinkedHashSet();
            // Instantiating modules
            for (ComponentState state : components.values())
            {
                if (state.enabled && state.instance == null && Module.class.isAssignableFrom(state.implClass))
                {
                    state.instance = state.implClass.newInstance();
                    injectState(state);
                    newComps.addAll(((Module) state.instance).getClasses());
                }
            }
            newComps.forEach(c -> add(c));

            List<ComponentState> comps = new ArrayList<>(components.values());

            // Instantiating
            for (ComponentState state : comps)
            {
                if (state.enabled && state.instance == null && state.isSingleton && Startable.class.isAssignableFrom(state.implClass))
                {
                    state.instance = state.implClass.newInstance();
                }
            }

            // Injecting fields
            for (ComponentState state : comps)
            {
                if (state.enabled && state.instance != null && !state.initialized)
                {
                    injectState(state);
                }
            }

            // Calling post constructors.
            for (ComponentState state : comps)
            {
                if (state.enabled && state.instance != null && !state.initialized)
                {
                    postConstruct(state.instance);
                    state.initialized = true;
                }
            }

            // just in case the post constructor added new components
            if (components.size() != comps.size())
            {
                comps.addAll(components.values());
            }

            List futures = new ArrayList<>();
            // Call start methods
            for (ComponentState state : comps)
            {
                if (state.enabled && state.instance != null && state.instance instanceof Startable)
                {
                    final Task future = ((Startable) state.instance).start();
                    if (future != null && !future.isDone())
                    {
                        futures.add(future);
                    }
                }
            }
            if (futures.size() > 0)
            {
                Task.allOf(futures).join();
            }
            state = ContainerState.STARTED;
        }
        catch (Throwable ex)
        {
            throw new UncheckedException(ex);
        }
    }

    public void stop()
    {
        state = ContainerState.STOPPING;
        for (ComponentState state : components.values())
        {
            if (state.enabled && state.instance != null && state.instance instanceof Startable)
            {
                try
                {
                    ((Startable) state.instance).stop();
                }
                catch (Exception e)
                {
                    throw new UncheckedException("Error stopping " + state.implClass.getName(), e);
                }
            }
        }
        stopFuture.complete(true);
        state = ContainerState.STOPPED;
    }


    protected ComponentState getState(final Class<?> componentClass)
    {
        ComponentState state = components.get(componentClass.getName());
        if (state != null && state.instance != null)
        {
            return state;
        }
        for (ComponentState s : components.values())
        {
            if (s.instance != null && componentClass.isInstance(s.instance))
            {
                return s;
            }
        }
        return state;
    }


    @SuppressWarnings("unchecked")
    public <T> T get(final Class<T> componentClass)
    {
        ComponentState state = getState(componentClass);
        if (state != null && state.instance != null)
        {
            return (T) state.instance;
        }
        return registry.locate(componentClass);
    }

    public List<Object> components()
    {
        List<Object> list = new ArrayList<>(components.size());
        for (ComponentState state : components.values())
        {
            if (state.enabled && state.instance != null)
            {
                list.add(state.instance);
            }
        }
        return Collections.unmodifiableList(list);
    }

    public <T> List<T> components(final Class<T> actorClass)
    {
        List<T> list = new ArrayList<T>();
        for (ComponentState s : components.values())
        {
            if (s.instance != null && actorClass.isInstance(s.instance))
            {
                list.add((T) s.instance);
            }
        }
        return list;
    }

}
