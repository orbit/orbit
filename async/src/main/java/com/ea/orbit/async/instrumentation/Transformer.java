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
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Value;

import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static final String AWAIT_INIT_METHOD_DESC = "()V";
    public static final String AWAIT_INIT_METHOD_NAME = "init";

    private static final String COMPLETABLE_FUTURE_DESCRIPTOR = "Ljava/util/concurrent/CompletableFuture;";
    private static final Type COMPLETABLE_FUTURE_TYPE = Type.getType(COMPLETABLE_FUTURE_DESCRIPTOR);
    private static final String COMPLETABLE_FUTURE_RET = ")Ljava/util/concurrent/CompletableFuture;";
    private static final String COMPLETABLE_FUTURE_NAME = "java/util/concurrent/CompletableFuture";

    private static final Type COMPLETION_STAGE_TYPE = Type.getType("Ljava/util/concurrent/CompletionStage;");
    private static final String COMPLETION_STAGE_RET = ")Ljava/util/concurrent/CompletionStage;";

    private static final String _THIS = "_this";

    private static final String TASK_DESCRIPTOR = "Lcom/ea/orbit/concurrent/Task;";
    private static final String TASK_RET = ")Lcom/ea/orbit/concurrent/Task;";
    private static final String TASK_NAME = "com/ea/orbit/concurrent/Task";
    private static final Type TASK_TYPE = Type.getType("Lcom/ea/orbit/concurrent/Task;");

    public static final String JOIN_METHOD_NAME = "join";
    public static final String JOIN_METHOD_DESC = "()Ljava/lang/Object;";

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
        final FrameAnalyzer.ExtendedFrame frame;
        // original instruction index
        final int index;
        public int[] stackToNewLocal;
        public int[] argumentToLocal;
        public int[] localToiArgument;

        public SwitchEntry(final int key, final FrameAnalyzer.ExtendedFrame frame, final int index)
        {
            this.key = key;
            this.frame = frame;
            this.index = index;
            this.futureIsDoneLabel = new Label();
            this.resumeLabel = new Label();
        }
    }

    static class Argument
    {
        BasicValue value;
        String name;
        int iArgumentLocal;
        // working space
        int tmpLocalMapping = -1;
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
                    || !original.visibleAnnotations.stream().anyMatch(new Predicate<AnnotationNode>()
            {
                @Override
                public boolean test(final AnnotationNode a)
                {
                    return ((AnnotationNode) a).desc.equals(ASYNC_DESCRIPTOR);
                }
            })))
            {
                continue;
            }
            countInstrumented++;

            transformAsyncMethod(classNode, original, nameUseCount);
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
                // this is only called if COMPUTE_FRAMES is enabled

                // implementing this properly would require loading information
                // for type1 and type2 from the class path.
                // that's too expensive and it also creates problems for offline instrumentation.
                // reusing the old frames and manually creating new ones is a lot cheaper.
                return type1.equals(type2) ? type1 : "java/lang/Object";
            }
        };
        classNode.accept(cw);
        byte[] bytes = cw.toByteArray();
        // for development use: new ClassReader(bytes).accept(new TraceClassVisitor(new PrintWriter(System.out)), ClassReader.EXPAND_FRAMES);
        // for development use: DevDebug.debugSaveTrace(classNode.name + ".3", classNode);
        // for development use: DevDebug.debugSave(classNode, bytes);
        // for development use: DevDebug.debugSaveTrace(classNode.name + ".1", bytes);
        return bytes;
    }

    private void transformAsyncMethod(final ClassNode classNode, final MethodNode original, final Map<String, Integer> nameUseCount) throws AnalyzerException
    {
        final List<SwitchEntry> switchEntries = new ArrayList<>();
        SwitchEntry entryPoint;
        List<Argument> arguments = new ArrayList<>();
        final List<Label> switchLabels = new ArrayList<>();
        Analyzer analyzer = new FrameAnalyzer();
        Frame[] frames = analyzer.analyze(classNode.name, original);

        entryPoint = new SwitchEntry(0, (FrameAnalyzer.ExtendedFrame) frames[0], 0);
        switchLabels.add(entryPoint.resumeLabel);
        int ii = 0;
        int count = 0;

        for (Iterator it = original.instructions.iterator(); it.hasNext(); ii++)
        {
            Object o = it.next();
            if ((o instanceof MethodInsnNode && isAwaitCall((MethodInsnNode) o)))
            {
                SwitchEntry se = new SwitchEntry(++count, (FrameAnalyzer.ExtendedFrame) frames[ii], ii);
                switchLabels.add(se.resumeLabel);
                switchEntries.add(se);
            }
        }
        // compute variable mapping
        // map stack->new locals
        // map (locals + new locals) -> parameters
        // result:
        //    stackToNewLocal mapping (stack_index, local_index)
        //        - original stack to new local
        //        - as an int array: int[stack_index] = local_index
        //    localToArgumentMapping (parameter,local_index)
        //       - in this order, the push order
        //       - as an int array: int[parameter_index] = local_index
        int newMaxLocals = 0;
        Set<AbstractInsnNode> uninitializedObjects = new HashSet<>();
        for (SwitchEntry se : switchEntries)
        {
            // clear the used state
            arguments.forEach(new Consumer<Argument>()
            {
                @Override
                public void accept(final Argument p)
                {
                    p.tmpLocalMapping = -1;
                }
            });
            se.stackToNewLocal = new int[se.frame.getStackSize()];
            Arrays.fill(se.stackToNewLocal, -1);
            int iNewLocal = original.maxLocals;
            for (int j = 0; j < se.frame.getStackSize(); j++)
            {
                final BasicValue value = se.frame.getStack(j);
                if (value != null && !isUninitialized(value))
                {
                    se.stackToNewLocal[j] = iNewLocal;
                    iNewLocal += valueSize(se.frame.getStack(j));
                }
                else
                {
                    se.stackToNewLocal[j] = -1;
                }
                // marks uninitialized objects
                if (isUninitialized(value))
                {
                    uninitializedObjects.add(((FrameAnalyzer.ExtendedValue) value).insnNode);
                }
            }

            newMaxLocals = Math.max(iNewLocal, newMaxLocals);
            se.localToiArgument = new int[newMaxLocals];
            Arrays.fill(se.localToiArgument, -1);
            // maps the locals to arguments
            for (int iLocal = 0; iLocal < se.frame.getLocals(); iLocal += valueSize(se.frame.getLocal(iLocal)))
            {
                final BasicValue value = se.frame.getLocal(iLocal);
                if (value != null && !isUninitialized(value) && value.getType() != null)
                {
                    mapLocalToLambdaArgument(original, se, arguments, iLocal, value);
                }
                // marks uninitialized objects
                if (isUninitialized(value))
                {
                    uninitializedObjects.add(((FrameAnalyzer.ExtendedValue) value).insnNode);
                }
            }
            // maps the stack locals to arguments
            for (int j = 0; j < se.frame.getStackSize(); j++)
            {
                final int iLocal = se.stackToNewLocal[j];
                if (iLocal >= 0)
                {
                    mapLocalToLambdaArgument(original, se, arguments, iLocal, se.frame.getStack(j));
                }
            }
            // extract local-to-argument mapping
            se.argumentToLocal = new int[arguments.size()];
            for (int j = 0; j < arguments.size(); j++)
            {
                se.argumentToLocal[j] = arguments.get(j).tmpLocalMapping;
                if (se.argumentToLocal[j] >= 0)
                {
                    se.localToiArgument[se.argumentToLocal[j]] = arguments.get(j).iArgumentLocal;
                }
            }
        }
        // only replaces object initialization
        // if uninitialized objects are present in the stack during a await call.
        if (uninitializedObjects.size() > 0)
        {
            replaceObjectInitialization(original, frames, uninitializedObjects);
        }
        original.maxLocals = Math.max(original.maxLocals, newMaxLocals);

        arguments.forEach(new Consumer<Argument>()
        {
            @Override
            public void accept(final Argument p)
            {
                p.tmpLocalMapping = -2;
            }
        });
        final Argument stateArgument = mapLocalToLambdaArgument(original, null, arguments, 0, BasicValue.INT_VALUE);
        final Argument lambdaArgument = mapLocalToLambdaArgument(original, null, arguments, 0, BasicValue.REFERENCE_VALUE);
        stateArgument.name = "async$state";
        lambdaArgument.name = "async$input";
        final Object[] defaultFrame = arguments.stream().map(new Function<Argument, Object>()
        {
            @Override
            public Object apply(final Argument a)
            {
                return Transformer.this.toFrameType(a.value);
            }
        }).toArray();
        final Type[] typeArguments = arguments.stream().map(new Function<Argument, Type>()
        {
            @Override
            public Type apply(final Argument a)
            {
                return a.value.getType();
            }
        }).toArray(new IntFunction<Type[]>()
        {
            @Override
            public Type[] apply(final int s)
            {
                return new Type[s];
            }
        });
        final String lambdaDesc = Type.getMethodDescriptor(Type.getType(Function.class), Arrays.copyOf(typeArguments, typeArguments.length - 1));

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

        final boolean staticSynchronized = ((original.access & ACC_SYNCHRONIZED) != 0 && (original.access & ACC_STATIC) != 0);
        final boolean instanceSynchronized = ((original.access & ACC_SYNCHRONIZED) != 0 && (original.access & ACC_STATIC) == 0);
        final MethodNode continued = new MethodNode(Opcodes.ACC_PRIVATE | ACC_STATIC | (staticSynchronized ? ACC_SYNCHRONIZED : 0),
                original.name, original.desc, original.signature, (String[]) original.exceptions.toArray(new String[original.exceptions.size()]));

        String continuedName = "async$" + original.name;
        Integer countUses = nameUseCount.get(continuedName);
        nameUseCount.put(continuedName, countUses == null ? 1 : countUses + 1);

        continued.name = (countUses == null) ? continuedName : (continuedName + "$" + countUses);
        continued.desc = Type.getMethodDescriptor(COMPLETABLE_FUTURE_TYPE, typeArguments);
        continued.signature = null;
        final Handle handle = new Handle(Opcodes.H_INVOKESTATIC, classNode.name, continued.name, continued.desc);

        final boolean isTaskRet = original.desc.endsWith(TASK_RET);

        // embedding this class here because it has so many captured vars
        class MyMethodVisitor extends MethodVisitor
        {
            int awaitIndex;

            public MyMethodVisitor(final MethodNode mv)
            {
                super(Opcodes.ASM5, mv);
            }

            @Override
            public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc, final boolean itf)
            {
                if (isAwaitCall(opcode, owner, name, desc))
                {
                    // passing 'this' here could create problems if the transformAwait starts adding calls to Await.await()
                    transformAwait(this, switchEntries.get(awaitIndex++), lambdaDesc, arguments, mv == continued, isTaskRet, handle);
                }
                else if (isAwaitInitCall(opcode, owner, name, desc))
                {
                    // replaces all references to Await.init with NOP
                    super.visitInsn(NOP);
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
                super.visitFrame((type == F_NEW) ? F_FULL : type, nLocal, local, nStack, stack);
            }

            @Override
            public void visitInsn(final int opcode)
            {
                if (opcode == ARETURN && mv == continued && instanceSynchronized)
                {
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitInsn(MONITOREXIT);
                }
                super.visitInsn(opcode);
            }
        }
        original.accept(new MyMethodVisitor(replacement));


        // if instanceSynchronized all exit paths must release the lock on "this"
        // the exception path is easy, a global exception handler can do it.
        // the return path demands a release before each return

        Label thisMonitorStart = null;
        Label thisMonitorEnd = null;
        if (instanceSynchronized)
        {
            // "this" must be in the first argument
            thisMonitorStart = new Label();
            thisMonitorEnd = new Label();
            continued.visitVarInsn(ALOAD, 0);
            continued.visitInsn(MONITORENTER);
            continued.visitLabel(thisMonitorStart);
            continued.visitTryCatchBlock(thisMonitorStart, thisMonitorEnd, thisMonitorEnd, null);
        }

        // get pos
        continued.visitVarInsn(ILOAD, stateArgument.iArgumentLocal);

        final Label defaultLabel = new Label();
        continued.visitTableSwitchInsn(0, switchLabels.size() - 1, defaultLabel, switchLabels.toArray(new Label[switchLabels.size()]));

        // original entry point
        continued.visitLabel(entryPoint.resumeLabel);
        continued.visitFrame(F_FULL, defaultFrame.length, defaultFrame, 0, new Object[0]);
        restoreStackAndLocals(continued, entryPoint, arguments);

        original.accept(new MyMethodVisitor(continued));


        // add switch entries for the continuation state machine
        Label lastRestorePoint = null;
        for (SwitchEntry se : switchEntries)
        {
            // code: resumeLabel:
            continued.visitLabel(se.resumeLabel);
            continued.visitFrame(F_FULL, defaultFrame.length, defaultFrame, 0, new Object[0]);

            // code:    restoreStack;
            // code:    restoreLocals;
            restoreStackAndLocals(continued, se, arguments);
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
        continued.visitFrame(F_FULL, defaultFrame.length, defaultFrame, 0, new Object[0]);
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
        if (instanceSynchronized)
        {
            // "this" must be in the first argument
            continued.visitLabel(thisMonitorEnd);
            continued.visitFrame(F_FULL, 1, new Object[]{ classNode.name }, 1, new Object[]{ Type.getType(Throwable.class).getInternalName() });
            continued.visitVarInsn(ALOAD, 0);
            continued.visitInsn(MONITOREXIT);
            continued.visitInsn(ATHROW);
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

    private Argument mapLocalToLambdaArgument(final MethodNode originalMethod, SwitchEntry se, final List<Argument> arguments, final int local, final BasicValue value)
    {
        Argument argument = null;
        String name = local < originalMethod.maxLocals ? guessName(originalMethod, se, local) : "_stack$" + (local - originalMethod.maxLocals);

        // tries to match by the name and type first.
        argument = arguments.stream()
                .filter(new Predicate<Argument>()
                {
                    @Override
                    public boolean test(final Argument x)
                    {
                        return x.tmpLocalMapping == -1 && x.value.equals(value) && x.name.equals(name);
                    }
                })
                .findFirst().orElse(null);

        if (argument != null)
        {
            argument.tmpLocalMapping = local;
            return argument;
        }
        // no name match, just grab the first available or create a new one.
        argument = arguments.stream()
                .filter(new Predicate<Argument>()
                {
                    @Override
                    public boolean test(final Argument x)
                    {
                        return x.tmpLocalMapping == -1 && x.value.equals(value);
                    }
                })
                .findFirst().orElseGet(
                        new Supplier<Argument>()
                        {
                            @Override
                            public Argument get()
                            {
                                final Argument np = new Argument();
                                np.iArgumentLocal = arguments.size() == 0 ? 0 :
                                        arguments.get(arguments.size() - 1).iArgumentLocal + arguments.get(arguments.size() - 1).value.getSize();
                                np.value = value;
                                np.name = (name == null) ? name : "_arg$" + np.iArgumentLocal;
                                arguments.add(np);
                                return np;
                            }
                        }
                );
        argument.tmpLocalMapping = local;
        return argument;
    }

    private String guessName(final MethodNode method, final SwitchEntry se, final int local)
    {
        if (se != null)
        {
            for (LocalVariableNode node : method.localVariables)
            {
                if (node.index == local
                        && method.instructions.indexOf(node.start) <= se.index
                        && method.instructions.indexOf(node.end) >= se.index)
                {
                    return node.name;
                }
            }
        }
        return "_local$" + local;
    }

    private boolean isUninitialized(final Value value)
    {
        return value instanceof FrameAnalyzer.ExtendedValue && ((FrameAnalyzer.ExtendedValue) value).isUninitialized();
    }

    private int valueSize(final Value local)
    {
        return local == null ? 1 : local.getSize();
    }

    // replacing only the initialization of objects that are uninitialized at the moment of an await call.
    void replaceObjectInitialization(
            final MethodNode methodNode,
            final Frame<BasicValue>[] frames,
            final Set<AbstractInsnNode> objectCreationNodes)
    {
        int originalLocals = methodNode.maxLocals;
        final Set<AbstractInsnNode> uninitializedObjects = objectCreationNodes != null
                ? objectCreationNodes
                : Stream.of(methodNode.instructions.toArray())
                .filter(new Predicate<AbstractInsnNode>()
                {
                    @Override
                    public boolean test(final AbstractInsnNode i)
                    {
                        return i.getOpcode() == NEW;
                    }
                })
                .collect(Collectors.toSet());

        // since we can't store uninitialized objects they have to be removed or replaced.
        // this works for bytecodes where the initialization is implemented like:
        // NEW T
        // DUP
        // ...
        // T.<init>(..)V

        // and the stack before <init> is: {... T' T' args}
        // and the stack after <init> is: {... T}
        // this conforms all cases of java derived bytecode that I'm aware of.
        // but it might not be always true.


        // replace frameNodes and constructor calls
        int index = 0;
        for (AbstractInsnNode insnNode = methodNode.instructions.getFirst(); insnNode != null; index++, insnNode = insnNode.getNext())
        {
            if (insnNode instanceof FrameNode)
            {
                FrameNode frameNode = (FrameNode) insnNode;
                frameNode.stack = replaceUninitializedFrameValues(uninitializedObjects, frameNode.stack);
                frameNode.local = replaceUninitializedFrameValues(uninitializedObjects, frameNode.local);
            }
            else if (insnNode.getOpcode() == INVOKESPECIAL)
            {
                MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                if (methodInsnNode.name.equals("<init>"))
                {
                    insnNode = replaceConstructorCall(methodNode, frames[index], uninitializedObjects, originalLocals, methodInsnNode);
                }
            }
        }
        // replace new calls
        for (AbstractInsnNode insnNode = methodNode.instructions.getFirst(); insnNode != null; insnNode = insnNode.getNext())
        {
            if (insnNode.getOpcode() == NEW && (uninitializedObjects.contains(insnNode)))
            {
                InsnNode newInsn = new InsnNode(ACONST_NULL);
                methodNode.instructions.insertBefore(insnNode, newInsn);
                methodNode.instructions.remove(insnNode);
                insnNode = newInsn;
            }
        }
    }

    private AbstractInsnNode replaceConstructorCall(
            final MethodNode methodNode,
            final Frame<BasicValue> frame, final Set<AbstractInsnNode> uninitializedObjects, final int originalLocals,
            final MethodInsnNode methodInsnNode)
    {
        Type[] oldArguments = Type.getArgumentTypes(methodInsnNode.desc);
        int targetStackIndex = frame.getStackSize() - (1 + oldArguments.length);
        final FrameAnalyzer.ExtendedValue target = (FrameAnalyzer.ExtendedValue) frame.getStack(targetStackIndex);
        if (uninitializedObjects != null && !uninitializedObjects.contains(target.insnNode))
        {
            // only replaces the objects that need replacement
            return methodInsnNode;
        }

        final InsnList instructions = methodNode.instructions;
        // later, methodInsnNode is moved to the end of all inserted instructions
        AbstractInsnNode currentInsn = methodInsnNode;

        // find the first reference to the target in the stack and saves everything after it.
        int firstOccurrence = 0;
        int[] stackToLocal = new int[frame.getStackSize()];
        Arrays.fill(stackToLocal, -1);
        while (firstOccurrence < targetStackIndex && !target.equals(frame.getStack(firstOccurrence)))
        {
            firstOccurrence++;
        }
        // number of repetitions in the stack
        int repetitions = 1;
        for (int i = firstOccurrence + 1; i < frame.getStackSize() && target.equals(frame.getStack(i)); i++)
        {
            repetitions++;
        }

        // stores relevant stack values to new local variables
        int newMaxLocals = 0;
        int newObject = -1;
        for (int iLocal = originalLocals, j = frame.getStackSize(); --j >= firstOccurrence; )
        {
            BasicValue value = frame.getStack(j);
            if (value.getType() == null)
            {
                // some uninitialized value, shouldn't happen, just in case
                instructions.insert(currentInsn, currentInsn = new InsnNode(POP));
                instructions.insert(currentInsn, currentInsn = new InsnNode(ACONST_NULL));
                value = BasicValue.REFERENCE_VALUE;
            }
            if (!target.equals(value))
            {
                stackToLocal[j] = iLocal;
                // storing to a temporary local variable
                instructions.insert(currentInsn, currentInsn = new VarInsnNode(value.getType().getOpcode(ISTORE), iLocal));
                iLocal += valueSize(value);
            }
            else
            {
                // position where we will put the new object if needed
                if (j >= firstOccurrence + repetitions)
                {
                    stackToLocal[j] = newObject != -1 ? newObject : (newObject = iLocal++);
                }
                instructions.insert(currentInsn, currentInsn = new InsnNode(POP));
            }
            newMaxLocals = iLocal;
        }
        methodNode.maxLocals = Math.max(newMaxLocals, methodNode.maxLocals);

        // creates the object
        instructions.insert(currentInsn, currentInsn = new TypeInsnNode(NEW, target.getType().getInternalName()));

        // stores the new object to all locals that should contain it, if any
        for (int j = 0; j < frame.getLocals(); )
        {
            // replaces all locals that used to reference the old value
            BasicValue local = frame.getLocal(j);
            if (target.equals(local))
            {
                instructions.insert(currentInsn, currentInsn = new InsnNode(DUP));
                instructions.insert(currentInsn, currentInsn = new VarInsnNode(ASTORE, j));
            }
            j += local.getSize();
        }

        if (firstOccurrence < targetStackIndex)
        {
            // duping instead of putting it to a local, just to look as regular java code.
            for (int i = 1; i < repetitions; i++)
            {
                instructions.insert(currentInsn, currentInsn = new InsnNode(DUP));
            }
            if (newObject != -1)
            {
                instructions.insert(currentInsn, currentInsn = new InsnNode(DUP));
                instructions.insert(currentInsn, currentInsn = new VarInsnNode(ASTORE, newObject));
            }
        }

        // restoring the the stack
        for (int j = firstOccurrence + repetitions; j < frame.getStackSize(); j++)
        {
            final BasicValue value = frame.getStack(j);
            if (value.getType() != null)
            {
                instructions.insert(currentInsn, currentInsn = new VarInsnNode(value.getType().getOpcode(ILOAD), stackToLocal[j]));
            }
            else
            {
                // uninitialized value
                instructions.insert(currentInsn, currentInsn = new InsnNode(ACONST_NULL));
            }
        }
        // move the constructor call to here
        instructions.remove(methodInsnNode);
        instructions.insert(currentInsn, currentInsn = methodInsnNode);

        // checks if there is stack reconstruction to do:
        return currentInsn;
    }

    private List<Object> replaceUninitializedFrameValues(
            final Set<AbstractInsnNode> uninitializedObjects,
            final List<Object> list)
    {
        if (list == null)
        {
            return null;
        }
        final List<Object> newList = new ArrayList<>(list);
        for (int i = 0, l = newList.size(); i < l; i++)
        {
            final Object v = newList.get(i);
            // replaces uninitialized object nodes with the actual type from the newList
            if (v instanceof LabelNode)
            {
                AbstractInsnNode node = (AbstractInsnNode) v;
                while (!(node instanceof TypeInsnNode && node.getOpcode() == NEW))
                {
                    node = node.getNext();
                }
                if (uninitializedObjects.contains(node))
                {
                    newList.set(i, Type.getType(((TypeInsnNode) node).desc).getInternalName());
                }
            }
        }
        return newList;
    }

    /**
     * Replaces calls to Await.await with returing a promise or with a join().
     *
     * @param mv          the method node whose instructions are being modified
     * @param lambdaDesc
     * @param isContinued if this is the continuation method
     * @param handle      @return a list of switch entries to finish the continuation state machine
     */
    private void transformAwait(
            final MethodVisitor mv,
            final SwitchEntry switchEntry,
            final String lambdaDesc,
            final List<Argument> lambdaArguments,
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
        // code:    saveStack to new locals
        // code:    push lambda parameters (locals and stack)
        // code:    return future.exceptionally(nop).thenCompose(x -> _func(x, state));
        // code: }
        // code: jump futureIsDoneLabel:
        // code: future.join():

        // and this is added to the switch
        // code: resumeLabel:
        // code:    restoreStack;
        // code:    restoreLocals;
        // code: futureIsDoneLabel:

        // label to the point to jump if the future is completed (isDone())
        Label futureIsDoneLabel = isContinued ? switchEntry.futureIsDoneLabel : new Label();

        mv.visitMethodInsn(INVOKEVIRTUAL, COMPLETABLE_FUTURE_NAME, "isDone", "()Z", false);
        // code: jump futureIsDoneLabel:
        mv.visitJumpInsn(IFNE, futureIsDoneLabel);

        // code:    saveStack to new state
        // code:    push the future
        // code:    push all lambda parameters
        // code:    create the lambda

        // clears the stack and leaves asyncState in the top
        // stack: { ... future }
        saveStack(mv, switchEntry);
        // stack: { }
        mv.visitVarInsn(ALOAD, switchEntry.stackToNewLocal[switchEntry.frame.getStackSize() - 1]);
        // stack: { future  }
        mv.visitMethodInsn(INVOKESTATIC, Type.getType(Function.class).getInternalName(), "identity", "()Ljava/util/function/Function;", false);
        // stack: { future identity_function }
        // this discards any exception. the exception will be thrown by calling join.
        // the other option is not to use thenCompose and use something more complex.
        mv.visitMethodInsn(INVOKEVIRTUAL, COMPLETABLE_FUTURE_NAME, "exceptionally", "(Ljava/util/function/Function;)Ljava/util/concurrent/CompletableFuture;", false);
        // stack: { new_future }

        // code:    return future.exceptionally(x -> x).thenCompose(x -> _func(state));

        pushArguments(mv, switchEntry, lambdaArguments);
        // stack: { new_future ...arguments-2...  }
        mv.visitIntInsn(SIPUSH, switchEntry.key);
        // stack: { new_future ...arguments-2... state  }

        mv.visitInvokeDynamicInsn("apply", lambdaDesc,
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
        // release monitors
        if (switchEntry.frame.monitors.length > 0)
        {
            for (int i = switchEntry.frame.monitors.length; --i >= 0; )
            {
                final BasicValue monitorValue = switchEntry.frame.monitors[i];
                int monitorLocal = -1;
                for (int iLocal = 0; iLocal < switchEntry.frame.getLocals(); iLocal += valueSize(switchEntry.frame.getLocal(iLocal)))
                {
                    if (switchEntry.frame.getLocal(iLocal) == monitorValue)
                    {
                        monitorLocal = iLocal;
                        // don't break; prefer using the last one
                    }
                }
                // only release if a local variable containing the monitor was found
                if (monitorLocal != -1)
                {
                    mv.visitVarInsn(ALOAD, monitorLocal);
                    mv.visitInsn(MONITOREXIT);
                }
                else
                {
                    // TODO: warn or error
                }
            }
        }
        mv.visitInsn(ARETURN);
        // code: futureIsDone:
        mv.visitLabel(futureIsDoneLabel);
        fullFrame(mv, switchEntry.frame);
        mv.visitMethodInsn(INVOKEVIRTUAL, COMPLETABLE_FUTURE_NAME, JOIN_METHOD_NAME, JOIN_METHOD_DESC, false);
        // changing back the instruction list
        // end of instruction loop
    }

    private void fullFrame(final MethodVisitor mv, final Frame frame)
    {
        Object[] locals = new Object[frame.getLocals()];
        Object[] stack = new Object[frame.getStackSize()];
        int nStack = 0;
        int nLocals = 0;
        int maxLocal = 0;
        for (int i = 0; i < locals.length; i++)
        {
            BasicValue value = (BasicValue) frame.getLocal(i);
            Type type = value.getType();
            if (type == null)
            {
                locals[nLocals++] = TOP;
            }
            else
            {
                locals[nLocals++] = toFrameType(value);
                if (value.getSize() == 2) i++;
                maxLocal = nLocals;
            }
        }
        for (int i = 0; i < frame.getStackSize(); i++)
        {
            BasicValue value = (BasicValue) frame.getStack(i);
            Type type = value.getType();
            if (type == null)
            {
                continue;
            }
            stack[nStack++] = toFrameType(value);
        }
        stack = nStack == stack.length ? stack : Arrays.copyOf(stack, nStack);
        locals = nLocals == locals.length ? locals : Arrays.copyOf(locals, maxLocal);
        mv.visitFrame(F_FULL, maxLocal, locals, nStack, stack);
    }

    private Object toFrameType(final BasicValue value)
    {
        final Type type = value.getType();
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

    private boolean isAwaitInitCall(final MethodInsnNode methodIns)
    {
        return isAwaitInitCall(methodIns.getOpcode(), methodIns.owner, methodIns.name, methodIns.desc);
    }

    private boolean isAwaitInitCall(int opcode, String owner, String name, String desc)
    {
        return opcode == Opcodes.INVOKESTATIC
                && AWAIT_INIT_METHOD_NAME.equals(name)
                && AWAIT_NAME.equals(owner)
                && AWAIT_INIT_METHOD_DESC.equals(desc);
    }

    private void saveStack(final MethodVisitor mv, final SwitchEntry se)
    {
        // stack: { ... }
        for (int i = se.stackToNewLocal.length; --i >= 0; )
        {
            int iLocal = se.stackToNewLocal[i];
            if (iLocal >= 0)
            {
                mv.visitVarInsn(se.frame.getStack(i).getType().getOpcode(ISTORE), iLocal);
            }
            else
            {
                mv.visitInsn(se.frame.getStack(i).getType().getSize() == 2 ? POP2 : POP);
            }
        }
        // stack: { } empty
    }

    private void pushArguments(final MethodVisitor mv, final SwitchEntry switchEntry, final List<Argument> lambdaArguments)
    {
        // stack: { ... }
        for (int i = 0, l = lambdaArguments.size() - 2; i < l; i++)
        {
            int iLocal = i < switchEntry.argumentToLocal.length ? switchEntry.argumentToLocal[i] : -1;
            final BasicValue value = lambdaArguments.get(i).value;
            if (iLocal >= 0)
            {
                mv.visitVarInsn(value.getType().getOpcode(ILOAD), iLocal);
            }
            else
            {
                pushDefault(mv, value);
            }
        }
        // stack: { ... lambdaArguments}
    }

    private void pushDefault(final MethodVisitor mv, final BasicValue value)
    {
        if (value.getType() == null)
        {
            mv.visitInsn(ACONST_NULL);
            return;
        }
        switch (value.getType().getSort())
        {
            case Type.VOID:
                mv.visitInsn(ACONST_NULL);
                return;
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                mv.visitInsn(ICONST_0);
                return;
            case Type.FLOAT:
                mv.visitInsn(FCONST_0);
                return;
            case Type.LONG:
                mv.visitInsn(LCONST_0);
                return;
            case Type.DOUBLE:
                mv.visitInsn(DCONST_0);
                return;
            case Type.ARRAY:
            case Type.OBJECT:
                mv.visitInsn(ACONST_NULL);
                return;
            default:
                throw new Error("Internal error");
        }
    }

    private void restoreStackAndLocals(final MethodVisitor mv, SwitchEntry se, List<Argument> lambdaArguments)
    {
        // restore the stack: just push in the right order.
        // restore the locals: push all that changed place then load
        if (se.argumentToLocal == null)
        {
            return;
        }
        final FrameAnalyzer.ExtendedFrame frame = se.frame;
        for (int i = 0; i < frame.getStackSize(); i++)
        {
            final BasicValue value = frame.getStack(i);
            // stack_index -> stackToNewLocal -> argumentToLocal -> arg_index
            int iLocal = se.stackToNewLocal[i];
            if (iLocal >= 0 && se.localToiArgument[iLocal] >= 0)
            {
                mv.visitVarInsn(value.getType().getOpcode(ILOAD), se.localToiArgument[iLocal]);
            }
            else
            {
                pushDefault(mv, value);
            }
        }
        // push the arguments that must be copied to locals
        for (int iLocal = 0; iLocal < frame.getLocals(); iLocal += valueSize(frame.getLocal(iLocal)))
        {
            final BasicValue value = frame.getLocal(iLocal);
            if (se.localToiArgument[iLocal] >= 0)
            {
                if (se.localToiArgument[iLocal] != iLocal)
                {
                    mv.visitVarInsn(value.getType().getOpcode(ILOAD), se.localToiArgument[iLocal]);
                }
            }
            else if (value != null && value.getType() != null)
            {
                pushDefault(mv, value);
                mv.visitVarInsn(value.getType().getOpcode(ISTORE), iLocal);
            }
        }
        // push the arguments that must be copied to locals
        for (int iLocal = frame.getLocals(); --iLocal >= 0; )
        {
            if (se.localToiArgument[iLocal] >= 0 && se.localToiArgument[iLocal] != iLocal)
            {
                mv.visitVarInsn(frame.getLocal(iLocal).getType().getOpcode(ISTORE), iLocal);
            }
        }
        // reacquire monitors
        for (int i = 0; i < se.frame.monitors.length; i++)
        {
            final BasicValue monitorValue = frame.monitors[i];
            int monitorLocal = -1;
            for (int iLocal = 0; iLocal < frame.getLocals(); iLocal += valueSize(frame.getLocal(iLocal)))
            {
                if (frame.getLocal(iLocal) == monitorValue)
                {
                    monitorLocal = iLocal;
                    // don't break; prefer using the last one
                }
            }
            // only acquire if a local variable containing the monitor was found
            if (monitorLocal != -1)
            {
                mv.visitVarInsn(ALOAD, monitorLocal);
                mv.visitInsn(MONITORENTER);
            }
            else
            {
                // TODO: warn or error
            }
        }
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

                        if (equalsUtf8(cr, classAddress, AWAIT_NAME))
                        {
                            // has Await.await
                            if (equalsUtf8(cr, ntAddress, AWAIT_METHOD_NAME)
                                    && equalsUtf8(cr, ntAddress + 2, AWAIT_METHOD_DESC))
                            {
                                return true;
                            }
                            // has Await.init
                            if (equalsUtf8(cr, ntAddress, AWAIT_INIT_METHOD_NAME)
                                    && equalsUtf8(cr, ntAddress + 2, AWAIT_INIT_METHOD_DESC))
                            {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
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
}
