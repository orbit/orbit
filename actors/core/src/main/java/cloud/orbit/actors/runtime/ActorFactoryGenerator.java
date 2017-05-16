/*
 Copyright (C) 2016 Electronic Arts Inc.  All rights reserved.

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

package cloud.orbit.actors.runtime;

import com.googlecode.gentyref.GenericTypeReflector;

import cloud.orbit.actors.annotation.OneWay;
import cloud.orbit.concurrent.Task;
import cloud.orbit.exception.UncheckedException;
import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.CtPrimitiveType;
import javassist.NotFoundException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class to create actor factories, actor references and actor invokers.
 * <ul>
 * <li>This enables other JVM languages to use orbit.</li>
 * </ul>
 */
public class ActorFactoryGenerator
{
    private final static ClassPool classPool;
    private final static WeakHashMap<Class, ConcurrentMap<String, Class>> derivedClasses = new WeakHashMap<>();


    static
    {
        classPool = new ClassPool();
        classPool.appendSystemPath();
        classPool.appendClassPath(new ClassClassPath(ActorFactoryGenerator.class));
    }

    private static class GenericActorFactory<T> extends ReferenceFactory<T>
    {
        private int interfaceId;
        private Class<T> interfaceClass;
        private ObjectInvoker<T> invoker;
        private Constructor<T> referenceConstructor;

        @Override
        public Class<T> getInterface()
        {
            return interfaceClass;
        }

        @Override
        public int getInterfaceId()
        {
            return interfaceId;
        }

        @Override
        public ObjectInvoker<T> getInvoker()
        {
            return invoker;
        }

