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

import com.ea.orbit.async.instrumentation.FrameAnalyzer.ExtendedValue;
import com.ea.orbit.async.test.BaseTest;

import org.junit.Test;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import static org.junit.Assert.*;
import static org.objectweb.asm.Opcodes.*;

public class AnalyserTest extends BaseTest
{

    @Test
    public void uninitializedInTheStack() throws Exception
    {
        MethodNode mv = new MethodNode(ACC_PUBLIC, "apply", "(Ljava/lang/String;)Ljava/lang/Integer;", null, null);
        mv.visitTypeInsn(NEW, "java/lang/Integer");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 1);
        // insn 3
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(Ljava/lang/String;)V", false);
        // insn 4
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 2);

        Frame<BasicValue>[] frames = new FrameAnalyzer().analyze("com/ea/orbit/async/Bogus", mv);
        assertEquals(3, frames[3].getStackSize());
        assertTrue(((ExtendedValue) frames[3].getStack(0)).isUninitialized());
        assertFalse(((ExtendedValue) frames[4].getStack(0)).isUninitialized());
    }


    @Test
    public void uninitializedInTheStackCheckOrder() throws Exception
    {
        MethodNode mv = new MethodNode(ACC_PUBLIC, "apply", "(Ljava/lang/String;)Ljava/lang/Integer;", null, null);
        mv.visitIntInsn(SIPUSH, 99);
        mv.visitTypeInsn(NEW, "java/lang/Integer");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 1);
        // insn 4
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(Ljava/lang/String;)V", false);
        // insn 5
        mv.visitInsn(SWAP);
        mv.visitInsn(POP);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(4, 2);

        Frame<BasicValue>[] frames = new FrameAnalyzer().analyze("com/ea/orbit/async/Bogus", mv);
        assertEquals(3, frames[3].getStackSize());
        assertTrue(((ExtendedValue) frames[4].getStack(1)).isUninitialized());
        assertFalse(((ExtendedValue) frames[5].getStack(1)).isUninitialized());
    }

    @Test
    public void uninitializedInTheLocals() throws Exception
    {
        MethodNode mv = new MethodNode(ACC_PUBLIC, "apply", "(Ljava/lang/String;)Ljava/lang/Integer;", null, null);
        mv.visitIntInsn(SIPUSH, 99);
        mv.visitTypeInsn(NEW, "java/lang/Integer");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 1);
        // insn 6
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(Ljava/lang/String;)V", false);
        // insn 7
        mv.visitInsn(SWAP);
        mv.visitInsn(POP);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(4, 3);

        Frame<BasicValue>[] frames = new FrameAnalyzer().analyze("com/ea/orbit/async/Bogus", mv);
        assertEquals(3, frames[3].getStackSize());
        // local is now initialized
        assertTrue(((ExtendedValue) frames[6].getStack(1)).isUninitialized());
        assertTrue(((ExtendedValue) frames[6].getLocal(2)).isUninitialized());
        // local is now initialized
        assertFalse(((ExtendedValue) frames[7].getStack(1)).isUninitialized());
        assertFalse(((ExtendedValue) frames[7].getLocal(2)).isUninitialized());
    }

    @Test
    public void branchAnalysisWithFrames() throws AnalyzerException
    {
        MethodNode mv = new MethodNode(ACC_PUBLIC, "apply", "(I)Ljava/lang/Object;", null, null);

        mv.visitCode();
        mv.visitInsn(ICONST_1);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(ICONST_3);
        Label l0 = new Label();
        mv.visitJumpInsn(IF_ICMPNE, l0);
        mv.visitLdcInsn(new Double("2.0"));
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitLabel(l0);
        mv.visitFrame(F_FULL, 3, new Object[]{ "com/ea/orbit/async/Bogus", INTEGER, "java/lang/Number" }, 0, new Object[0]);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 3);
        mv.visitEnd();
        Frame<BasicValue>[] frames = new FrameAnalyzer().analyze("com/ea/orbit/async/Bogus", mv);
        // when the frameNode is not used by the analyzer this will return java/lang/Object
        Frame<BasicValue> frame = frames[frames.length - 1];
        assertEquals("com/ea/orbit/async/Bogus", frame.getLocal(0).getType().getInternalName());
        assertEquals("I", frame.getLocal(1).getType().toString());
        assertEquals("java/lang/Number", frame.getLocal(2).getType().getInternalName());
    }


    @Test
    public void exceptionTest() throws AnalyzerException
    {
        MethodNode mv = new MethodNode(ACC_PUBLIC, "doIt", "(Lcom/ea/orbit/concurrent/Task;)Lcom/ea/orbit/concurrent/Task;", "(Lcom/ea/orbit/concurrent/Task<*>;)Lcom/ea/orbit/concurrent/Task<*>;", null);
        {
            Label l0 = new Label();
            Label l1 = new Label();
            Label l2 = new Label();
            mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
            mv.visitLabel(l0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESTATIC, "com/ea/orbit/async/Await", "await", "(Ljava/util/concurrent/CompletableFuture;)Ljava/lang/Object;", false);
            mv.visitInsn(POP);
            mv.visitLabel(l1);
            Label l3 = new Label();
            mv.visitJumpInsn(GOTO, l3);
            mv.visitLabel(l2);
            mv.visitFrame(Opcodes.F_FULL,
                    2, new Object[]{ "com/ea/orbit/async/test/SmallestTest", "com/ea/orbit/concurrent/Task" },
                    1, new Object[]{ "java/lang/Exception" });
            mv.visitVarInsn(ASTORE, 2);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESTATIC, "com/ea/orbit/async/Await", "await", "(Ljava/util/concurrent/CompletableFuture;)Ljava/lang/Object;", false);
            mv.visitInsn(POP);
            mv.visitLabel(l3);
            mv.visitFrame(Opcodes.F_FULL,
                    2, new Object[]{ "com/ea/orbit/async/test/SmallestTest", "com/ea/orbit/concurrent/Task" },
                    0, new Object[0]);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 3);
        }

        Frame<BasicValue>[] frames = new FrameAnalyzer().analyze("com/ea/orbit/async/test/SmallestTest", mv);
        // when the frameNode is not used by the analyzer this will return java/lang/Object
        Frame<BasicValue> frame = frames[8];
        assertEquals(ASTORE, mv.instructions.get(8).getOpcode());
        assertEquals("com/ea/orbit/async/test/SmallestTest", frame.getLocal(0).getType().getInternalName());
        assertEquals("com/ea/orbit/concurrent/Task", frame.getLocal(1).getType().getInternalName());
        assertEquals("java/lang/Exception", frame.getStack(0).getType().getInternalName());
    }


}
