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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;

/**
 * Internal class to modify the bytecodes of async-await methods to
 * make them behave in the expected fashion.
 *
 * @author Daniel Sperry
 */
class Transformer implements ClassFileTransformer
{
    /**
     * Name of the property that will be set by the
     * Agent to flag that the instrumentation is already running.
     *
     * There are two definitions of this constant because the initialization
     * classes are not supposed be accessed by the agent classes, and vice-versa
     *
     * @see com.ea.orbit.async.instrumentation.InitializeAsync#ORBIT_ASYNC_RUNNING
     * @see com.ea.orbit.async.instrumentation.Transformer#ORBIT_ASYNC_RUNNING
     */
    // there is a test case that asserts that these constants contain the same value.
    static final String ORBIT_ASYNC_RUNNING = "orbit-async.running";

    private static final String ASYNC_DESCRIPTOR = "Lcom/ea/orbit/async/Async;";
    private static final String AWAIT_NAME = "com/ea/orbit/async/Await";

    public static final String AWAIT_METHOD_DESC = "(Ljava/util/concurrent/CompletableFuture;)Ljava/lang/Object;";
    public static final String AWAIT_METHOD_NAME = "await";

    private static final Type ASYNC_STATE_TYPE = Type.getType("Lcom/ea/orbit/async/runtime/AsyncAwaitState;");
    private static final String ASYNC_STATE_NAME = ASYNC_STATE_TYPE.getInternalName();

    private static final String COMPLETABLE_FUTURE_DESCRIPTOR = "Ljava/util/concurrent/CompletableFuture;";
    private static final Type COMPLETABLE_FUTURE_TYPE = Type.getType(COMPLETABLE_FUTURE_DESCRIPTOR);
    private static final String COMPLETABLE_FUTURE_RET = ")Ljava/util/concurrent/CompletableFuture;";
    private static final String COMPLETABLE_FUTURE_NAME = "java/util/concurrent/CompletableFuture";

    private static final Type COMPLETION_STAGE_TYPE = Type.getType("Ljava/util/concurrent/CompletionStage;");

    private static final Type OBJECT_TYPE = Type.getType(Object.class);
    private static final String _THIS = "_this";
    private static final String DYN_FUNCTION = "(" + ASYNC_STATE_TYPE.getDescriptor() + ")Ljava/util/function/Function;";

    private static final String TASK_DESCRIPTOR = "Lcom/ea/orbit/concurrent/Task;";
    private static final String TASK_RET = ")Lcom/ea/orbit/concurrent/Task;";
    private static final String TASK_NAME = "com/ea/orbit/concurrent/Task";

