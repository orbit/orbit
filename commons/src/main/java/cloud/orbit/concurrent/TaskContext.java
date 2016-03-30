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

package cloud.orbit.concurrent;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class TaskContext
{
    private final static ThreadLocal<Deque<TaskContext>> contextStacks = new ThreadLocal<>();
    private final static WeakHashMap<Thread, Deque<TaskContext>> contextStacksMap = new WeakHashMap<>();
    private final static AtomicLong nextId = new AtomicLong(1);

    // human friendly id, for debugging
    private long id = nextId.getAndIncrement();

    private ConcurrentHashMap<String, Object> properties = new ConcurrentHashMap<>();

    private Executor defaultExecutor = null;

    public Executor getDefaultExecutor()
    {
        return defaultExecutor;
    }

    public void setDefaultExecutor(Executor defaultExecutor)
    {
        this.defaultExecutor = defaultExecutor;
    }

    /**
     * Adds this execution context to the top of the context stack for the current thread.
     */
    public void push()
    {
        Deque<TaskContext> stack = contextStacks.get();
        if (stack == null)
        {
            // Attention! Do not use a concurrent collection for the stack
            // it has been measured that concurrent collections decrease the TaskContext's performance.
            stack = new LinkedList<>();
            contextStacks.set(stack);
            final Thread currentThread = Thread.currentThread();
            synchronized (contextStacksMap)
            {
                // this happens only once per thread, no need to optimize it
                contextStacksMap.put(currentThread, stack);
            }
        }
        stack.addLast(this);
    }

    /**
     * Removes the this execution context from the context stack for the current thread.
     * This will fail with IllegalStateException if the current context is not at the top of the stack.
     */
    public void pop()
    {
        Deque<TaskContext> stack = contextStacks.get();
        if (stack == null)
        {
            throw new IllegalStateException("Invalid execution context stack state: " + stack + " trying to remove: " + this);
        }
        final TaskContext last = stack.pollLast();
        if (last != this)
        {
            if (last != null)
            {
                // returning it to the stack
                stack.addLast(last);
            }
            throw new IllegalStateException("Invalid execution context stack state: " + stack + " trying to remove: " + this + " but got: " + last);
        }
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + ":" + id;
    }

    /**
     * Gets the current execution context for this thread from the stack.
     *
     * @return the current context or null if there is none.
     */
    public static TaskContext current()
    {
        final Deque<TaskContext> stack = contextStacks.get();
        if (stack == null)
        {
            return null;
        }
        return stack.peekLast();
    }

    /**
     * Enables the application to peek into what is being executed in another thread.
     * This method is intended for debugging and profiling.
     */
    public static TaskContext currentFor(Thread thread)
    {
        final Deque<TaskContext> stack;
        synchronized (contextStacksMap)
        {
            // this should not be called very often, it's for profiling
            stack = contextStacksMap.get(thread);
        }
        // beware: this is peeking in a non synchronized LinkedList
        // just peeking is safe enough for profiling.
        return (stack != null) ? stack.peek() : null;
    }

    /**
     * @return all threads that have active contexts
     */
    public static Set<Thread> activeThreads()
    {
        synchronized (contextStacksMap)
        {
            return new HashSet<>(contextStacksMap.keySet());
        }
    }

    /**
     * Wraps a Runnable in such a way the it will push the current execution context before any code gets executed and pop it afterwards
     *
     * @param w the functional interface to be wrapped
     * @return wrapped object if there is a current execution context, or the same object if not.
     */
    public static Runnable wrap(Runnable w)
    {
        TaskContext c = current();
        if (c != null)
        {
            return () -> {
                c.push();
                try
                {
                    w.run();
                }
                finally
                {
                    c.pop();
                }
            };
        }
        return w;
    }

    /**
     * Wraps a BiConsumer in such a way the it will push the current execution context before any code gets executed and pop it afterwards
     *
     * @param w the functional interface to be wrapped
     * @return wrapped object if there is a current execution context, or the same object if not.
     */
    public static <T, U> BiConsumer<T, U> wrap(BiConsumer<T, U> w)
    {
        TaskContext c = current();
        if (c != null)
        {
            return (t, u) -> {
                c.push();
                try
                {
                    w.accept(t, u);
                }
                finally
                {
                    c.pop();
                }
            };
        }
        return w;
    }

    /**
     * Wraps a Consumer in such a way the it will push the current execution context before any code gets executed and pop it afterwards
     *
     * @param w the functional interface to be wrapped
     * @return wrapped object if there is a current execution context, or the same object if not.
     */
    public static <T> Consumer<T> wrap(Consumer<T> w)
    {
        TaskContext c = current();
        if (c != null)
        {
            return (t) -> {
                c.push();
                try
                {
                    w.accept(t);
                }
                finally
                {
                    c.pop();
                }
            };
        }
        return w;
    }

    /**
     * Wraps a Function in such a way the it will push the current execution context before any code gets executed and pop it afterwards
     *
     * @param w the functional interface to be wrapped
     * @return wrapped object if there is a current execution context, or the same object if not.
     */
    public static <T, R> Function<T, R> wrap(Function<T, R> w)
    {
        TaskContext c = current();
        if (c != null)
        {
            return (t) -> {
                c.push();
                try
                {
                    return w.apply(t);
                }
                finally
                {
                    c.pop();
                }
            };
        }
        return w;
    }


    /**
     * Wraps a Function in such a way the it will push the current execution context before any code gets executed and pop it afterwards
     *
     * @param w the functional interface to be wrapped
     * @return wrapped object if there is a current execution context, or the same object if not.
     */
    public static <T, U, R> BiFunction<T, U, R> wrap(BiFunction<T, U, R> w)
    {
        TaskContext c = current();
        if (c != null)
        {
            return (t, u) -> {
                c.push();
                try
                {
                    return w.apply(t, u);
                }
                finally
                {
                    c.pop();
                }
            };
        }
        return w;
    }

    /**
     * Wraps a Supplier in such a way the it will push the current execution context before any code gets executed and pop it afterwards
     *
     * @param w the functional interface to be wrapped
     * @return wrapped object if there is a current execution context, or the same object if not.
     */
    public static <T> Supplier<T> wrap(Supplier<T> w)
    {
        TaskContext c = current();
        if (c != null)
        {
            return () -> {
                c.push();
                try
                {
                    return w.get();
                }
                finally
                {
                    c.pop();
                }
            };
        }
        return w;
    }

    /**
     * Returns the property with the given name registered in the current execution context,
     * {@code null} if there is no property by that name.
     * <p>
     * A property allows orbit extensions to exchange custom information.
     * </p>
     *
     * @param name the name of the property
     * @return an {@code Object} or
     * {@code null} if no property exists matching the given name.
     */
    public Object getProperty(String name)
    {
        if (properties == null)
        {
            return null;
        }
        return properties.get(name);
    }

    /**
     * Binds an object to a given property name in the current execution context.
     * If the name specified is already used for a property,
     * this method will replace the value of the property with the new value.
     * <p>
     * A property allows orbit extensions to exchange custom information.
     * </p>
     * <p>
     * A null value will work to remove the property.
     * </p>
     *
     * @param name  a {@code String} the name of the property.
     * @param value an {@code Object} may be null
     */
    public void setProperty(String name, Object value)
    {
        if (value != null)
        {
            properties.put(name, value);
        }
        else
        {
            properties.remove(name);
        }
    }

    protected Map<String, Object> properties()
    {
        return properties;
    }

}