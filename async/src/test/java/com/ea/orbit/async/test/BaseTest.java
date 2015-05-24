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

package com.ea.orbit.async.test;

import com.ea.orbit.async.Await;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;
import com.ea.orbit.util.StringUtils;

import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.objectweb.asm.Opcodes.*;


public class BaseTest
{
    static
    {
        Await.init();
    }

    // pairs of completable futures and the future completions.
    protected Queue<Pair<CompletableFuture, Object>> blockedFutures = new LinkedList<>();

    // just calls a function
    public <T> Task<T> futureFrom(Supplier<Task<T>> supplier)
    {
        return supplier.get();
    }

    /**
     * Creates and an uncompleted future and adds it the the queue for later completion.
     * To help with the tests
     */
    public <T> CompletableFuture<T> getBlockedFuture(T value)
    {
        final CompletableFuture<T> future = new CompletableFuture<>();
        blockedFutures.add(Pair.of(future, value));
        return future;
    }

    public <T> CompletableFuture<T> getBlockedFuture()
    {
        return getBlockedFuture(null);
    }

    public <T> Task<T> getBlockedTask(T value)
    {
        final Task<T> future = new Task<>();
        blockedFutures.add(Pair.of(future, value));
        return future;
    }


    public <T> Task<T> getBlockedTask()
    {
        return getBlockedTask(null);
    }

    /**
     * Complete all the blocked futures, even new ones created while executing this method
     */
    public void completeFutures()
    {
        while (blockedFutures.size() > 0)
        {
            final Pair<CompletableFuture, Object> pair = blockedFutures.poll();
            if (pair != null)
            {
                pair.getKey().complete(pair.getValue());
            }
        }
    }

    /**
     * Shortcut to create tests with lambda functions
     */
    protected <T, V> Task<V> call(T t, Function<T, Task<V>> function)
    {
        return function.apply(t);
    }

    /**
     * Shortcut to create tests with lambda functions
     */
    protected <V> Task<V> call(Callable<Task<V>> function) throws Exception
    {
        return function.call();
    }


    // utility method to create arbitrary classes.
    protected <T> T createClass(Class<T> superClass, Consumer<ClassVisitor> populate)
    {
        ClassNode cn = createClassNode(superClass, populate);
        return createClass(superClass, cn);
    }

    protected <T> T createClass(final Class<T> superClass, final ClassNode cn)
    {
        ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        final byte[] bytes = cw.toByteArray();
        // perhaps we should use the source class ClassLoader as parent.
        class Loader extends ClassLoader
        {
            Loader()
            {
                super(superClass.getClassLoader());
            }

            public Class<?> define(final String o, final byte[] bytes)
            {
                return super.defineClass(o, bytes, 0, bytes.length);
            }
        }
        //noinspection unchecked
        try
        {
            return (T) new Loader().define(null, bytes).newInstance();
        }
        catch (Exception e)
        {
            throw new UncheckedException(e);
        }
    }

    protected <T> ClassNode createClassNode(final Class<T> superClass, final Consumer<ClassVisitor> populate)
    {
        ClassNode cn = new ClassNode();
        String[] interfaces = null;
        Type superType;
        if (superClass.isInterface())
        {
            superType = Type.getType(Object.class);
            interfaces = new String[]{ Type.getType(superClass).getInternalName() };
        }
        else
        {
            superType = Type.getType(superClass);
        }
        String superName = superType.getInternalName();
        StackTraceElement caller = new Exception().getStackTrace()[1];
        final String name = "Experiment" + StringUtils.capitalize(caller.getMethodName()) + caller.getLineNumber();

        cn.visit(52, ACC_PUBLIC, Type.getType(getClass()).getInternalName() + "$" + name, null, superName, interfaces);
        if (populate != null)
        {
            populate.accept(cn);
        }
        MethodVisitor mv;
        {
            mv = cn.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, superName, "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        cn.visitEnd();
        return cn;
    }


    public interface AsyncCallable<T>
    {
        Task<T> call() throws Exception;
    }

    public interface AsyncFunction<T, V>
    {
        Task<T> apply(V v) throws Exception;
    }
}
