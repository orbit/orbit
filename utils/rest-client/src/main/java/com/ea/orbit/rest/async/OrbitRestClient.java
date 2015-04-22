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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Daniel Sperry
 */
public class OrbitRestClient
{
    private WebTarget target;
    private ConcurrentHashMap<Class<?>, Object> proxies = new ConcurrentHashMap<>();

    public OrbitRestClient(WebTarget webTarget)
    {
        target = webTarget;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> interfaceClass)
    {
        Object proxy = proxies.get(interfaceClass);
        if (proxy == null)
        {
            proxy = Proxy.newProxyInstance(OrbitRestClient.class.getClassLoader(), new Class[]{ interfaceClass },
                    (theProxy, method, args) -> invoke(interfaceClass, method, args));
            proxies.put(interfaceClass, proxy);
        }
        return (T) proxy;
    }

    private Object invoke(final Class<?> interfaceClass, final Method method, final Object[] args)
    {
        CompletableFuture<Object> future = new CompletableFuture<>();
        Path annotation = interfaceClass.getAnnotation(Path.class);
        final Class<?> returnType = method.getReturnType();
        final Type genericReturnType = method.getGenericReturnType();

        String httpMethod = getHttpMethod(method);

        Invocation.Builder builder = target.path("/").request();

        builder.async().method(httpMethod, new RestInvocationCallbackImpl().future(future));
        return future;
    }


    private String getHttpMethod(final Method method)
    {
        HttpMethod httpMethod = method.getAnnotation(HttpMethod.class);
        if (httpMethod != null)
        {
            return httpMethod.value();
        }
        for (Annotation an : method.getAnnotations())
        {
            httpMethod = an.annotationType().getAnnotation(HttpMethod.class);
            if (httpMethod != null)
            {
                return httpMethod.value();
            }
        }
        return null;
    }


    public static byte[] createGenericSubclass(Class<?> superClass, Type genericParameter) throws Exception
    {

        ClassWriter cw = new ClassWriter(0);
        FieldVisitor fv;
        MethodVisitor mv;
        AnnotationVisitor av0;

        String name = "com/ea/orbit/rest/async/RestInvocationCallbackImpl";

        //name.getClass().getTypeParameters()
        //genericToSignature(genericParameter);


        String superName = "com/ea/orbit/rest/async/OrbitRestClient$RestInvocationCallback";
        String signature = "L" + superName + "<Ljava/lang/String;>;";
        cw.visit(52, Opcodes.ACC_SUPER, name, signature, superName, null);

        cw.visitSource("RestInvocationCallbackImpl.java", null);

        cw.visitInnerClass(superName, "com/ea/orbit/rest/async/OrbitRestClient", "RestInvocationCallback", Opcodes.ACC_STATIC);
        {
            mv = cw.visitMethod(0, "<init>", "()V", null, null);
            mv.visitCode();
            Label lbStart = new Label();
            mv.visitLabel(lbStart);
            mv.visitLineNumber(31, lbStart);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "<init>", "()V", false);
            mv.visitInsn(Opcodes.RETURN);
            Label lbEnd = new Label();
            mv.visitLabel(lbEnd);
            mv.visitLocalVariable("this", "L" + name + ";", null, lbStart, lbEnd, 0);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }

    static String toGenericSignature(final Type type)
    {
        if (type instanceof Class)
        {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        toGenericSignature(sb, type);
        return sb.toString();
    }

    static void toGenericSignature(StringBuilder sb, final Type type)
    {
        if (type instanceof GenericArrayType)
        {
            sb.append("[");
            toGenericSignature(sb, ((GenericArrayType) type).getGenericComponentType());
        }
        else if (type instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType) type;
            if (pt.getOwnerType() != null)
            {
                toGenericSignature(sb, pt.getOwnerType());
                sb.append("$");
            }
            sb.append('L');
            sb.append(((Class) pt.getRawType()).getName().replace('.', '/'));
            sb.append('<');
            for (Type p : pt.getActualTypeArguments())
            {
                if (p instanceof Class)
                {
                    toGenericSignature(sb, p);
                }
                else
                {
                    toGenericSignature(sb, p);
                }
            }
            sb.append(">;");
        }
        else if (type instanceof Class)
        {
            Class clazz = (Class) type;
            if (!clazz.isPrimitive() && !clazz.isArray())
            {
                sb.append('L');
                sb.append(clazz.getName().replace('.', '/'));
                sb.append(';');
            }
            else
            {
                sb.append(clazz.getName().replace('.', '/'));
            }
        }
        else if (type instanceof WildcardType)
        {
            WildcardType wc = (WildcardType) type;
            Type[] lowerBounds = wc.getLowerBounds();
            Type[] upperBounds = wc.getUpperBounds();
            boolean hasLower = lowerBounds != null && lowerBounds.length > 0;
            boolean hasUpper = upperBounds != null && upperBounds.length > 0;

            if (hasUpper && hasLower && Object.class.equals(lowerBounds[0]) && Object.class.equals(upperBounds[0]))
            {
                sb.append('*');
            }
            else if (hasLower)
            {
                sb.append("-");
                for (Type b : lowerBounds)
                {
                    toGenericSignature(sb, b);
                }
            }
            else if (hasUpper)
            {
                if (upperBounds.length == 1 && Object.class.equals(upperBounds[0]))
                {
                    sb.append("*");
                }
                else
                {
                    sb.append("+");
                    for (Type b : upperBounds)
                    {
                        toGenericSignature(sb, b);
                    }
                }
            }
            else
            {
                sb.append('*');
            }
        }
        else
        {
            throw new IllegalArgumentException("Invalid type: " + type);
        }
    }

    static class RestInvocationCallback<T> implements InvocationCallback<T>
    {
        private CompletableFuture<?> future;


        public RestInvocationCallback<T> future(final CompletableFuture<?> future)
        {
            this.future = future;
            return this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void completed(T response)
        {
            ((CompletableFuture) future).complete(response);
        }

        @Override
        public void failed(Throwable throwable)
        {
            future.completeExceptionally(throwable);
        }
    }
}
