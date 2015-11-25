package com.ea.orbit.concurrent;

import java.util.function.Function;

public interface TaskFunction<T, R> extends Function<T, Task<R>>
{
    @Override
    Task<R> apply(T t);
}
