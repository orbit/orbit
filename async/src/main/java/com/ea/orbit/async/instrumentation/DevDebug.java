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
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * For development use only
 */
class DevDebug
{
    static void printMethod(final ClassNode cn, final MethodNode mv)
    {
        final PrintWriter pw = new PrintWriter(System.out);
        pw.println("method " + mv.name + mv.desc);
        Textifier p = new Textifier();
        final TraceMethodVisitor tv = new TraceMethodVisitor(p);

        try
        {
            Analyzer analyzer2 = new Analyzer(new FrameAnalyzer.TypeInterpreter());
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


    static void debugSave(final ClassNode classNode, final byte[] bytes)
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


    static void debugSaveTrace(String name, final byte[] bytes)
    {
        try
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            new ClassReader(bytes).accept(new TraceClassVisitor(pw), 0);
            pw.flush();

            Path path = Paths.get("target/classes2/" + name + ".trace.txt");
            Files.createDirectories(path.getParent());
            Files.write(path, sw.toString().getBytes(Charset.forName("UTF-8")));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    static void debugSaveTrace(String name, ClassNode node)
    {
        try
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            node.accept(new TraceClassVisitor(pw));
            pw.flush();

            Path path = Paths.get("target/classes2/" + name + ".trace.txt");
            Files.createDirectories(path.getParent());
            Files.write(path, sw.toString().getBytes(Charset.forName("UTF-8")));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

}
