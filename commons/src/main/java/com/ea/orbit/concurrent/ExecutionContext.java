package com.ea.orbit.concurrent;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ExecutionContext
{
    private static ThreadLocal<LinkedList<ExecutionContext>> contextStacks = new ThreadLocal<>();
    private static AtomicLong nextId = new AtomicLong(1);
    private long id = nextId.getAndIncrement();

    private ConcurrentHashMap<String, Object> properties = new ConcurrentHashMap<>();

    /**
     * Adds this execution context to the top of the context stack for the current thread.
     */
    public void push()
    {
        LinkedList<ExecutionContext> stack = contextStacks.get();
        if (stack == null)
        {
            stack = new LinkedList<>();
            contextStacks.set(stack);
        }
        stack.add(this);
    }

    /**
     * Creates a new execution context and pushes it to the current thread context stack.
     *
     * @return the new execution context
     */
    public static ExecutionContext pushNew()
    {
        final ExecutionContext context = new ExecutionContext();
        context.push();
        return context;
    }

    /**
     * Removes the this execution context from the context stack for the current thread.
     * This will fail with IllegalStateException if the current context is not at the top of the stack.
     */
    public void pop()
    {
        LinkedList<ExecutionContext> stack = contextStacks.get();
        if (stack == null || stack.size() == 0 || stack.getLast() != this)
        {
            throw new IllegalStateException("Invalid execution context stack state: " + stack);
        }
        stack.add(this);
    }

    @Override
    public String toString()
    {
        return "ExecutionContext:" + id;
    }

    /**
     * Gets the current execution context for this thread from the stack.
     *
     * @return the current context or null if there is none.
     */
    public static ExecutionContext current()
    {
        final LinkedList<ExecutionContext> stack = contextStacks.get();
        if (stack == null || stack.size() == 0)
        {
            return null;
        }
        return stack.getLast();
    }

    /**
     * Wraps a Runnable in such a way the it will push the current execution context before any code gets executed and pop it afterwards
     *
     * @param w the functional interface to be wrapped
     * @return wrapped object if there is a current execution context, or the same object if not.
     */
    public static Runnable wrap(Runnable w)
    {
        ExecutionContext c = current();
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
        ExecutionContext c = current();
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
        ExecutionContext c = current();
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
        ExecutionContext c = current();
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
        ExecutionContext c = current();
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
        ExecutionContext c = current();
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


}