    @Override
    public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined, final ProtectionDomain protectionDomain, final byte[] classfileBuffer) throws IllegalClassFormatException
    {
        try
        {
            if (className.startsWith("java"))
            {
                return null;
            }
            ClassReader cr = new ClassReader(classfileBuffer);
            if (needsInstrumentation(cr))
            {
                return transform(cr);
            }
            return null;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Does the actual instrumentation generating new bytecode
     *
     * @param cr the class reader for this class
     * @return null or the new class bytes
     */
    public byte[] transform(ClassReader cr) throws AnalyzerException
    {
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        int countInstrumented = 0;

        for (MethodNode mn : (List<MethodNode>) new ArrayList(cn.methods))
        {
            boolean taskReturn = mn.desc.endsWith(TASK_RET);
            if (!taskReturn && !mn.desc.endsWith(COMPLETABLE_FUTURE_RET))
            {
                // method must return completable future or task
                continue;
            }
            boolean hasAwaitCall = false;
            // checks if the method has a call to await()
            for (Iterator it = mn.instructions.iterator(); it.hasNext(); )
            {
                Object o = it.next();
                if (o instanceof MethodInsnNode && isAwaitCall((MethodInsnNode) o))
                {
                    hasAwaitCall = true;
                    break;
                }
            }
            if (!hasAwaitCall)
            {
                continue;
            }
            if (!taskReturn && (mn.visibleAnnotations == null
                    || !mn.visibleAnnotations.stream().anyMatch(a -> ((AnnotationNode) a).desc.equals(ASYNC_DESCRIPTOR))))
            {
                continue;
            }
            countInstrumented++;
            // for development use: printMethod(cn, mn);
            final MethodNode mv = new MethodNode(Opcodes.ACC_PRIVATE | ACC_STATIC,
                    mn.name, mn.desc, mn.signature, (String[]) mn.exceptions.toArray(new String[mn.exceptions.size()]));
            mn.accept(mv);
            // TODO generate better names, reuse names if possible
            mv.name = mn.name + "$" + countInstrumented;
            mv.desc = Type.getMethodDescriptor(COMPLETABLE_FUTURE_TYPE, ASYNC_STATE_TYPE, OBJECT_TYPE);
            mv.signature = null;

            Analyzer analyzer = new Analyzer(new TypeInterpreter());
            Frame[] frames = analyzer.analyze(cn.name, mn);
            final AbstractInsnNode[] instructions = mn.instructions.toArray();

            mv.instructions.clear();

            // get pos
            mv.visitVarInsn(ALOAD, !Modifier.isStatic(mv.access) ? 1 : 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, ASYNC_STATE_NAME, "getPos", Type.getMethodDescriptor(Type.INT_TYPE), false);

            final Label defaultLabel = new Label();
            mv.visitTableSwitchInsn(0, 0, defaultLabel, new Label[0]);
            TableSwitchInsnNode switchIns = (TableSwitchInsnNode) mv.instructions.getLast();
            final Label entryPointLabel = new Label();

            // original entry point

            mv.visitLabel(entryPointLabel);
            switchIns.labels.add((LabelNode) mv.instructions.getLast());
            restoreStackAndLocals(frames[0], mv);
            Label lastRestorePoint = null;
            for (int i = 0; i < instructions.length; i++)
            {
                final AbstractInsnNode ins = instructions[i];
                final Frame frame = frames[i];

                if ((!(ins instanceof MethodInsnNode)) || (!isAwaitCall((MethodInsnNode) ins)))
                {
                    ins.accept(mv);
                    continue;
                }

                // TODO: other comparisons.
                mv.visitInsn(Opcodes.DUP);
                // stack: completableFuture completableFuture

                // original: Await.await(future)  (that by default does: future.join())

                // turns into:

                // code: if(!future.isDone()) {
                // code:    saveLocals to new state
                // code:    saveStack to new state
                // code:    return future.exceptionally(nop).thenCompose(x -> _func(x, state));
                // code: }
                // code: jump futureIsDoneLabel:
                // code: resumeLabel:
                // code:    restoreStack;
                // code:    restoreLocals;
                // code: futureIsDone:
                // code: future.join():

                Label futureIsDoneLabel = new Label();

                mv.visitMethodInsn(INVOKEVIRTUAL, COMPLETABLE_FUTURE_NAME, "isDone", "()Z", false);
                // code: jump futureIsDoneLabel:
                mv.visitJumpInsn(Opcodes.IFNE, futureIsDoneLabel);

                // code:    saveStack to new state
                // code:    saveLocals to new state
                int offset = saveLocals(switchIns.labels.size(), frame, mv);
                // stack { .. future state }

                // clears the stack and leaves asyncState in the top
                saveStack(frame, mv);
                // stack: { state }
                mv.visitInsn(DUP);
                // stack: { state state }
                mv.visitIntInsn(SIPUSH, offset);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ASYNC_STATE_NAME, "getObj", Type.getMethodDescriptor(OBJECT_TYPE, Type.INT_TYPE), false);
                mv.visitTypeInsn(CHECKCAST, COMPLETABLE_FUTURE_NAME);

                // stack: { state future }
                mv.visitMethodInsn(INVOKESTATIC, Type.getType(Function.class).getInternalName(), "identity", "()Ljava/util/function/Function;", false);
                // stack: { state future function }
                mv.visitMethodInsn(INVOKEVIRTUAL, COMPLETABLE_FUTURE_NAME, "exceptionally", "(Ljava/util/function/Function;)Ljava/util/concurrent/CompletableFuture;", false);
                // stack: { state new_future }

                // code:    return future.exceptionally(x -> x).thenCompose(x -> _func(state));

                mv.visitInsn(SWAP);
                // stack: { new_future state}

                mv.visitInvokeDynamicInsn("apply", DYN_FUNCTION,
                        new Handle(Opcodes.H_INVOKESTATIC,
                                "java/lang/invoke/LambdaMetafactory",
                                "metafactory",
                                "(Ljava/lang/invoke/MethodHandles$Lookup;"
                                        + "Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                                        + "Ljava/lang/invoke/MethodType;"
                                        + "Ljava/lang/invoke/MethodHandle;"
                                        + "Ljava/lang/invoke/MethodType;"
                                        + ")Ljava/lang/invoke/CallSite;"),
                        Type.getType("(Ljava/lang/Object;)Ljava/lang/Object;"),
                        new Handle(Opcodes.H_INVOKESTATIC, cn.name, mv.name, mv.desc),
                        Type.getType("(Ljava/lang/Object;)Ljava/util/concurrent/CompletableFuture;"));


                // stack: { new_future function }
                mv.visitMethodInsn(INVOKEVIRTUAL, COMPLETABLE_FUTURE_NAME, "thenCompose", "(Ljava/util/function/Function;)Ljava/util/concurrent/CompletableFuture;", false);
                // stack: { new_future_02 }
                mv.visitInsn(ARETURN);

                // code: resumeLabel:
                Label resumeLabel = new Label();
                mv.visitLabel(resumeLabel);
                switchIns.labels.add((LabelNode) mv.instructions.getLast());

                // code:    restoreStack;
                // code:    restoreLocals;
                restoreStackAndLocals(frame, mv);
                if (!Modifier.isStatic(mn.access))
                {
                    if (lastRestorePoint != null)
                    {
                        mv.visitLocalVariable(_THIS, "L" + cn.name + ";", null, lastRestorePoint, resumeLabel, 0);
                    }
                    lastRestorePoint = new Label();
                    mv.visitLabel(lastRestorePoint);
                }

                // code: futureIsDone:
                mv.visitLabel(futureIsDoneLabel);
                mv.visitMethodInsn(INVOKEVIRTUAL, COMPLETABLE_FUTURE_NAME, "join", "()Ljava/lang/Object;", false);

            }
            switchIns.max = switchIns.labels.size() - 1;

            mv.visitLabel(defaultLabel);
            mv.visitTypeInsn(NEW, "java/lang/IllegalArgumentException");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "()V", false);
            mv.visitInsn(ATHROW);
            if (!Modifier.isStatic(mn.access))
            {
                if (lastRestorePoint != null)
                {
                    Label endLabel = new Label();
                    mv.visitLabel(endLabel);
                    mv.visitLocalVariable(_THIS, "L" + cn.name + ";", null, lastRestorePoint, endLabel, 0);
                }
            }
            mv.accept(cn);

            {
                mv.maxLocals = Math.max(8, mv.maxLocals + 4);
                mv.maxStack = Math.max(8, mv.maxStack + 4);
                // for development use: printMethod(cn, mv);
            }

            mn.instructions.clear();
            // can't be done: mn.access |= ACC_SYNTHETIC;
            mn.tryCatchBlocks.clear();

            saveLocals(0, frames[0], mn);
            mn.visitInsn(ACONST_NULL);
            mn.visitMethodInsn(INVOKESTATIC, cn.name, mv.name, mv.desc, false);

            Type futureType = Type.getReturnType(mn.desc);
            if (!COMPLETABLE_FUTURE_TYPE.equals(futureType)
                    && futureType.getInternalName().equals(TASK_NAME))
            {
                mn.visitMethodInsn(INVOKESTATIC,
                        futureType.getInternalName(),
                        "from",
                        Type.getMethodDescriptor(futureType, COMPLETION_STAGE_TYPE),
                        false);
            }
            mn.visitInsn(ARETURN);

            {
                mn.maxStack = 8;
                // for development use: printMethod(cn, mn);
                // for development use: printMethod(cn, mv);
            }
        }
        if (countInstrumented == 0)
        {
            return null;
        }

        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES)
        {
            @Override
            protected String getCommonSuperClass(final String type1, final String type2)
            {
                return "java/lang/Object";
            }
        };
        cn.accept(cw);
        byte[] bytes = cw.toByteArray();
        {
            // for development use: new ClassReader(bytes).accept(new TraceClassVisitor(new PrintWriter(System.out)), ClassReader.EXPAND_FRAMES);
        }
        return bytes;
    }

    private boolean isAwaitCall(final MethodInsnNode methodIns)
    {
        return methodIns.getOpcode() == Opcodes.INVOKESTATIC
                && AWAIT_METHOD_NAME.equals(methodIns.name)
                && AWAIT_NAME.equals(methodIns.owner)
                && AWAIT_METHOD_DESC.equals(methodIns.desc);
    }

    private void printMethod(final ClassNode cn, final MethodNode mv)
    {
        try
        {
            Analyzer analyzer2 = new Analyzer(new TypeInterpreter());
            Frame[] frames2;
            try
            {
                frames2 = analyzer2.analyze(cn.superName, mv);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                frames2 = null;
            }
            final AbstractInsnNode[] isns2 = mv.instructions.toArray();
            final PrintWriter pw = new PrintWriter(System.out);
            pw.println("method " + mv.name + mv.desc);
            Textifier p = new Textifier();
            final TraceMethodVisitor tv = new TraceMethodVisitor(p);

            for (int i = 0; i < isns2.length; i++)
            {
                Frame frame;
                if (frames2 != null && (frame = frames2[i]) != null)
                {
                    p.getText().add("Locals: [ ");
                    for (int x = 0; x < frame.getLocals(); x++)
                    {
                        p.getText().add(String.valueOf(((BasicValue) frame.getLocal(x)).getType()));
                        p.getText().add(" ");
                    }
                    p.getText().add("]\r\n");
                    p.getText().add("Stack: [ ");
                    for (int x = 0; x < frame.getStackSize(); x++)
                    {
                        p.getText().add(String.valueOf(((BasicValue) frame.getStack(x)).getType()));
                        p.getText().add(" ");
                    }
                    p.getText().add("]\r\n");
                }
                p.getText().add(i + ": ");
                isns2[i].accept(tv);
            }
            p.print(pw);
            pw.println();
            pw.flush();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private int saveLocals(int pos, Frame frame, MethodNode mv)
    {
        mv.visitTypeInsn(Opcodes.NEW, ASYNC_STATE_NAME);
        mv.visitInsn(Opcodes.DUP);
        mv.visitIntInsn(SIPUSH, pos);
        mv.visitIntInsn(SIPUSH, frame.getLocals());
        mv.visitIntInsn(SIPUSH, frame.getStackSize());
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, ASYNC_STATE_NAME, "<init>", "(III)V", false);
        int count = 0;
        for (int i = 0; i < frame.getLocals(); i++)
        {
            final BasicValue value = (BasicValue) frame.getLocal(i);
            if (value.getType() != null)
            {
                count++;
                varInsn(mv, value.getType(), false, i);

                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ASYNC_STATE_NAME, "push",
                        Type.getMethodDescriptor(ASYNC_STATE_TYPE, value.isReference() ? OBJECT_TYPE : value.getType()), false);
            }
        }
        return count;
    }

    private void saveStack(Frame frame, MethodNode mv)
    {
        // stack: { ... async_state }
        for (int i = frame.getStackSize(); --i >= 0; )
        {
            final BasicValue value = (BasicValue) frame.getStack(i);
            if (value.getType() != null)
            {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, ASYNC_STATE_NAME, "push",
                        Type.getMethodDescriptor(ASYNC_STATE_TYPE, value.isReference() ? OBJECT_TYPE : value.getType(), ASYNC_STATE_TYPE), false);
            }
        }
        // stack: { async_state }
    }

    private void restoreStackAndLocals(final Frame frame, final MethodNode mv)
    {
        // local_0 = async_state

        // count locals+stack
        int localsPlusStack = 0;
        final int firstLocal = 0;
        // counting locals, counts double and long only once (they use two slots)
        for (int i = firstLocal; i < frame.getLocals(); i++)
        {
            BasicValue local = (BasicValue) frame.getLocal(i);
            if (local != null && local != BasicValue.UNINITIALIZED_VALUE && local.getType() != null)
            {
                localsPlusStack++;
            }
        }
        // counting stack, counts double and long only once (they use two slots)
        for (int i = 0; i < frame.getStackSize(); i++)
        {
            BasicValue local = (BasicValue) frame.getStack(i);
            if (local != null && local.getType() != null)
            {
                localsPlusStack++;
            }
        }

        // local_0 = async_state
        // restore stack
        // stack: { }
        for (int i = 0; i < frame.getStackSize(); i++)
        {
            BasicValue local = (BasicValue) frame.getStack(i);
            if (local != null && local.getType() != null)
            {
                final int idx = --localsPlusStack;
                mv.visitVarInsn(Opcodes.ALOAD, firstLocal);
                mv.visitIntInsn(Opcodes.BIPUSH, idx);

                if (local.isReference())
                {
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ASYNC_STATE_NAME, "getObj", Type.getMethodDescriptor(OBJECT_TYPE, Type.INT_TYPE), false);
                    mv.visitTypeInsn(CHECKCAST, local.getType().getInternalName());
                }
                else
                {
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ASYNC_STATE_NAME, "get" + local.getType(), Type.getMethodDescriptor(local.getType(), Type.INT_TYPE), false);
                }
            }
        }
        // stack: { ... }
        // local_0 = async_state
        // restore local vars
        // from last to first to hold async_state until the very end
        for (int i = frame.getLocals(); --i >= firstLocal; )
        {
            BasicValue local = (BasicValue) frame.getLocal(i);
            if (local != null && local != BasicValue.UNINITIALIZED_VALUE)
            {
                if (local.getType() != null)
                {
                    final int idx = --localsPlusStack;
                    // dup the state
                    mv.visitVarInsn(Opcodes.ALOAD, firstLocal);
                    mv.visitIntInsn(Opcodes.BIPUSH, idx);
                    if (local.isReference())
                    {
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ASYNC_STATE_NAME, "getObj", Type.getMethodDescriptor(OBJECT_TYPE, Type.INT_TYPE), false);
                        mv.visitTypeInsn(CHECKCAST, local.getType().getInternalName());
                    }
                    else
                    {
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ASYNC_STATE_NAME, "get" + local.getType(), Type.getMethodDescriptor(local.getType(), Type.INT_TYPE), false);
                    }
                    varInsn(mv, local.getType(), true, i);
                }
                else
                {
                    throw new RuntimeException();
                    // TODO: double check this, mv.visitVarInsn(ASTORE, i);
                }
            }
        }
        // stack: { ... }
        // locals: { ... }
        // local_0 = (this or ? if static method)
    }

    boolean needsInstrumentation(ClassReader cr)
    {
        try
        {
            // checks the class pool for references
            // to the class com.ea.orbit.async.Await
            // and to the method Await.await(CompletableFuture)Object

            // https://docs.oracle.com/javase/specs/jvms/se8/jvms8.pdf

            boolean hasAwaitCall = false;
            boolean hasAwait = false;
            for (int i = 1, c = cr.getItemCount(); i < c; i++)
            {
                final int address = cr.getItem(i);
                if (address > 0
                        && cr.readByte(address - 1) == 7
                        && equalsUtf8(cr, address, AWAIT_NAME))
                {
                    // CONSTANT_Class_info {
                    //    u1 tag; = 7
                    //    u2 name_index; -> utf8
                    // }
                    hasAwait = true;
                    break;
                }
            }
            // failing fast, no references to Await implies nothing to do.
            if (!hasAwait)
            {
                return false;
            }

            // now checking for method references to Await.await
            for (int i = 1, c = cr.getItemCount(); i < c; i++)
            {
                final int address = cr.getItem(i);
                if (address > 0)
                {
                    // CONSTANT_Methodref_info | CONSTANT_InterfaceMethodref_info {
                    //    u1 tag; = 10 or 11
                    //    u2 class_index; -> class info
                    //    u2 name_and_type_index;
                    //}
                    // CONSTANT_Class_info {
                    //    u1 tag;
                    //    u2 name_index; -> utf8
                    // }
                    // CONSTANT_NameAndType_info {
                    //    u1 tag;
                    //    u2 name_index; -> utf8
                    //    u2 descriptor_index;; -> utf8
                    // }
                    int tag = cr.readByte(address - 1);
                    if (tag == 11 || tag == 10)
                    {
                        int classIndex = cr.readUnsignedShort(address);
                        if (classIndex == 0) continue;
                        int classAddress = cr.getItem(classIndex);

                        int ntIndex = cr.readUnsignedShort(address + 2);
                        if (ntIndex == 0) continue;
                        int ntAddress = cr.getItem(ntIndex);

                        if (equalsUtf8(cr, classAddress, AWAIT_NAME)
                                && equalsUtf8(cr, ntAddress, AWAIT_METHOD_NAME)
                                && equalsUtf8(cr, ntAddress + 2, AWAIT_METHOD_DESC))
                        {
                            hasAwaitCall = true;
                            break;
                        }
                    }
                }
            }
            return hasAwaitCall;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    private boolean equalsUtf8(final ClassReader cr, final int pointerToUtf8Index, final String str)
    {
        int utf8_index = cr.readUnsignedShort(pointerToUtf8Index);
        if (utf8_index == 0)
        {
            return false;
        }

        //  CONSTANT_Utf8_info {
        //    u1 tag;  = 1
        //    u2 length;
        //    u1 bytes[length];
        // }

        int utf8_address = cr.getItem(utf8_index);
        final int utf8_length = cr.readUnsignedShort(utf8_address);
        if (utf8_length == str.length())
        {
            // assuming the str is utf8 "safe", no special chars
            int idx = utf8_address + 2;
            for (int ic = 0; ic < utf8_length; ic++, idx++)
            {
                if (str.charAt(ic) != (char) cr.readByte(idx))
                {
                    return false;
                }
            }
            return true;
        }
        return false;

    }

    boolean needsInstrumentation(final Class<?> c)
    {
        try
        {
            InputStream resourceAsStream = c.getClassLoader().getResourceAsStream(Type.getInternalName(c) + ".class");
            if (resourceAsStream == null)
            {
                return false;
            }
            return needsInstrumentation(new ClassReader(resourceAsStream));
        }
        catch (Throwable ex)
        {
            // ignoring
        }
        return false;
    }


    /**
     * Used to discover the object types that are currently
     * being stored in the stack and in the locals.
     */
    private static class TypeInterpreter extends BasicInterpreter
    {
        @Override
        public BasicValue newValue(Type type)
        {
            if (type != null && type.getSort() == Type.OBJECT)
            {
                return new BasicValue(type);
            }
            return super.newValue(type);
        }

        @Override
        public BasicValue merge(BasicValue v, BasicValue w)
        {
            if (v != w && v != null && w != null && !v.equals(w))
            {
                Type t = ((BasicValue) v).getType();
                Type u = ((BasicValue) w).getType();
                if (t != null && u != null
                        && t.getSort() == Type.OBJECT
                        && u.getSort() == Type.OBJECT)
                {
                    // could find a common super type here, a bit expensive
                    // TODO: test this with an assignment
                    //    like: local1 was CompletableFuture <- store Task
                    return BasicValue.REFERENCE_VALUE;
                }
            }
            return super.merge(v, w);
        }
    }

    /**
     * Emits a var opcode (STORE or LOAD) with the proper operand type.
     *
     * @param mv    the method visitor
     * @param type  the var type
     * @param store true for store, false for load
     * @param local the index of the local variable
     */
    private void varInsn(MethodVisitor mv, Type type, boolean store, int local)
    {
        switch (type.getSort())
        {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                mv.visitVarInsn(store ? ISTORE : ILOAD, local);
                break;
            case Type.FLOAT:
                mv.visitVarInsn(store ? FSTORE : FLOAD, local);
                break;
            case Type.LONG:
                mv.visitVarInsn(store ? LSTORE : LLOAD, local);
                break;
            case Type.DOUBLE:
                mv.visitVarInsn(store ? DSTORE : DLOAD, local);
                break;
            default:
                mv.visitVarInsn(store ? ASTORE : ALOAD, local);
        }
    }

}
