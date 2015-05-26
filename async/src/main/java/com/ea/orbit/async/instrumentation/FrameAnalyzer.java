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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;

import java.util.Arrays;

// uses previous frames
// consider uninitialized values
public class FrameAnalyzer extends Analyzer
{
    private static final int FN_TOP = 0;
    private static final int FN_INTEGER = 1;
    private static final int FN_FLOAT = 2;
    private static final int FN_DOUBLE = 3;
    private static final int FN_LONG = 4;
    private static final int FN_NULL = 5;
    private static final int FN_UNINITIALIZED_THIS = 6;
    private final TypeInterpreter interpreter;

    static class ExtendedValue extends BasicValue
    {
        AbstractInsnNode insnNode;
        boolean uninitialized;
        public BasicValue[] undecided;

        public ExtendedValue(final Type type)
        {
            super(type);
        }

        public boolean isUninitialized()
        {
            return uninitialized;
        }

        @Override
        public boolean equals(final Object value)
        {
            if (insnNode != null || uninitialized == true)
            {
                return (value instanceof ExtendedValue) &&
                        ((ExtendedValue) value).uninitialized == uninitialized
                        && ((ExtendedValue) value).insnNode == insnNode;
            }
            return super.equals(value);
        }

        @Override
        public String toString()
        {
            return undecided != null ? "?" : uninitialized ? "%" + super.toString() : super.toString();
        }
    }

    static class ExtendedFrame extends Frame<BasicValue>
    {
        static final BasicValue[] EMPTY_MONITORS = new BasicValue[0];

        boolean force;
        // treat this array as immutable
        BasicValue[] monitors = EMPTY_MONITORS;

        public ExtendedFrame(final int nLocals, final int nStack)
        {
            super(nLocals, nStack);
        }

        public ExtendedFrame(final Frame<? extends BasicValue> src)
        {
            super(src);
            if (src instanceof ExtendedFrame)
            {
                this.monitors = ((ExtendedFrame) src).monitors;
            }
        }

        @Override
        public Frame<BasicValue> init(final Frame<? extends BasicValue> src)
        {
            final Frame<BasicValue> frame = super.init(src);
            if (frame instanceof ExtendedFrame && src instanceof ExtendedFrame)
            {
                ((ExtendedFrame) frame).monitors = ((ExtendedFrame) src).monitors;
            }
            return frame;
        }

        @Override
        public void execute(final AbstractInsnNode insn, final Interpreter<BasicValue> interpreter) throws AnalyzerException
        {
            switch (insn.getOpcode())
            {
                case Opcodes.MONITORENTER:
                    monitors = Arrays.copyOf(monitors, monitors.length + 1);
                    monitors[monitors.length - 1] = pop();
                    return;
                case Opcodes.MONITOREXIT:
                {
                    // tracking the monitors by `Value` identity only works if
                    // all object values are always unique different even if they have the same type
                    // if that can't be guaranteed we could store each monitor in a local variable

                    final BasicValue v = pop();
                    int iv = monitors.length;
                    for (; --iv >= 0 && monitors[iv] != v; )
                    {
                        // find lastIndexOf this monitor in the "monitor stack"
                    }
                    // there are usually two or more MONITOREXIT for each monitor
                    // due to the compiler generated exception handler
                    // obs.: the application code won't be in the handlers' block
                    if (iv != -1)
                    {
                        final BasicValue[] newMonitors = Arrays.copyOf(monitors, monitors.length - 1);
                        if (monitors.length - iv > 1)
                        {
                            // if not the last element (unlikely)
                            System.arraycopy(monitors, iv + 1, newMonitors, iv, monitors.length - iv);
                        }
                        monitors = newMonitors;
                    }
                    return;
                }
                case Opcodes.INVOKESPECIAL:
                    MethodInsnNode methodInsnNode = (MethodInsnNode) insn;
                    if (methodInsnNode.name.equals("<init>"))
                    {
                        // clear uninitialized flag from values in the frame
                        final BasicValue target = getStack(getStackSize() - (1 + Type.getArgumentTypes(methodInsnNode.desc).length));
                        final BasicValue newValue = interpreter.newValue(target.getType());
                        super.execute(insn, interpreter);
                        for (int i = 0; i < getLocals(); i++)
                        {
                            if (target.equals(getLocal(i)))
                            {
                                setLocal(i, newValue);
                            }
                        }
                        int s = getStackSize();
                        final BasicValue[] stack = new BasicValue[s];
                        for (int i = s; --i >= 0; )
                        {
                            final BasicValue v = pop();
                            stack[i] = target.equals(v) ? newValue : v;
                        }
                        for (int i = 0; i < s; i++)
                        {
                            push(stack[i]);
                        }
                        return;
                    }
                    break;
            }
            super.execute(insn, interpreter);
        }

