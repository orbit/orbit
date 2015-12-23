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

package com.ea.orbit.rest.async;

import com.ea.agentloader.ClassPathUtils;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test the generic type signature conversion for generation dynamic classes
 *
 *  This is not the test you are looking for.
 */
public class SignatureConversionTest
{
    public String getSignature(Class c)
    {
        class Visitor extends org.objectweb.asm.ClassVisitor
        {
            String signature;

            public Visitor()
            {
                super(Opcodes.ASM5);
            }

            @Override
            public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces)
            {
                this.signature = signature;
            }
        }
        try
        {
            final ClassReader cr = new ClassReader(ClassPathUtils.getClassFile(c).openStream());
            final Visitor visitor = new Visitor();
            cr.accept(visitor, 0);
            return visitor.signature;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public String getSignature(Class c, String methodName)
    {
        class Visitor extends org.objectweb.asm.ClassVisitor
        {
            String signature;

            public Visitor()
            {
                super(Opcodes.ASM5);
            }

            @Override
            public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions)
            {
                if (name.equals(methodName))
                {
                    this.signature = signature;
                }
                return null;
            }
        }
        try
        {
            final ClassReader cr = new ClassReader(ClassPathUtils.getClassFile(c).openStream());
            final Visitor visitor = new Visitor();
            cr.accept(visitor, 0);
            return visitor.signature;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }


    public class C3<F,G> {

    }

    public abstract class C4<Q,Z,L,M> extends C3<L,M> implements List<Q>, Comparable<Z>
    {

    }

    public abstract class C5 extends C4<Object,Object,Object,Object>
    {

    }
    public <Q,Z,L,M> C4<Q,Z,L,M> c4()
    {
        return null;
    }

    public static interface ClassWithGenericMethods
    {
        ArrayList<List<Map<?, String>>> m1();

        List m2();

        List<?> m3();

        Map<String, String> m4();

        Map<String, List<?>> m5();

        Map<?, ?> m6();

        Map<Object, Object> m7();

        List<? extends Number> m8();

        List<? super Integer> m8_2();

        Map<String, ? extends List<?>> m9();

        Map<String, ? super List<?>> m10();

        Integer m11();

        String m12();

        Map<? extends List, ?> m13();

        Map<? super List<? extends Integer>, ?> m14();

        String[] m15();

        String[][] m16();

        List<Map<?, Integer>>[][] m17();

        int[][] m18();

        List<Map<?, int[]>> m19();

        List<Map<?, List<int[]>[]>> m20();

        List<int[]> m21();

        List<int[][][]> m23();

        List<Integer[][][]> m24();

        List<List<int[][]>[][][]> m25();

        class AA<Q>
        {

        }

        class BB extends AA<String>
        {
            class CC
            {
            }
        }

        BB m26();

        AA m27();

        BB.CC m29();

        AA<BB> m30();

    }

    public interface ClassWithParametrizedMethods {
        <X> ClassWithGenericMethods.AA<X> m32();

        <R> List<R> m31();
    }


    String toMethodSignature(Type type)
    {
        if (type instanceof Class)
        {
            return null;
        }
        return GenericUtils.toGenericSignature(type);
    }

    @Test
    public void testSignatureConversion() throws NoSuchMethodException
    {
        for (Method m : ClassWithGenericMethods.class.getDeclaredMethods())
        {
            String asmSignature = getSignature(ClassWithGenericMethods.class, m.getName());
            String mySignature = toMethodSignature(m.getGenericReturnType());
            if (asmSignature != null)
            {
                assertEquals(m.getName(), asmSignature.replaceFirst("\\(\\)", ""), mySignature);
            }
            else
            {
                assertNull(m.getName(), mySignature);
            }
        }
        assertEquals("Ljava/util/List<Ljava/lang/Object;>;", toMethodSignature(ClassWithParametrizedMethods.class.getDeclaredMethod("m31").getGenericReturnType()));
        assertEquals("Lcom/ea/orbit/rest/async/SignatureConversionTest$ClassWithGenericMethods$AA<Ljava/lang/Object;>;", toMethodSignature(ClassWithParametrizedMethods.class.getDeclaredMethod("m32").getGenericReturnType()));
    }


    @Test
    public void testSignatureConversionWithMethodHack() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        Method methodGenericSignature = Method.class.getDeclaredMethod(
                "getGenericSignature",
                (Class<?>[]) null
        );
        methodGenericSignature.setAccessible(true);


        for (Method m : ClassWithGenericMethods.class.getDeclaredMethods())
        {
            String asmSignature = getSignature(ClassWithGenericMethods.class, m.getName());
            String mySignature = (String) methodGenericSignature.invoke(m);
            if (asmSignature != null)
            {
                assertEquals(m.getName(), asmSignature.substring(2), mySignature.substring(2));
            }
            else
            {
                assertNull(m.getName(), mySignature);
            }
        }
        assertTrue(ClassWithGenericMethods.class.getDeclaredMethods().length > 10);
    }


    @Test
    public void testClassTypes() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        Method methodGenericSignature = Method.class.getDeclaredMethod(
                "getGenericSignature",
                (Class<?>[]) null
        );
        methodGenericSignature.setAccessible(true);

        final Method c4 = SignatureConversionTest.class.getDeclaredMethod("c4");
        assertEquals(getSignature(C5.class),
                GenericUtils.toGenericSignature(c4.getGenericReturnType()));
    }
}
