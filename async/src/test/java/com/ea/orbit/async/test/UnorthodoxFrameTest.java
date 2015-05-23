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

import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;

import org.junit.Ignore;
import org.junit.Test;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static com.ea.orbit.async.Await.await;
import static org.junit.Assert.*;
import static org.objectweb.asm.Opcodes.*;

// testing scenarios that might have been produced by other bytecode libraries
// sometimes valid bytecode is different from what the java compiler produces.
public class UnorthodoxFrameTest extends BaseTest
{

    // utility method to create arbitrary classes.
    public <T> T createClass(Class<T> superClass, Consumer<ClassVisitor> populate)
    {
        ClassWriter cw = new ClassWriter(0);
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
        cw.visit(52, ACC_PUBLIC, "com/ea/orbit/async/test/Experiment", null, superName, interfaces);
        populate.accept(cw);
        MethodVisitor mv;
        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, superName, "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        cw.visitEnd();
        cw.toByteArray();
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

    // sanity check of the creator
    @Test
    public void testCreateClass() throws Exception
    {
        assertTrue(createClass(ArrayList.class, cv -> {
        }) instanceof List);
        assertEquals("hello", createClass(Callable.class, cv -> {
            MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "call", "()Ljava/lang/Object;", null, new String[]{ "java/lang/Exception" });
            mv.visitCode();
            mv.visitLdcInsn("hello");
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }).call());
    }

    public interface AsyncCallable<T>
    {
        Task<T> call() throws Exception;
    }

    public interface AsyncFunction<T, V>
    {
        Task<T> apply(V v) throws Exception;
    }