        @Override
        public boolean merge(final Frame<? extends BasicValue> frame,
                             final Interpreter<BasicValue> interpreter) throws AnalyzerException
        {
            if (force)
            {
                // uses the current frame
                return true;
            }
            if (frame instanceof ExtendedFrame && ((ExtendedFrame) frame).force)
            {
                init(frame);
                return true;
            }
            return super.merge(frame, interpreter);
        }
    }

    /**
     * Used to discover the object types that are currently
     * being stored in the stack and in the locals.
     */
    static class TypeInterpreter extends BasicInterpreter
    {
        static TypeInterpreter instance = new TypeInterpreter();

        @Override
        public BasicValue newValue(Type type)
        {
            if (type != null && type.getSort() == Type.OBJECT)
            {
                return new ExtendedValue(type);
            }
            return super.newValue(type);
        }

        @Override
        public BasicValue newOperation(final AbstractInsnNode insn) throws AnalyzerException
        {
            if (insn.getOpcode() == Opcodes.NEW)
            {
                final Type type = Type.getObjectType(((TypeInsnNode) insn).desc);
                final ExtendedValue extendedValue = new ExtendedValue(type);
                extendedValue.uninitialized = true;
                extendedValue.insnNode = insn;
                return extendedValue;
            }
            return super.newOperation(insn);
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
                    ExtendedValue nv = (ExtendedValue) newValue(BasicValue.REFERENCE_VALUE.getType());
                    nv.undecided = new BasicValue[]{ v, w };
                    return nv;
                }
            }
            return super.merge(v, w);
        }
    }

    public FrameAnalyzer()
    {
        super(TypeInterpreter.instance);
        interpreter = TypeInterpreter.instance;
    }

    @Override
    protected Frame newFrame(final int nLocals, final int nStack)
    {
        return new ExtendedFrame(nLocals, nStack);
    }

    @Override
    protected Frame newFrame(final Frame src)
    {
        return new ExtendedFrame(src);
    }

    protected void init(String owner, MethodNode m) throws AnalyzerException
    {
        final Frame[] frames = getFrames();

        // populates frames from frame nodes
        AbstractInsnNode insnNode = m.instructions.getFirst();
        FrameAnalyzer.TypeInterpreter interpreter = FrameAnalyzer.TypeInterpreter.instance;
        Frame lastFrame = frames[0];
        for (int insnIndex = 0; insnNode != null; insnNode = insnNode.getNext(), insnIndex++)
        {
            if (insnNode instanceof FrameNode)
            {
                FrameNode frameNode = (FrameNode) insnNode;
                final int frameType = frameNode.type;
                if (frameType == F_NEW || frameType == F_FULL)
                {
                    ExtendedFrame frame = (ExtendedFrame) newFrame(lastFrame);
                    frame.force = true;
                    frames[insnIndex] = frame;
                    int iLocal_w = 0;
                    if (frameNode.local != null && frameNode.local.size() > 0)
                    {
                        for (int j = 0; j < frameNode.local.size(); j++)
                        {
                            BasicValue value = convertFrameNodeType(frameNode.local.get(j));
                            frame.setLocal(iLocal_w, value);
                            iLocal_w += value.getSize();
                        }
                    }
                    BasicValue nullValue = interpreter.newValue(null);
                    while (iLocal_w < m.maxLocals)
                    {
                        frame.setLocal(iLocal_w++, nullValue);
                    }
                    frame.clearStack();
                    if (frameNode.stack != null && frameNode.stack.size() > 0)
                    {
                        for (int j = 0; j < frameNode.stack.size(); j++)
                        {
                            frame.push(convertFrameNodeType(frameNode.stack.get(j)));
                        }
                    }
                    lastFrame = frame;
                }
            }
        }
    }

    // converts FrameNode information to the way Frame stores it
    BasicValue convertFrameNodeType(final Object v) throws AnalyzerException
    {
        if (v instanceof String)
        {
            return interpreter.newValue(Type.getObjectType((String) v));
        }
        else if (v instanceof Integer)
        {
            switch ((Integer) v)
            {
                case FN_TOP:
                    // TODO: check this
                    return interpreter.newValue(null);
                case FN_INTEGER:
                    return interpreter.newValue(Type.INT_TYPE);
                case FN_FLOAT:
                    return interpreter.newValue(Type.FLOAT_TYPE);
                case FN_DOUBLE:
                    return interpreter.newValue(Type.DOUBLE_TYPE);
                case FN_LONG:
                    return interpreter.newValue(Type.LONG_TYPE);
                case FN_NULL:
                    // TODO: check this
                    return interpreter.newValue(BasicValue.REFERENCE_VALUE.getType());
                case FN_UNINITIALIZED_THIS:
                    // TODO: check this
                    return interpreter.newValue(null);
            }
        }
        else if (v instanceof LabelNode)
        {
            AbstractInsnNode node = (AbstractInsnNode) v;
            while (node.getOpcode() != NEW)
            {
                node = node.getNext();
            }
            return interpreter.newOperation(node);
        }
        return interpreter.newValue(null);
    }
}
