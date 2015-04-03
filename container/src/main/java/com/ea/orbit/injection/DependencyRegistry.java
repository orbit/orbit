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

package com.ea.orbit.injection;

import com.ea.orbit.annotation.Wired;
import com.ea.orbit.exception.UncheckedException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Very basic injection library.
 * <p/>
 * Request based injection can be done by composition (adding the parent singletons to the request Registry)
 * <p/>
 * Please note: The parent registry should not be modified once the request/child contexts start running in their threads.
 */
public class DependencyRegistry
{
    private final DependencyRegistry parent;
    private final Map<Class<?>, Object[]> singletons = new HashMap<>();

    private final Set<Object> currentlyInjecting = new HashSet<>();

    public DependencyRegistry()
    {
        this.parent = null;
    }

    public DependencyRegistry(final DependencyRegistry parent)
    {
        this.parent = parent;
    }

    public <T> T locate(final Class<T> clazz)
    {
        T o = getSingleton(clazz);
        if (o == null && parent != null)
        {
            o = parent.getSingleton(clazz);
        }
        if (o != null)
        {
            return o;
        }

        return createNew(clazz);
    }

    protected <T> T createNew(final Class<T> clazz)
    {
        return initialize(clazz, null, false);
    }

    protected <T> T initialize(final Class<T> clazz, final T o, final boolean isSingleton)
    {
        if (!isSingleton)
        {
            if (currentlyInjecting.contains(clazz))
            {
                throw new UncheckedException("Found cycle with: " + clazz);
            }
            currentlyInjecting.add(clazz);
        }
        try
        {
            final T obj = o == null ? clazz.newInstance() : o;
            inject(obj);
            postConstruct(obj);
            return obj;
        }
        catch (InstantiationException | IllegalAccessException e)
        {
            throw new UncheckedException("Error creating " + clazz, e);
        }
        finally
        {
            if (!isSingleton)
            {
                currentlyInjecting.remove(clazz);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getSingleton(final Class<T> clazz)
    {
        Class<T> aClazz = clazz;
        // look for existing singletons
        Object[] o = singletons.get(clazz);
        if (o == null)
        {
            // look for subclasses in the existing singletons
            for (final Map.Entry<Class<?>, Object[]> s : singletons.entrySet())
            {
                if (clazz.isAssignableFrom(s.getKey()))
                {
                    aClazz = (Class<T>) s.getKey();
                    o = s.getValue();
                    break;
                }
            }
            if (o == null)
            {
                return null;
            }
        }
        if (o[0] == null)
        {
            try
            {
                o[0] = aClazz.newInstance();
                return initialize(aClazz, (T) o[0], true);
            }
            catch (InstantiationException | IllegalAccessException e)
            {
                throw new UncheckedException(e);
            }

        }
        return (T) o[0];
    }

    public void addSingleton(final Class<?> c, final Object object)
    {
        singletons.put(c, new Object[]{object});
    }

    public void addSingleton(final Class<?> c)
    {
        addSingleton(c, null);
    }

    public void inject(final Object o)
    {
        for (Class<?> cl = o.getClass(); cl != null && cl != Object.class; cl = cl.getSuperclass())
        {
            for (final Field f : cl.getDeclaredFields())
            {
                try
                {
                    injectField(o, f);
                }
                catch (Exception e)
                {
                    throw new UncheckedException("Error initializing field " + f, e);
                }
            }
        }
        postInject(o);
    }

    protected void postInject(final Object o)
    {
        if (parent != null)
        {
            parent.postInject(o);
        }
    }

    protected void injectField(final Object o, final Field f) throws IllegalAccessException
    {
        if (f.isAnnotationPresent(Inject.class) || f.isAnnotationPresent(Wired.class))
        {
            f.setAccessible(true);
            final Object located = locate(f.getType());
            f.set(o, located);
        }
    }

    protected void postConstruct(final Object o)
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
}
