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

import org.junit.Test;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.objectweb.asm.Opcodes.*;

public class ConstructorReplacementTest extends BaseTest
{
    @Test
    @SuppressWarnings("unchecked")
    public void withInterleavedCopies() throws Exception
    {
        // check that the constructor replacement is able to replace interleaved elements in the stack
        // obs.: the java compiler doesn't usually produce code like this
        // regular java compiler will do:
        // -- { new dup dup ... <init> }
        // this tests what happens if:
        // -- { new dup push_1 swap ... <init> pop }
        MethodNode mv = new MethodNode(ACC_PUBLIC, "apply", "(Ljava/lang/Object;)Ljava/lang/Object;", null, new String[]{ "java/lang/Exception" });
        mv.visitTypeInsn(NEW, "java/lang/Integer");
        mv.visitInsn(DUP);
        // interleaving copies in the stack
        mv.visitIntInsn(SIPUSH, 1);
        mv.visitInsn(SWAP);
        // stack: { uobj, int, uobj }
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, "java/lang/String");
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(Ljava/lang/String;)V", false);
        // stack: { uobj, int }
        mv.visitInsn(POP);
        // stack: { uobj }
        mv.visitInsn(ARETURN);
        mv.visitMaxs(4, 2);

        // without replacement
        {
            final ClassNode cn = createClassNode(Function.class, null);
            mv.accept(cn);
            assertEquals(101, createClass(Function.class, cn).apply("101"));
        }

        // with replacement
        {
            final ClassNode cn = createClassNode(Function.class, null);
            new Transformer().replaceObjectInitialization(cn, mv,
                    new HashMap<>(), new FrameAnalyzer().analyze(cn.name, mv));
            mv.accept(cn);
            // DevDebug.debugSaveTrace(cn.name, cn);
            assertEquals(101, createClass(Function.class, cn).apply("101"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void withMultipleInterleavedCopies() throws Exception
    {
        // check that the constructor replacement is able to replace interleaved elements in the stack
        // obs.: the java compiler doesn't usually produce code like this
        MethodNode mv = new MethodNode(ACC_PUBLIC, "apply", "(Ljava/lang/Object;)Ljava/lang/Object;", null, new String[]{ "java/lang/Exception" });
        mv.visitTypeInsn(NEW, "java/lang/Integer");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ASTORE, 2);
        // interleaving copies in the stack
        mv.visitVarInsn(ALOAD, 2);
        mv.visitIntInsn(SIPUSH, 1);
        mv.visitIntInsn(SIPUSH, 2);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitLdcInsn(1.0d);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitLdcInsn(1L);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitIntInsn(SIPUSH, 2);
        mv.visitVarInsn(ALOAD, 2);
        // stack: { uobj, int, uobj }
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, "java/lang/String");
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(Ljava/lang/String;)V", false);
        // stack: { uobj, int }
        mv.visitInsn(POP);
        mv.visitInsn(POP);
        mv.visitInsn(POP2);
        mv.visitInsn(POP);
        mv.visitInsn(POP2);
        mv.visitInsn(POP);
        mv.visitInsn(POP);
        mv.visitInsn(POP);
        mv.visitInsn(POP);
        // stack: { uobj }
        mv.visitInsn(ARETURN);
        mv.visitMaxs(16, 3);

        // without replacement
        {
            final ClassNode cn = createClassNode(Function.class, null);
            mv.accept(cn);
            assertEquals(101, createClass(Function.class, cn).apply("101"));
        }

        // with replacement
        {
            final ClassNode cn = createClassNode(Function.class, null);
            new Transformer().replaceObjectInitialization(cn, mv,
                    new HashMap<>(), new FrameAnalyzer().analyze(cn.name, mv));
            mv.accept(cn);
             DevDebug.debugSaveTrace(cn.name, cn);
            assertEquals(101, createClass(Function.class, cn).apply("101"));
        }
    }
}