    // sanity check of the creator
    @Test
    public void simpleAsyncMethod() throws Exception
    {
        final Task task = createClass(AsyncCallable.class, cw -> {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "call", "()Lcom/ea/orbit/concurrent/Task;", null, new String[]{ "java/lang/Exception" });
            mv.visitCode();
            mv.visitMethodInsn(INVOKESTATIC, "com/ea/orbit/concurrent/Task", "done", "()Lcom/ea/orbit/concurrent/Task;", false);
            mv.visitMethodInsn(INVOKESTATIC, "com/ea/orbit/async/Await", "await", "(Ljava/util/concurrent/CompletableFuture;)Ljava/lang/Object;", false);
            mv.visitInsn(POP);
            mv.visitMethodInsn(INVOKESTATIC, "com/ea/orbit/concurrent/Task", "done", "()Lcom/ea/orbit/concurrent/Task;", false);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }).call();
        assertTrue(task.isDone());
    }


    // sanity check of the creator
    @Test
    @SuppressWarnings("unchecked")
    public void simpleBlockingAsyncMethod() throws Exception
    {
        final Task task = createClass(AsyncFunction.class, cw -> {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "apply", "(Ljava/lang/Object;)Lcom/ea/orbit/concurrent/Task;", null, new String[]{ "java/lang/Exception" });
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "com/ea/orbit/concurrent/Task");
            mv.visitMethodInsn(INVOKESTATIC, "com/ea/orbit/async/Await", "await", "(Ljava/util/concurrent/CompletableFuture;)Ljava/lang/Object;", false);
            mv.visitInsn(POP);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "com/ea/orbit/concurrent/Task");
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 2);
            mv.visitEnd();
        }).apply(getBlockedTask("hello"));
        assertFalse(task.isDone());
        completeFutures();
        assertEquals("hello", task.join());
    }


    public static class Experiment implements AsyncFunction
    {

        @Override
        public Task apply(final Object o) throws Exception
        {
            new SomeObject(await((Task) o));
            return (Task) o;
        }
    }

    public static class SomeObject
    {
        public SomeObject(final Object obj)
        {
        }
    }

    // sanity check of the creator
    @Test
    @SuppressWarnings("unchecked")
    public void regularConstructorCall() throws Exception
    {
        final Task task = createClass(AsyncFunction.class, cw -> {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "apply", "(Ljava/lang/Object;)Lcom/ea/orbit/concurrent/Task;", null, new String[]{ "java/lang/Exception" });
            mv.visitCode();
            mv.visitTypeInsn(NEW, "java/lang/Integer");
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "com/ea/orbit/concurrent/Task");
            mv.visitMethodInsn(INVOKESTATIC, "com/ea/orbit/async/Await", "await", "(Ljava/util/concurrent/CompletableFuture;)Ljava/lang/Object;", false);
            mv.visitTypeInsn(CHECKCAST, "java/lang/String");
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(Ljava/lang/String;)V", false);
            mv.visitMethodInsn(INVOKESTATIC, "com/ea/orbit/concurrent/Task", "fromValue", "(Ljava/lang/Object;)Lcom/ea/orbit/concurrent/Task;",  false);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(3, 2);
            mv.visitEnd();
        }).apply(getBlockedTask("100"));
        assertFalse(task.isDone());
        completeFutures();
        assertEquals(100, task.join());
    }

    @Test
    @SuppressWarnings("unchecked")
    @Ignore
    // TODO: create a fix for this condition
    public void uninitializedStore() throws Exception
    {
        // check what happens when the uninitialized object is stored in a local variable
        final Task task = createClass(AsyncFunction.class, cw -> {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "apply", "(Ljava/lang/Object;)Lcom/ea/orbit/concurrent/Task;", null, new String[]{ "java/lang/Exception" });
            mv.visitCode();
            mv.visitTypeInsn(NEW, "java/lang/Integer");
            mv.visitInsn(DUP);

            // this is valid bytecode: storing the uninitialized object
            mv.visitInsn(DUP);
            mv.visitVarInsn(ASTORE, 2);

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "com/ea/orbit/concurrent/Task");
            mv.visitMethodInsn(INVOKESTATIC, "com/ea/orbit/async/Await", "await", "(Ljava/util/concurrent/CompletableFuture;)Ljava/lang/Object;", false);
            mv.visitTypeInsn(CHECKCAST, "java/lang/String");
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(Ljava/lang/String;)V", false);

            // discarding the object and getting the one that was stored (without instrumentation they are the same)
            mv.visitInsn(POP);
            mv.visitVarInsn(ALOAD, 2);

            mv.visitMethodInsn(INVOKESTATIC, "com/ea/orbit/concurrent/Task", "fromValue", "(Ljava/lang/Object;)Lcom/ea/orbit/concurrent/Task;", false);

            mv.visitInsn(ARETURN);
            mv.visitMaxs(4, 3);
            mv.visitEnd();
        }).apply(getBlockedTask("101"));
        assertFalse(task.isDone());
        completeFutures();
        assertEquals(101, task.join());
    }

    @Test
    @SuppressWarnings("unchecked")
    @Ignore
    // TODO: create a fix for this condition
    public void uninitializedInTheStack() throws Exception
    {
        // check what happens when the uninitialized object is stored in a local variable
        final Task task = createClass(AsyncFunction.class, cw -> {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "apply", "(Ljava/lang/Object;)Lcom/ea/orbit/concurrent/Task;", null, new String[]{ "java/lang/Exception" });
            mv.visitCode();
            mv.visitTypeInsn(NEW, "java/lang/Integer");
            mv.visitInsn(DUP);

            // this is valid bytecode: creating a 3rd copy of the uninitialized object
            mv.visitInsn(DUP);

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "com/ea/orbit/concurrent/Task");
            mv.visitMethodInsn(INVOKESTATIC, "com/ea/orbit/async/Await", "await", "(Ljava/util/concurrent/CompletableFuture;)Ljava/lang/Object;", false);
            mv.visitTypeInsn(CHECKCAST, "java/lang/String");
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(Ljava/lang/String;)V", false);

            // discarding the object and getting the one that is in the stack (without instrumentation they are the same)
            mv.visitInsn(POP);

            mv.visitMethodInsn(INVOKESTATIC, "com/ea/orbit/concurrent/Task", "fromValue", "(Ljava/lang/Object;)Lcom/ea/orbit/concurrent/Task;", false);

            mv.visitInsn(ARETURN);
            mv.visitMaxs(4, 3);
            mv.visitEnd();
        }).apply(getBlockedTask("101"));
        assertFalse(task.isDone());
        completeFutures();
        assertEquals(101, task.join());
    }
}
