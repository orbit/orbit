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

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.googlecode.gentyref.GenericTypeReflector;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class to create proxies for JAX-RS client interfaces.
 * <p/>
 * This class is  thread safe and immutable.
 *
 * @author Daniel Sperry
 */
public class RestClient
{
    private final static Map<String, Class<? extends RestInvocationCallback>> invocationClasses = new ConcurrentHashMap<>();
    private static final Class<? extends CompletableFuture> futureClass;

    private final WebTarget target;
    private final MultivaluedHashMap<String, Object> headers;

    // This is an internal cache. It changes, but the instance behaviour stays the same.
    private Map<Class<?>, Object> proxies = new ConcurrentHashMap<>();

    static
    {
        Class<CompletableFuture> futureClass1;
        try
        {
            //noinspection unchecked
            futureClass1 = (Class<CompletableFuture>) Class.forName("com.ea.orbit.concurrent.Task");
        }
        catch (Exception e)
        {
            futureClass1 = CompletableFuture.class;
        }
        futureClass = futureClass1;
    }

    /**
     * Constructs an orbit rest client from a jax-rs WebTarget.
     *
     * @param webTarget a jax-rx target, can be obtained with a jax-rs implementation,
     *                  for instance org.glassfish.jersey.core:jersey-clien.
     */
    public RestClient(WebTarget webTarget)
    {
        target = webTarget;
        headers = new MultivaluedHashMap<>();
    }

    public RestClient(final WebTarget target, final MultivaluedHashMap<String, Object> headers)
    {
        this.target = target;
        this.headers = headers;
    }

    /**
     * Returns a <b>copy</b> of the headers.
     */
    public MultivaluedMap<String, Object> getHeaders()
    {
        return new MultivaluedHashMap<String, Object>(headers);
    }

    /**
     * Returns a new orbit rest client replacing all the headers
     */
    public <T extends RestClient> T setHeaders(MultivaluedMap<String, Object> headers)
    {
        final MultivaluedHashMap<String, Object> newHeaders = new MultivaluedHashMap<>();
        newHeaders.putAll(headers);
        return newClient(target, newHeaders);
    }

    /**
     * Returns a new OrbitRestClient with the header {@code key} replaced/set
     */
    public <T extends RestClient> T setHeader(String key, String value)
    {
        final MultivaluedHashMap<String, Object> newHeaders = new MultivaluedHashMap<String, Object>(headers);
        newHeaders.putSingle(key, value);
        return newClient(target, newHeaders);
    }

    /**
     * Returns a new OrbitRestClient with where with an extra header
     */
    public <T extends RestClient> T addHeader(String key, String value)
    {
        final MultivaluedHashMap<String, Object> newHeaders = new MultivaluedHashMap<String, Object>(headers);
        newHeaders.add(key, value);
        return newClient(target, newHeaders);
    }

    @SuppressWarnings("unchecked")
    protected <T extends RestClient> T newClient(WebTarget target, MultivaluedHashMap<String, Object> headers)
    {
        return (T) new RestClient(target, headers);
    }

    /**
     * Returns an implementation of a jax-rs interface.
     * <p/>
     * The methods will issue remote calls to the web target provided on the OrbitRestClient constructor.
     * <p/>
     * The there are other implementation of rest proxies.
     * The special benefit of OrbitRestClient is that the interface method can return
     * {@link java.util.concurrent.CompletableFuture} or {@link com.ea.orbit.concurrent.Task}
     * If that is the case, those methods will return immediately and the Future will be completed asynchronously.
     *
     * @param interfaceClass the jax-rs annotated interface class
     * @param <T>            The interface type
     * @return an implementation of the interface that will make remote rest calls
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> interfaceClass)
    {
        Object proxy = proxies.get(interfaceClass);
        if (proxy == null)
        {
            final WebTarget interfaceTarget = addPath(target, interfaceClass);
            proxy = Proxy.newProxyInstance(RestClient.class.getClassLoader(), new Class[]{interfaceClass},
                    (theProxy, method, args) -> invoke(interfaceClass, interfaceTarget, method, args));
            proxies.put(interfaceClass, proxy);
        }
        return (T) proxy;
    }

    /**
     * Allows Rest Clients to filter exceptions that may arises from invoked calls.
     * @param throwable The exception that was encountered
     * @param <TThrowable> The Throwable type that will be returned
     * @param <TResult> The actual type returned
     * @return The filtered exception
     * @throws TThrowable
     */
    @SuppressWarnings("unchecked")
    protected static <TThrowable extends Throwable, TResult> TResult reThrow(Throwable throwable) throws TThrowable {
        throw (TThrowable)throwable;
    }

    protected Object handleInvokeException(Throwable e)
    {
        return reThrow(e);
    }

    private Object invoke(final Class<?> interfaceClass, WebTarget localTarget, final Method method, final Object[] args)
    {
        try
        {
            final Object invokeResult = invokeInternal(interfaceClass, localTarget, method, args);

            if (invokeResult instanceof CompletionStage)
            {
                return ((CompletionStage)invokeResult).exceptionally(e -> handleInvokeException((Throwable)e));
            }

            return invokeResult;
        }
        catch (Exception | Error e)
        {
            return handleInvokeException(e);
        }
    }

