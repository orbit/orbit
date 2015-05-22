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
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;

/**
 * Internal class to modify the bytecodes of async-await methods to
 * make them behave in the expected fashion.
 *
 * @author Daniel Sperry
 */
public class Transformer implements ClassFileTransformer
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
    private static final String COMPLETION_STAGE_RET = ")Ljava/util/concurrent/CompletionStage;";

    private static final Type OBJECT_TYPE = Type.getType(Object.class);
    private static final String _THIS = "_this";
    private static final String DYN_FUNCTION = "(" + ASYNC_STATE_TYPE.getDescriptor() + ")Ljava/util/function/Function;";

    private static final String TASK_DESCRIPTOR = "Lcom/ea/orbit/concurrent/Task;";
    private static final String TASK_RET = ")Lcom/ea/orbit/concurrent/Task;";
    private static final String TASK_NAME = "com/ea/orbit/concurrent/Task";
    private static final Type TASK_TYPE = Type.getType("Lcom/ea/orbit/concurrent/Task;");

    @Override
    public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined, final ProtectionDomain protectionDomain, final byte[] classfileBuffer) throws IllegalClassFormatException
    {
        try
        {
            if (className != null && className.startsWith("java"))
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
        catch (Exception | Error e)
        {
            // TODO: better formatting.
            // Avoid using slf4j or any dependency here.
            // this is supposed to be a critical error.
            // it should be ok to write directly to the syserr.
            // Alternatively using the standard java logging is also a possibility.
            final RuntimeException exception = new RuntimeException("Error instrumenting: " + className, e);
            exception.printStackTrace();
            throw exception;
        }
    }

    static class SwitchEntry
    {
        final Label resumeLabel;
        final Label futureIsDoneLabel;
        final int key;
        final Frame frame;

        public SwitchEntry(final int key, final Frame frame)
        {
            this.key = key;
            this.frame = frame;
            this.futureIsDoneLabel = new Label();
            this.resumeLabel = new Label();
        }
    }

    public byte[] instrument(InputStream inputStream)
    {
        try
        {
            ClassReader cr = new ClassReader(inputStream);
            if (needsInstrumentation(cr))
            {
                return transform(cr);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Does the actual instrumentation generating new bytecode
     *
     * @param cr the class reader for this class
     * @return null or the new class bytes
     */
    public byte[] transform(ClassReader cr) throws AnalyzerException
    {
        ClassNode classNode = new ClassNode();
        // using EXPAND_FRAMES because F_SAME causes problems when inserting new frames
        cr.accept(classNode, ClassReader.EXPAND_FRAMES);

        int countInstrumented = 0;

        Map<String, Integer> nameUseCount = new HashMap<>();

        // TODO: also remove calls to `Await.init()`
        for (MethodNode original : (List<MethodNode>) new ArrayList(classNode.methods))
        {
            Integer countOriginalUses = nameUseCount.get(original.name);
            nameUseCount.put(original.name, countOriginalUses == null ? 1 : countOriginalUses + 1);

            boolean taskReturn = original.desc.endsWith(TASK_RET);
            if (!taskReturn && !original.desc.endsWith(COMPLETABLE_FUTURE_RET) && !original.desc.endsWith(COMPLETION_STAGE_RET))
            {
                // method must return completable future or task
                continue;
            }
            boolean hasAwaitCall = false;
            // checks if the method has a call to await()
            for (Iterator it = original.instructions.iterator(); it.hasNext(); )
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
            if (!taskReturn && !original.name.contains("$") && (original.visibleAnnotations == null
                    || !original.visibleAnnotations.stream().anyMatch(a -> ((AnnotationNode) a).desc.equals(ASYNC_DESCRIPTOR))))
            {
                continue;
            }
            countInstrumented++;

            final List<SwitchEntry> switchEntries = new ArrayList<>();
            SwitchEntry entryPoint;
            final List<Label> switchLabels = new ArrayList<>();
            {
                Analyzer analyzer = new Analyzer(new TypeInterpreter());
                Frame[] frames = analyzer.analyze(classNode.name, original);
                entryPoint = new SwitchEntry(0, frames[0]);
                switchLabels.add(entryPoint.resumeLabel);
                int ii = 0;
                int count = 0;

                for (Iterator it = original.instructions.iterator(); it.hasNext(); ii++)
                {
                    Object o = it.next();
                    if ((o instanceof MethodInsnNode && isAwaitCall((MethodInsnNode) o)))
                    {
                        SwitchEntry se = new SwitchEntry(++count, frames[ii]);
                        switchLabels.add(se.resumeLabel);
                        switchEntries.add(se);
                    }
                }
            }


            // adding the switch entries and restore code
            // the local variable restoration has to occur outside
            // the try-catch blocks to avoid problems with the
            // local-frame analysis
            //
            // Where the jvm is unsure of the actual type of a local var
            // when inside the exception handler because the var has changed.
            // for development use: printMethod(cn, mn);

            final MethodNode replacement = new MethodNode(original.access,
                    original.name, original.desc, original.signature, (String[]) original.exceptions.toArray(new String[original.exceptions.size()]));

            final MethodNode continued = new MethodNode(Opcodes.ACC_PRIVATE | ACC_STATIC,
                    original.name, original.desc, original.signature, (String[]) original.exceptions.toArray(new String[original.exceptions.size()]));

            String continuedName = "async$" + original.name;
            Integer countUses = nameUseCount.get(continuedName);
            nameUseCount.put(continuedName, countUses == null ? 1 : countUses + 1);

            continued.name = (countUses == null) ? continuedName : (continuedName + "$" + countUses);
            continued.desc = Type.getMethodDescriptor(COMPLETABLE_FUTURE_TYPE, ASYNC_STATE_TYPE, OBJECT_TYPE);
            continued.signature = null;
            final Handle handle = new Handle(Opcodes.H_INVOKESTATIC, classNode.name, continued.name, continued.desc);

            final boolean isTaskRet = original.desc.endsWith(TASK_RET);

            original.accept(new MyMethodVisitor(replacement, switchEntries, false, isTaskRet, handle));

            // get pos
            continued.visitVarInsn(ALOAD, !Modifier.isStatic(continued.access) ? 1 : 0);
            continued.visitMethodInsn(INVOKEVIRTUAL, ASYNC_STATE_NAME, "getPos", Type.getMethodDescriptor(Type.INT_TYPE), false);

            final Label defaultLabel = new Label();
            continued.visitTableSwitchInsn(0, switchLabels.size() - 1, defaultLabel, switchLabels.toArray(new Label[switchLabels.size()]));

            // original entry point
            continued.visitLabel(entryPoint.resumeLabel);
            continued.visitFrame(F_FULL, 2, new Object[]{ ASYNC_STATE_NAME, OBJECT_TYPE.getInternalName() }, 0, new Object[0]);
            restoreStackAndLocals(continued, entryPoint.frame);

            original.accept(new MyMethodVisitor(continued, switchEntries, true, isTaskRet, handle));


            // add switch entries for the continuation state machine
            Label lastRestorePoint = null;
            for (SwitchEntry se : switchEntries)
            {
                // code: resumeLabel:
                continued.visitLabel(se.resumeLabel);
                continued.visitFrame(F_FULL, 2, new Object[]{ ASYNC_STATE_NAME, OBJECT_TYPE.getInternalName() }, 0, new Object[0]);

                // code:    restoreStack;
                // code:    restoreLocals;
                restoreStackAndLocals(continued, se.frame);
                if (!Modifier.isStatic(original.access))
                {
                    if (lastRestorePoint != null)
                    {
                        continued.visitLocalVariable(_THIS, "L" + classNode.name + ";", null, lastRestorePoint, se.resumeLabel, 0);
                    }
                    lastRestorePoint = new Label();
                    continued.visitLabel(lastRestorePoint);
                }
                continued.visitJumpInsn(GOTO, se.futureIsDoneLabel);
            }

            // last switch case
            continued.visitLabel(defaultLabel);
            continued.visitFrame(F_FULL, 2, new Object[]{ ASYNC_STATE_NAME, OBJECT_TYPE.getInternalName() }, 0, new Object[0]);
            continued.visitTypeInsn(NEW, "java/lang/IllegalArgumentException");
            continued.visitInsn(DUP);
            continued.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "()V", false);
            continued.visitInsn(ATHROW);
            if (!Modifier.isStatic(original.access))
            {
                if (lastRestorePoint != null)
                {
                    Label endLabel = new Label();
                    continued.visitLabel(endLabel);
                    continued.visitLocalVariable(_THIS, "L" + classNode.name + ";", null, lastRestorePoint, endLabel, 0);
                }
            }

            // removing original method
            classNode.methods.remove(original);
            replacement.maxStack = Math.max(16, replacement.maxStack + 16);
            // adding replacement
            replacement.accept(classNode);


            continued.maxLocals = Math.max(16, continued.maxLocals + 16);
            continued.maxStack = Math.max(16, continued.maxStack + 16);
            // adding the continuation method
            continued.accept(classNode);

            // for development use: printMethod(classNode, replacement);
            // for development use: printMethod(classNode, classNode.methods.get(classNode.methods.size() - 2));
            // for development use: printMethod(classNode, continued);

            //final AbstractInsnNode[] in = replacement.instructions.toArray();
            //System.out.println(in);
        }
        // no changes.
        if (countInstrumented == 0)
        {
            return null;
        }

        // avoiding using COMPUTE_FRAMES
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS)
        {
            @Override
            protected String getCommonSuperClass(final String type1, final String type2)
            {
                return "java/lang/Object";
            }
        };
        classNode.accept(cw);
        byte[] bytes = cw.toByteArray();
        {
//             for development use: new ClassReader(bytes).accept(new TraceClassVisitor(new PrintWriter(System.out)), ClassReader.EXPAND_FRAMES);
//             for development use: debugSaveTrace(classNode.name + ".3", classNode);
//            {
//                final ClassWriter cw2 = new ClassWriter(ClassWriter.COMPUTE_FRAMES)
//                {
//                    @Override
//                    protected String getCommonSuperClass(final String type1, final String type2)
//                    {
//                        return "java/lang/Object";
//                    }
//                };
//                classNode.accept(cw2);
//                byte[] bytes2 = cw2.toByteArray();
//                debugSaveTrace(classNode.name + ".2", bytes2);
//            }
            // for development use: debugSave(classNode, bytes);
            // for development use: debugSaveTrace(classNode.name + ".1", bytes);

        }
        return bytes;
    }

    private void debugSaveTrace(String name, final byte[] bytes)
    {
        try
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            new ClassReader(bytes).accept(new TraceClassVisitor(pw), 0);
            pw.flush();

            Path path = Paths.get("target/classes2/" + name + ".trace");
            Files.createDirectories(path.getParent());
            Files.write(path, sw.toString().getBytes(Charset.forName("UTF-8")));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void debugSaveTrace(String name, ClassNode node)
    {
        try
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            node.accept(new TraceClassVisitor(pw));
            pw.flush();

            Path path = Paths.get("target/classes2/" + name + ".trace");
            Files.createDirectories(path.getParent());
            Files.write(path, sw.toString().getBytes(Charset.forName("UTF-8")));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void debugSave(final ClassNode classNode, final byte[] bytes)
    {
        try
        {

            Path path = Paths.get("target/classes2/" + classNode.name + ".class");
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Replaces calls to Await.await with returing a promise or with a join().
     *
     * @param mv          the method node whose instructions are being modified
     * @param isContinued if this is the continuation method
     * @param handle
     * @return a list of switch entries to finish the continuation state machine
     */
    private void transformAwait(
            final MethodVisitor mv,
            final SwitchEntry switchEntry,
            final boolean isContinued,
            final boolean isTaskRet,
            final Handle handle)
    {


        // stack: completableFuture
        mv.visitInsn(DUP);
        // stack: completableFuture completableFuture

        // original: Await.await(future)  (that by default does: future.join())

        // turns into:

        // code: if(!future.isDone()) {
        // code:    saveLocals to new state
        // code:    saveStack to new state
        // code:    return future.exceptionally(nop).thenCompose(x -> _func(x, state));
        // code: }
        // code: jump futureIsDoneLabel:
        // code: future.join():

        // and this is added to the switch
        // code: resumeLabel:
        // code:    restoreStack;
        // code:    restoreLocals;
        // code: futureIsDoneLabel:

        // lable to the point to jump if the future is completed (isDone())
        Label futureIsDoneLabel = isContinued ? switchEntry.futureIsDoneLabel : new Label();

        mv.visitMethodInsn(INVOKEVIRTUAL, COMPLETABLE_FUTURE_NAME, "isDone", "()Z", false);
        // code: jump futureIsDoneLabel:
        mv.visitJumpInsn(IFNE, futureIsDoneLabel);

        // code:    saveStack to new state
        // code:    saveLocals to new state
        int offset = saveLocals(mv, switchEntry.key, switchEntry.frame);
        // stack { .. future state }

        // clears the stack and leaves asyncState in the top
        saveStack(mv, switchEntry.frame);
        // stack: { state }
        mv.visitInsn(DUP);
        // stack: { state state }
        mv.visitInsn(DUP);
        mv.visitIntInsn(SIPUSH, offset);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ASYNC_STATE_NAME, "getObj", Type.getMethodDescriptor(OBJECT_TYPE, Type.INT_TYPE), false);
        mv.visitTypeInsn(CHECKCAST, COMPLETABLE_FUTURE_NAME);

        // stack: { state future }
        mv.visitMethodInsn(INVOKESTATIC, Type.getType(Function.class).getInternalName(), "identity", "()Ljava/util/function/Function;", false);
        // stack: { state future function }
        // this discards any exception. the exception will be thrown by calling join.
        // the other option is not to use thenCompose and use something more complex.
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
                handle,
                Type.getType("(Ljava/lang/Object;)Ljava/util/concurrent/CompletableFuture;"));


        // stack: { new_future function }
        mv.visitMethodInsn(INVOKEVIRTUAL, COMPLETABLE_FUTURE_NAME, "thenCompose", "(Ljava/util/function/Function;)Ljava/util/concurrent/CompletableFuture;", false);
        // stack: { new_future_02 }
        if (!isContinued && isTaskRet)
        {
            mv.visitMethodInsn(INVOKESTATIC,
                    TASK_NAME, "from",
                    Type.getMethodDescriptor(TASK_TYPE, COMPLETION_STAGE_TYPE),
                    false);
        }
        mv.visitInsn(ARETURN);
        // code: futureIsDone:
        mv.visitLabel(futureIsDoneLabel);
        fullFrame(mv, switchEntry.frame);
        mv.visitMethodInsn(INVOKEVIRTUAL, COMPLETABLE_FUTURE_NAME, "join", "()Ljava/lang/Object;", false);
        // changing back the instruction list
        // end of instruction loop
    }

    private void fullFrame(final MethodVisitor mv, final Frame frame)
    {
        Object[] locals = new Object[frame.getLocals()];
        Object[] stack = new Object[frame.getStackSize()];
        int nStack = 0;
        int nLocals = 0;
        for (int i = 0; i < locals.length; i++)
        {
            BasicValue value = (BasicValue) frame.getLocal(i);
            Type type = value.getType();
            if (type == null)
            {
                continue;
            }
            locals[nLocals++] = toFrameType(type);
        }
        for (int i = 0; i < frame.getStackSize(); i++)
        {
            BasicValue value = (BasicValue) frame.getStack(i);
            Type type = value.getType();
            if (type == null)
            {
                continue;
            }
            stack[nStack++] = toFrameType(type);
        }
        stack = nStack == stack.length ? stack : Arrays.copyOf(stack, nStack);
        locals = nLocals == locals.length ? locals : Arrays.copyOf(locals, nLocals);
        mv.visitFrame(F_FULL, nLocals, locals, nStack, stack);
    }

    private Object toFrameType(final Type type)
    {
        if (type == null)
        {
            return Opcodes.NULL;
        }
        switch (type.getSort())
        {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.INT:
            case Type.CHAR:
            case Type.SHORT:
                return Opcodes.INTEGER;
            case Type.LONG:
                return Opcodes.LONG;
            case Type.FLOAT:
                return Opcodes.FLOAT;
            case Type.DOUBLE:
                return Opcodes.DOUBLE;
        }
        return type.getInternalName();
    }


    private boolean isAwaitCall(final MethodInsnNode methodIns)
    {
        return isAwaitCall(methodIns.getOpcode(), methodIns.owner, methodIns.name, methodIns.desc);
    }

    private boolean isAwaitCall(int opcode, String owner, String name, String desc)
    {
        return opcode == Opcodes.INVOKESTATIC
                && AWAIT_METHOD_NAME.equals(name)
                && AWAIT_NAME.equals(owner)
                && AWAIT_METHOD_DESC.equals(desc);
    }

    private void printMethod(final ClassNode cn, final MethodNode mv)
    {
        final PrintWriter pw = new PrintWriter(System.out);
        pw.println("method " + mv.name + mv.desc);
        Textifier p = new Textifier();
        final TraceMethodVisitor tv = new TraceMethodVisitor(p);

        try
        {
            Analyzer analyzer2 = new Analyzer(new TypeInterpreter());
            Frame[] frames2;
            try
            {
                frames2 = analyzer2.analyze(cn.superName, mv);
            }
            catch (AnalyzerException ex)
            {
                if (ex.node != null)
                {
                    pw.print("Error at: ");
                    ex.node.accept(tv);
                }
                ex.printStackTrace();
                frames2 = null;
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                frames2 = null;
            }

            final AbstractInsnNode[] isns2 = mv.instructions.toArray();

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

    private int saveLocals(final MethodVisitor mv, final int pos, final Frame frame)
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

    private void saveStack(final MethodVisitor mv, final Frame frame)
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

    private void restoreStackAndLocals(final MethodVisitor mv, final Frame frame)
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

    boolean needsInstrumentation(final ClassReader cr)
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
    private void varInsn(final MethodVisitor mv, final Type type, final boolean store, final int local)
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

    private class MyMethodVisitor extends MethodVisitor
    {
        private final List<SwitchEntry> switchEntries;
        private boolean isContinued;
        private final boolean isTaskRet;
        private final Handle handle;

        int awaitIndex;

        public MyMethodVisitor(
                final MethodNode mv,
                final List<SwitchEntry> switchEntries,
                final boolean isContinued, final boolean isTaskRet,
                final Handle handle)
        {
            super(Opcodes.ASM5, mv);
            this.switchEntries = switchEntries;
            this.isContinued = isContinued;
            this.isTaskRet = isTaskRet;
            this.handle = handle;
            awaitIndex = 0;
        }

        @Override
        public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc, final boolean itf)
        {
            if (isAwaitCall(opcode, owner, name, desc))
            {
                // passing this here could create problems if the transformAwait starts adding calls to Await.await()
                transformAwait(this, switchEntries.get(awaitIndex++), isContinued, isTaskRet, handle);
            }
            else
            {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }


        @Override
        public void visitFrame(final int type, final int nLocal, final Object[] local, final int nStack, final Object[] stack)
        {
            // the use of EXPAND_FRAMES adds F_NEW which creates problems if not removed.
            super.visitFrame((type == F_NEW) ? F_FULL : type,
                    nLocal, local, nStack, stack);
        }
    }
}
