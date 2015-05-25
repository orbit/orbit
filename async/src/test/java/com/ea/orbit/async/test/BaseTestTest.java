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

import org.junit.Test;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.Assert.*;
import static org.objectweb.asm.Opcodes.*;


public class BaseTestTest extends BaseTest
{

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

}