    private Object invokeInternal(final Class<?> interfaceClass, WebTarget localTarget, final Method method, final Object[] args)
    {
        Type methodGenericReturnType = method.getGenericReturnType();
        Type genericReturnType;
        genericReturnType = getActualType(method.getReturnType(), methodGenericReturnType);

        String httpMethod = getHttpMethod(method);
        if (httpMethod == null)
        {
            throw new IllegalArgumentException("Method not annotate with a valid http method annotation" + method);
        }

        MultivaluedHashMap<String, Object> localHeaders = new MultivaluedHashMap<>();
        localHeaders.putAll(this.headers);
        localTarget = addPath(localTarget, method);

        Object entity = null;
        Type entityType = null;

        final Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        for (int i = 0; i < parameterAnnotations.length; i++)
        {
            Object value = args[i];
            if (value instanceof CompletableFuture)
            {
                // TODO: make this entire method async if it returns CompletableFuture, and await() on the params
                value = ((CompletableFuture) value).join();
            }
            if (value == null)
            {
                // scan for a default value
                for (Annotation ann : parameterAnnotations[i])
                {
                    if (ann instanceof DefaultValue)
                    {
                        value = ((DefaultValue) ann).value();
                    }
                }
            }
            int paramAnnotationCount = parameterAnnotations[i].length;
            for (Annotation ann : parameterAnnotations[i])
            {
                if (ann instanceof PathParam)
                {
                    localTarget = localTarget.resolveTemplate(((PathParam) ann).value(), value);
                }
                else if (ann instanceof QueryParam)
                {
                    localTarget = localTarget.queryParam(((QueryParam) ann).value(), value);
                }
                else if (ann instanceof MatrixParam)
                {
                    localTarget = localTarget.matrixParam(((MatrixParam) ann).value(), value);
                }
                else if (ann instanceof HeaderParam)
                {
                    if (value instanceof Collection)
                    {
                        localHeaders.addAll(((HeaderParam) ann).value(), ((Collection) value).toArray());
                    }
                    else
                    {
                        localHeaders.add(((HeaderParam) ann).value(), value);
                    }
                }
                else if (ann instanceof FormParam)
                {
                    throw new NotSupportedException("Form params are not supported " + method.getName() + " param " + i);
                }
                else if (ann instanceof CookieParam)
                {
                    throw new NotSupportedException("Cookie params are not supported " + method.getName() + " param " + i);
                }
                else
                {
                    // not a param annotation...
                    paramAnnotationCount--;
                }
            }
            if (paramAnnotationCount == 0)
            {
                entity = value;
                entityType = getActualType(method.getParameterTypes()[i], method.getGenericParameterTypes()[i]);
            }
        }


        Invocation.Builder builder = localTarget.request();
        builder = builder.headers(localHeaders);
        Produces produces = getAnnotation(interfaceClass, method, Produces.class);
        if (produces != null)
        {
            builder = builder.accept(produces.value());
        }

        final GenericType responseGenericType = new GenericType(genericReturnType);

        if (entity != null)
        {
            String contentType = getContentType(interfaceClass, method, localHeaders);

            if (entityType instanceof ParameterizedType)
            {
                //noinspection unchecked
                entity = new GenericEntity(entity, entityType);
            }
            if (CompletableFuture.class.isAssignableFrom(method.getReturnType()))
            {
                CompletableFuture<Object> future = createFuture(method);
                builder.async().method(httpMethod, Entity.entity(entity, contentType), createAsyncInvocationCallback(genericReturnType).future(future));
                return future;
            }
            else
            {
                //noinspection unchecked
                return builder.method(httpMethod, Entity.entity(entity, contentType), responseGenericType);
            }
        }

        // no entity
        if (CompletableFuture.class.isAssignableFrom(method.getReturnType()))
        {
            CompletableFuture<Object> future = createFuture(method);
            builder.async().method(httpMethod, createAsyncInvocationCallback(genericReturnType).future(future));
            return future;
        }
        else
        {
            return builder.method(httpMethod, responseGenericType);
        }
    }

    private String getContentType(
            final Class<?> interfaceClass,
            final Method method,
            final MultivaluedHashMap<String, Object> headers)
    {

        final List<Object> contentTypes = headers.get(HttpHeaders.CONTENT_TYPE);
        if ((contentTypes != null) && (!contentTypes.isEmpty()))
        {
            // gets the first one
            return contentTypes.get(0).toString();
        }
        Consumes consumes = getAnnotation(interfaceClass, method, Consumes.class);
        if (consumes != null && consumes.value().length > 0)
        {
            // gets the first one
            return consumes.value()[0];
        }
        return null;
    }

    private Type getActualType(final Class rawType, final Type type)
    {
        if (CompletableFuture.class.isAssignableFrom(rawType))
        {
            return GenericTypeReflector.getTypeParameter(type,
                    CompletableFuture.class.getTypeParameters()[0]);
        }
        return type;
    }

    @SuppressWarnings("unchecked")
    protected CompletableFuture<Object> createFuture(final Method method)
    {
        try
        {
            return futureClass.newInstance();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Can't instantiate future class: " + futureClass);
        }
    }


    private static WebTarget addPath(WebTarget target, final AnnotatedElement element)
    {
        final Path path = element.getAnnotation(Path.class);
        return (path != null) ? target.path(path.value()) : target;
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

    private <T extends Annotation> T getAnnotation(Class<?> interfaceClass, Method method, Class<T> annotationClass)
    {
        T annotation = method.getAnnotation(annotationClass);
        if (annotation == null)
        {
            return interfaceClass.getAnnotation(annotationClass);
        }
        return annotation;
    }

    @SuppressWarnings("unchecked")
    static RestInvocationCallback<?> createAsyncInvocationCallback(Type type)
    {
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
                super(RestClient.class.getClassLoader());
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

    /**
     * Builds a new Rest Client with a WebTarget that applies the specified property.
     * @param propertyName Property to set a value on
     * @param propertyValue Value to apply
     * @param <T> Rest Client
     * @return new Rest Client with applied change
     */
    public <T extends RestClient> T property(String propertyName, Object propertyValue)
    {
        return newClient(
            target.path("").property(propertyName, propertyValue),
            this.headers);
    }
}
