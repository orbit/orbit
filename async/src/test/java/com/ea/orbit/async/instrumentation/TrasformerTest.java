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

package com.ea.orbit.async.instrumentation;

import com.ea.orbit.async.test.BaseTest;
import com.ea.orbit.concurrent.Task;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;

import static org.junit.Assert.assertEquals;
import static org.objectweb.asm.Opcodes.*;

public class TrasformerTest extends BaseTest
{
    // sanity check of the creator
    @Test
    @SuppressWarnings("unchecked")
    public void simpleAsyncMethod() throws Exception
    {
        final ClassNode cn = createClassNode(AsyncFunction.class, cw -> {
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
        });
        ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        final byte[] bytes = new Transformer().transform(new ClassReader(cw.toByteArray()));
        //DevDebug.debugSaveTrace(cn.name, bytes);
        assertEquals("hello", createClass(AsyncFunction.class, bytes).apply(Task.fromValue("hello")).join());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void withLocals() throws Exception
    {
        final ClassNode cn = createClassNode(AsyncFunction.class, cw -> {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "apply", "(Ljava/lang/Object;)Lcom/ea/orbit/concurrent/Task;", null, new String[]{ "java/lang/Exception" });
            mv.visitCode();
            mv.visitIntInsn(SIPUSH, 1);
            mv.visitVarInsn(ISTORE, 2);

            mv.visitInsn(LCONST_0);
            mv.visitVarInsn(LSTORE, 3);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "com/ea/orbit/concurrent/Task");
            mv.visitMethodInsn(INVOKESTATIC, "com/ea/orbit/async/Await", "await", "(Ljava/util/concurrent/CompletableFuture;)Ljava/lang/Object;", false);
            mv.visitInsn(POP);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "com/ea/orbit/concurrent/Task");
            mv.visitInsn(ARETURN);
            mv.visitMaxs(2, 5);
            mv.visitEnd();
        });
        ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        final byte[] bytes = new Transformer().transform(new ClassReader(cw.toByteArray()));
        // DevDebug.debugSaveTrace(cn.name, bytes);
        assertEquals("hello", createClass(AsyncFunction.class, bytes).apply(Task.fromValue("hello")).join());
    }


    @Test
    @SuppressWarnings("unchecked")
    public void withTwoFutures() throws Exception
    {
        final ClassNode cn = createClassNode(AsyncBiFunction.class, cw -> {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "apply", "(Ljava/lang/Object;Ljava/lang/Object;)Lcom/ea/orbit/concurrent/Task;", null, new String[]{ "java/lang/Exception" });
            mv.visitCode();

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "com/ea/orbit/concurrent/Task");
            mv.visitMethodInsn(INVOKESTATIC, "com/ea/orbit/async/Await", "await", "(Ljava/util/concurrent/CompletableFuture;)Ljava/lang/Object;", false);
            mv.visitInsn(POP);

            mv.visitVarInsn(ALOAD, 2);
            mv.visitTypeInsn(CHECKCAST, "com/ea/orbit/concurrent/Task");
            mv.visitMethodInsn(INVOKESTATIC, "com/ea/orbit/async/Await", "await", "(Ljava/util/concurrent/CompletableFuture;)Ljava/lang/Object;", false);
            mv.visitInsn(POP);

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "com/ea/orbit/concurrent/Task");
            mv.visitInsn(ARETURN);
            mv.visitMaxs(2, 5);
            mv.visitEnd();
        });
        ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        final byte[] bytes = new Transformer().transform(new ClassReader(cw.toByteArray()));
        //DevDebug.debugSaveTrace(cn.name, bytes);

        assertEquals("hello", createClass(AsyncBiFunction.class, bytes).apply(Task.fromValue("hello"), Task.fromValue("world")).join());

        final Task rest = createClass(AsyncBiFunction.class, bytes).apply(getBlockedTask("hello"), getBlockedTask("world"));
        completeFutures();
        assertEquals("hello", rest.join());
    }

}
