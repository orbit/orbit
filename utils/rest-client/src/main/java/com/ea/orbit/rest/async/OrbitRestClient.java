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

import com.googlecode.gentyref.GenericTypeReflector;
import com.sun.org.apache.bcel.internal.generic.INVOKESPECIAL;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Daniel Sperry
 */
public class OrbitRestClient
{
    private WebTarget target;
    private Map<Class<?>, Object> proxies = new ConcurrentHashMap<>();
    private final static Map<String, Class<? extends RestInvocationCallback>> invocationClasses = new ConcurrentHashMap<>();

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

        builder.async().method(httpMethod, createAsyncInvocationCallback(method).future(future));
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

    @SuppressWarnings("unchecked")
    public static RestInvocationCallback<?> createAsyncInvocationCallback(Method method)
    {

        Type type = method.getGenericReturnType();
        type = type != null ? type : method.getReturnType();
        if (CompletableFuture.class.isAssignableFrom(method.getReturnType()))
        {
            type = GenericTypeReflector.getTypeParameter(type,
                    CompletableFuture.class.getTypeParameters()[0]);
        }
        String genericSignature = GenericUtils.toGenericSignature(type);

        Class<?> clazz = invocationClasses.get(genericSignature);
        if (clazz == null)
        {
            synchronized (invocationClasses)
            {
                clazz = invocationClasses.get(genericSignature);
                if (clazz == null)
                {
                    clazz = createInvocationCallbackClass(genericSignature);
                    invocationClasses.put(genericSignature, (Class<? extends RestInvocationCallback>) clazz);
                }
            }
        }
        try
        {
            return (RestInvocationCallback<?>) clazz.newInstance();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static Class<?> createInvocationCallbackClass(final String genericSignature)
    {
        org.objectweb.asm.Type superType = org.objectweb.asm.Type.getType(RestInvocationCallback.class);
        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        String name = "com/ea/orbit/rest/async/RestInvocationCallback_" + (genericSignature.hashCode() & 0xffff);

        String superName = superType.getInternalName();
        String signature = "L" + superName + "<" + genericSignature + ">;";
        cw.visit(Opcodes.V1_8, Opcodes.ACC_SUPER | Opcodes.ACC_PUBLIC, name, signature, superName, null);


        cw.visitInnerClass(superName, "com/ea/orbit/rest/async/OrbitRestClient", "RestInvocationCallback", Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
        {
            mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            Label lbStart = new Label();
            mv.visitLabel(lbStart);
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
        final byte[] bytes = cw.toByteArray();
        // perhaps we should use the source class ClassLoader as parent.
        class Loader extends ClassLoader
        {
            Loader()
            {
                super(OrbitRestClient.class.getClassLoader());
            }

            public Class<?> define(final String o, final byte[] bytes)
            {
                return super.defineClass(o, bytes, 0, bytes.length);
            }
        }
        return new Loader().define(null, bytes);
    }

    public static class RestInvocationCallback<T> implements InvocationCallback<T>
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