        @Override
        public T createReference(final String id)
        {
            try
            {
                return referenceConstructor.newInstance(id);
            }
            catch (final Exception e)
            {
                throw new UncheckedException(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T> ReferenceFactory<T> getFactoryFor(final Class<T> aInterface)
    {
        final String interfaceFullName = aInterface.getName().replace('$', '.');

        final int interfaceId = DefaultClassDictionary.get().getClassId(aInterface);
        final String referenceFullName = aInterface.getName() + "$Reference";
        try
        {
            final Class<T> referenceClass = makeReferenceClass(aInterface, interfaceFullName, interfaceId, referenceFullName);

            final GenericActorFactory<T> dyn = new GenericActorFactory<>();
            dyn.interfaceId = interfaceId;
            dyn.interfaceClass = aInterface;
            dyn.referenceConstructor = referenceClass.getConstructor(String.class);
            return dyn;
        }
        catch (final Exception e)
        {
            throw new UncheckedException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> makeReferenceClass(final Class<T> aInterface, final String interfaceFullName, final int interfaceId, final String referenceFullName) throws NotFoundException, CannotCompileException
    {
        Class clazz = lookup(aInterface, referenceFullName);
        if (clazz != null)
        {
            return clazz;
        }

        synchronized (aInterface)
        {
            // trying again from within the synchronized block.
            clazz = lookup(aInterface, referenceFullName);
            if (clazz != null)
            {
                return clazz;
            }

            final ClassPool pool = classPool;

            final CtClass cc = pool.makeClass(referenceFullName);
            final CtClass ccInterface = pool.get(aInterface.getName());
            final CtClass ccActorReference = pool.get(RemoteReference.class.getName());
            cc.setSuperclass(ccActorReference);
            cc.addInterface(ccInterface);
            cc.addConstructor(CtNewConstructor.make(new CtClass[]{ pool.get(String.class.getName()) }, null, "{ super($1); }", cc));

            int count = 0;
            for (final CtMethod m : ccInterface.getMethods())
            {
                if (!m.getDeclaringClass().isInterface() || !m.getReturnType().getName().equals(Task.class.getName()))
                {
                    continue;
                }

                count++;
                final boolean oneWay = m.hasAnnotation(OneWay.class);

                final String methodName = m.getName();
                final CtClass[] parameterTypes = m.getParameterTypes();

                final int methodId = computeMethodId(methodName, parameterTypes);

                final String methodReferenceField = methodName + "_" + count;
                cc.addField(CtField.make("private static java.lang.reflect.Method " + methodReferenceField + " = null;", cc));

                // could move this to a static initializer so save a few cylces
                final String lazyMethodReferenceInit = "(" + methodReferenceField + "!=null) ? " + methodReferenceField + " : ( "
                        + methodReferenceField + "=" + aInterface.getName() + ".class.getMethod(\"" + methodName + "\",$sig) )";

                // TODO: remove the method parameter from the invoke, this could be an utility method of RemoteReference
                final CtMethod newMethod = CtNewMethod.make(m.getReturnType(), methodName,
                        parameterTypes, m.getExceptionTypes(),
                        "{ return super.invoke(" + lazyMethodReferenceInit + ", " + oneWay + ", " + methodId + ", $args);  }",
                        cc);
                cc.addMethod(newMethod);
            }
            cc.addMethod(CtNewMethod.make("protected int _interfaceId() { return " + interfaceId + ";}", cc));
            cc.addMethod(CtNewMethod.make("protected Class  _interfaceClass() { return " + interfaceFullName + ".class;}", cc));
            return loadClass(cc, aInterface);
        }
    }

    private static Class loadClass(final CtClass cc, final Class relatedClass)
    {

        Class newClass;
        try
        {
            newClass = cc.getClassPool().toClass(cc, relatedClass.getClassLoader(), relatedClass.getProtectionDomain());
        }
        catch (final Exception ex)
        {
            try
            {
                newClass = relatedClass.getClassLoader().loadClass(cc.getName());
            }
            catch (final ClassNotFoundException e)
            {
                throw new UncheckedException(e);
            }
        }

        final ConcurrentMap<String, Class> map = getRelatedClassMap(relatedClass);
        map.put(newClass.getName(), newClass);
        return newClass;
    }

    private static ConcurrentMap<String, Class> getRelatedClassMap(final Class relatedClass)
    {
        ConcurrentMap<String, Class> map;
        synchronized (derivedClasses)
        {
            map = derivedClasses.get(relatedClass);
            if (map == null)
            {
                map = new ConcurrentHashMap<>();
                derivedClasses.put(relatedClass, map);
            }
        }
        return map;
    }

    private int computeMethodId(final String methodName, final CtClass[] parameterTypes)
    {
        final String methodSignature = methodName + "(" + Stream.of(parameterTypes).map(p -> p.getName()).collect(Collectors.joining(",")) + ")";
        return methodSignature.hashCode();
    }

    public int getMethodId(Method method)
    {
        final Class<?>[] parameterTypes = method.getParameterTypes();
        final String methodSignature = method.getName() + "(" + Stream.of(parameterTypes).map(p -> p.getName()).collect(Collectors.joining(",")) + ")";
        return methodSignature.hashCode();
    }

    public ObjectInvoker<Object> getInvokerFor(final Class<?> concreteClass)
    {
        try
        {
            final Class<?> aClass = makeInvokerClass(concreteClass, concreteClass.getName() + "$ObjectInvoker");
            return (ObjectInvoker<Object>) aClass.newInstance();
        }
        catch (final Exception e)
        {
            throw new UncheckedException(e);
        }
    }

    private <T> Class<?> makeInvokerClass(final Class<T> actorClass, final String invokerFullName)
            throws NotFoundException, CannotCompileException
    {
        Class clazz = lookup(actorClass, invokerFullName);
        if (clazz != null)
        {
            return clazz;
        }

        synchronized (actorClass)
        {
            // trying again from within the synchronized block.
            clazz = lookup(actorClass, invokerFullName);
            if (clazz != null)
            {
                return clazz;
            }
            final ClassPool pool = classPool;
            final CtClass cc = pool.makeClass(invokerFullName);
            final String className = actorClass.getName();
            final CtClass ccInterface = pool.get(className);
            final CtClass ccActorInvoker = pool.get(ObjectInvoker.class.getName());
            cc.setSuperclass(ccActorInvoker);

            final StringBuilder invokerBody = new StringBuilder(2000);
            invokerBody.append("public " + Task.class.getName() + " invoke(Object target, int methodId, Object[] params) {");
            final CtMethod[] declaredMethods = ccInterface.getMethods();
            invokerBody.append(" switch(methodId) { ");

            final StringBuilder getMethodBody = new StringBuilder(2000);
            getMethodBody.append("public java.lang.reflect.Method getMethod(int methodId) {");
            getMethodBody.append(" switch(methodId) { ");

            for (final CtMethod m : declaredMethods)
            {
                if (!m.getReturnType().getName().equals(Task.class.getName())
                        || !Modifier.isPublic(m.getModifiers())
                        || Modifier.isStatic(m.getModifiers()))
                {
                    continue;
                }
                final CtClass[] parameterTypes = m.getParameterTypes();
                final String methodName = m.getName();
                final int methodId = computeMethodId(methodName, parameterTypes);

                // invoker
                invokerBody.append("case " + methodId + ": return ((" + className + ")target)." + methodName + "(");
                unwrapParams(invokerBody, parameterTypes, "params");
                invokerBody.append("); ");

                final String parameterTypesStr = Stream.of(parameterTypes)
                        .map(p -> p.getName().replace('$', '.') + ".class")
                        .collect(Collectors.joining(","));

                // get method
                final String methodField = methodId > 0 ? "m" + methodId : "m_" + Math.abs(methodId);
                final String src = "private static java.lang.reflect.Method " + methodField + " = "
                        + className + ".class.getMethod(\"" + methodName + "\", "
                        + "new Class"
                        + ((parameterTypes.length > 0) ? "[]{" + parameterTypesStr + "}" : "[0]")
                        + ");";
                cc.addField(CtField.make(src, cc));
                getMethodBody.append("case " + methodId + ": return " + methodField + ";");


            }
            invokerBody.append("default: ");
            invokerBody.append("return super.invoke(target,methodId,params);} }");
            cc.addMethod(CtNewMethod.make(invokerBody.toString(), cc));

            getMethodBody.append("default: ");
            getMethodBody.append("return super.getMethod(methodId);} }");
            cc.addMethod(CtNewMethod.make(getMethodBody.toString(), cc));

            cc.addMethod(CtNewMethod.make("public Class getInterface() { return " + className + ".class; }", cc));

            return loadClass(cc, actorClass);
        }
    }

    protected static Class<?> makeStateClass(Class<? extends AbstractActor> actorClass)
    {
        final Type stateType = GenericTypeReflector.getTypeParameter(actorClass,
                AbstractActor.class.getTypeParameters()[0]);

        final String newStateName = actorClass.getName() + "$ActorState";

        if (stateType == null)
        {
            return LinkedHashMap.class;
        }

        Class c = lookup(actorClass, newStateName);
        if (c != null)
        {
            return c;
        }
        synchronized (actorClass)
        {
            c = lookup(actorClass, newStateName);
            if (c != null)
            {
                return c;
            }
            try
            {
                final Class<?> erased = GenericTypeReflector.erase(stateType);
                final Class<?> baseClass = erased.isInterface() ? Object.class : erased;
                final String genericSignature = GenericUtils.toGenericSignature(stateType);

                final ClassPool pool = classPool;
                final CtClass cc = pool.makeClass(newStateName);
                cc.setGenericSignature(genericSignature);
                final CtClass baseStateClass = pool.get(baseClass.getName());
                cc.setSuperclass(baseStateClass);

                final CtConstructor cons = CtNewConstructor.make(null, new CtClass[0], cc);
                cc.addConstructor(cons);

                cc.addInterface(pool.get(ActorState.class.getName()));


                return loadClass(cc, actorClass);
            }
            catch (final Exception ex)
            {
                throw new UncheckedException("Don't know how to handle state: " + stateType.getTypeName(), ex);
            }
        }
    }

    private static void unwrapParams(final StringBuilder invoker, final CtClass[] parameterTypes, final String paramsVar)
    {
        for (int i = 0; i < parameterTypes.length; i++)
        {
            invoker.append(i > 0 ? ",(" : "(");
            final CtClass parameterType = parameterTypes[i];
            if (parameterType.isPrimitive())
            {
                final CtPrimitiveType pt = (CtPrimitiveType) parameterType;
                invoker.append('(').append(pt.getWrapperName()).append(")")
                        .append(paramsVar).append("[").append(i).append("]).");
                invoker.append(pt.getGetMethodName()).append("()");
            }
            else
            {
                invoker.append(parameterType.getName()).append(")")
                        .append(paramsVar).append("[").append(i).append("]");
            }
        }
    }

    private static Class lookup(Class relatedClass, String className)
    {
        try
        {
            return Class.forName(className);
        }
        catch (final Exception ex)
        {
            // ignore;
        }
        try
        {
            return relatedClass.getClassLoader().loadClass(className);
        }
        catch (final Exception ex2)
        {
            // ignore;
        }
        final ConcurrentMap<String, Class> relatedClassMap = getRelatedClassMap(relatedClass);
        return relatedClassMap.get(className);
    }

}
