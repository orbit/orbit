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

import com.ea.orbit.async.Async;
import com.ea.orbit.async.runtime.AsyncAwaitState;

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
import org.objectweb.asm.tree.InsnList;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
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
    public static final String ASYNC_DESCRIPTOR = "Lcom/ea/orbit/async/Async;";
    public static final String AWAIT_NAME = "com/ea/orbit/async/Await";

    public static final Type ASYNC_STATE_TYPE = Type.getType(AsyncAwaitState.class);
    public static final String ASYNC_STATE_NAME = ASYNC_STATE_TYPE.getInternalName();

    public static final Type COMPLETABLE_FUTURE_TYPE = Type.getType(CompletableFuture.class);
    public static final String COMPLETABLE_FUTURE_NAME = "java/util/concurrent/CompletableFuture";

    public static final Type OBJECT_TYPE = Type.getType(Object.class);
    public static final String _THIS = "_this";
    private static final String DYN_FUNCTION = "(" + ASYNC_STATE_TYPE.getDescriptor() + ")Ljava/util/function/Function;";

    static CompletableFuture initialized = new CompletableFuture();

    static class AsyncClassLoader extends ClassLoader
    {
        public AsyncClassLoader(final ClassLoader parent)
        {
            super(parent);
        }

        public Class<?> define(String name, byte[] b, int off, int len)
        {
            return defineClass(name, b, off, len);
        }
    }


    @Override
    public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined, final ProtectionDomain protectionDomain, final byte[] classfileBuffer) throws IllegalClassFormatException
    {
        try
        {
            if (className.startsWith("java"))
            {
                return null;
            }
            return transform(new ByteArrayInputStream(classfileBuffer));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public <T> Class<T> instrument(Class<T> c)
    {
        final boolean needsInstrumentation = needsInstrumentation(c);
        if (!needsInstrumentation)
        {
            return c;
        }
        try
        {
            String fileName;
            String binName;
            if (c.getPackage() != null)
            {
                String packageName = c.getPackage().getName();
                String simpleBinName = c.getCanonicalName().substring(packageName.length() + 1).replace('.', '$');
                binName = packageName + "." + simpleBinName;
                fileName = simpleBinName + ".class";
            }
            else
            {
                binName = c.getCanonicalName().replace('.', '$');
                fileName = binName + ".class";
            }
            byte[] bytes = transform(c);
            if (bytes != null)
            {
                AsyncClassLoader loader = new AsyncClassLoader(c.getClassLoader());
                return (Class<T>) loader.define(binName, bytes, 0, bytes.length);
            }
            return c;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public byte[] transform(final Class<?> clazz)
    {
        try
        {
            String fileName;
            String binName;
            if (clazz.getPackage() != null)
            {
                String packageName = clazz.getPackage().getName();
                String simpleBinName = clazz.getCanonicalName().substring(packageName.length() + 1).replace('.', '$');
                binName = packageName + "." + simpleBinName;
                fileName = simpleBinName + ".class";
            }
            else
            {
                binName = clazz.getCanonicalName().replace('.', '$');
                fileName = binName + ".class";
            }
            for (Method m : clazz.getDeclaredMethods())
            {
                if (m.isAnnotationPresent(Async.class))
                {
                    return transform(clazz.getResourceAsStream(fileName));
                }
            }
            return null;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    public byte[] transform(final InputStream resourceAsStream) throws IOException, AnalyzerException, NoSuchMethodException
    {
        final ClassReader cr = new ClassReader(resourceAsStream);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        int countInstrumented = 0;

        for (MethodNode mn : (List<MethodNode>) new ArrayList(cn.methods))
        {
            if (mn.visibleAnnotations == null
                    || !mn.visibleAnnotations.stream().anyMatch(a -> ((AnnotationNode) a).desc.equals(ASYNC_DESCRIPTOR)))
            {
                continue;
            }
            // removing aync annotation to prevent reinstrumentation
            List<AnnotationNode> visibleAnnotations = (List<AnnotationNode>) mn.visibleAnnotations;
            for (int i = visibleAnnotations.size(); --i >= 0; )
            {
                if ((visibleAnnotations.get(i)).desc.equals(ASYNC_DESCRIPTOR))
                {
                    mn.visibleAnnotations.remove(i);
                }
            }
            countInstrumented++;
            //printMethod(cn, mn);
            final MethodNode mv = new MethodNode(Opcodes.ACC_PRIVATE | ACC_STATIC,
                    mn.name, mn.desc, mn.signature, (String[])mn.exceptions.toArray(new String[mn.exceptions.size()]));
            mn.accept(mv);
            // TODO generate better names, reuse names if possible
            mv.name = mn.name + "$" + countInstrumented;
            mv.desc = Type.getMethodDescriptor(COMPLETABLE_FUTURE_TYPE, ASYNC_STATE_TYPE, OBJECT_TYPE);
            mv.signature = null;

            final InsnList instructions = mn.instructions;


            Analyzer analyzer = new Analyzer(new TypeInterpreter());
            Frame[] frames = analyzer.analyze(cn.name, mn);
            final AbstractInsnNode[] isns = instructions.toArray();


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
            for (int i = 0; i < isns.length; i++)
            {
                final AbstractInsnNode ins = isns[i];
                final Frame frame = frames[i];

                if (ins instanceof MethodInsnNode)
                {
                    // TODO: check cast at instrumentation time
                    MethodInsnNode methodIns = (MethodInsnNode) ins;
                    if (ins.getOpcode() == Opcodes.INVOKESTATIC
                            && "await".equals(methodIns.name)
                            && methodIns.owner.equals(AWAIT_NAME)
                            && methodIns.desc.equals("(Ljava/util/concurrent/CompletableFuture;)Ljava/lang/Object;"))
                    {
                        // TODO: other comparisons.
                        mv.visitInsn(Opcodes.DUP);
                        // stack: completableFuture completableFuture

                        // original: future.join()

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
                        // stack: { state newfuture }

                        // code:    return future.exceptionally(x -> x).thenCompose(x -> _func(state));

                        mv.visitInsn(SWAP);
                        // stack: { newfuture state}

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


                        // stack: { newfuture function }
                        mv.visitMethodInsn(INVOKEVIRTUAL, COMPLETABLE_FUTURE_NAME, "thenCompose", "(Ljava/util/function/Function;)Ljava/util/concurrent/CompletableFuture;", false);
                        // stack: { newfuture2 }
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
                        continue;
                    }
                }
                ins.accept(mv);
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

            mv.access &= ~ACC_PUBLIC;
            mv.access |= ACC_PRIVATE;

            {
                mv.maxLocals = Math.max(8, mv.maxLocals + 4);
                mv.maxStack = Math.max(8, mv.maxStack + 4);
                //printMethod(cn, mv);
            }

            mn.instructions.clear();
            //can't be done: mn.access |= ACC_SYNTHETIC;
            mn.tryCatchBlocks.clear();

            saveLocals(0, frames[0], mn);
            mn.visitInsn(ACONST_NULL);
            mn.visitMethodInsn(INVOKESTATIC, cn.name, mv.name, mv.desc, false);

            Type futureType = Type.getReturnType(mn.desc);
            if (!COMPLETABLE_FUTURE_TYPE.equals(futureType)
                    && futureType.getInternalName().equals("com/ea/orbit/concurrent/Task"))
            {
                mn.visitMethodInsn(INVOKESTATIC,
                        futureType.getInternalName(),
                        "from",
                        Type.getMethodDescriptor(futureType, Type.getType(CompletionStage.class)),
                        false);
            }
            mn.visitInsn(ARETURN);

            {
                mn.maxStack = 8;
                //printMethod(cn, mn);
                //printMethod(cn, mv);
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
            // new ClassReader(bytes).accept(new TraceClassVisitor(new PrintWriter(System.out)), ClassReader.EXPAND_FRAMES);
        }
        return bytes;
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
                        p.getText().add(String.valueOf(((BasicValue)frame.getLocal(x)).getType()));
                        p.getText().add(" ");
                    }
                    p.getText().add("]\r\n");
                    p.getText().add("Stack: [ ");
                    for (int x = 0; x < frame.getStackSize(); x++)
                    {
                        p.getText().add(String.valueOf(((BasicValue)frame.getStack(x)).getType()));
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

    private int saveLocals(int pos, Frame frame, MethodNode mv) throws NoSuchMethodException
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

    private void saveStack(Frame frame, MethodNode mv) throws NoSuchMethodException
    {
        // stack: { ... state }
        for (int i = frame.getStackSize(); --i >= 0; )
        {
            final BasicValue value = (BasicValue) frame.getStack(i);
            if (value.getType() != null)
            {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, ASYNC_STATE_NAME, "push",
                        Type.getMethodDescriptor(ASYNC_STATE_TYPE, value.isReference() ? OBJECT_TYPE : value.getType(), ASYNC_STATE_TYPE), false);
            }
        }
        // stack: { state }
    }

    private void restoreStackAndLocals(final Frame frame, final MethodNode mv)
    {
        // local_1 = async state

        // count locals+stack
        int localsPlusStack = 0;
        int startIndex = 0;
        for (int i = startIndex; i < frame.getLocals(); i++)
        {
            BasicValue local = (BasicValue) frame.getLocal(i);
            if (local != null && local != BasicValue.UNINITIALIZED_VALUE && local.getType() != null)
            {
                localsPlusStack++;
            }
        }
        for (int i = 0; i < frame.getStackSize(); i++)
        {
            BasicValue local = (BasicValue) frame.getStack(i);
            if (local != null && local.getType() != null)
            {
                localsPlusStack++;
            }
        }

        // async state
        for (int i = 0; i < frame.getStackSize(); i++)
        {
            BasicValue local = (BasicValue)frame.getStack(i);
            if (local != null && local.getType() != null)
            {
                final int idx = --localsPlusStack;
                mv.visitVarInsn(Opcodes.ALOAD, startIndex);
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

        // async state
        for (int i = frame.getLocals(); --i >= startIndex; )
        {
            BasicValue local = (BasicValue)frame.getLocal(i);
            if (local != null && local != BasicValue.UNINITIALIZED_VALUE)
            {
                if (local.getType() != null)
                {
                    final int idx = --localsPlusStack;
                    // dup the state
                    mv.visitVarInsn(Opcodes.ALOAD, startIndex);
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
                    //mv.visitVarInsn(ASTORE, i);
                }
            }
        }
    }


    boolean needsInstrumentation(final Class<?> c)
    {
        for (Class<?> s = c; s != null; s = s.getSuperclass())
        {
            for (Method m : s.getDeclaredMethods())
            {
                if (m.isAnnotationPresent(Async.class))
                {
                    return true;
                }
            }
        }
        return false;
    }

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
                if (t != null && u != null && t.getSort() == Type.OBJECT
                        && u.getSort() == Type.OBJECT)
                {
                    return BasicValue.REFERENCE_VALUE;
                }
            }
            return super.merge(v, w);
        }
    }

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
