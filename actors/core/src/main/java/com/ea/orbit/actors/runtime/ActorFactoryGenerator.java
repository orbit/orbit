package com.ea.orbit.actors.runtime;

import com.ea.orbit.actors.annotation.OneWay;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.CtPrimitiveType;
import javassist.NotFoundException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
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


    static
    {
        classPool = new ClassPool(null);
        classPool.appendSystemPath();
        classPool.appendClassPath(new ClassClassPath(ActorFactoryGenerator.class));
    }

    private static class GenericActorFactory<T> extends ActorFactory<T>
    {
        private int interfaceId;
        private Class<T> interfaceClass;
        private ActorInvoker<T> invoker;
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
        public ActorInvoker<T> getInvoker()
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
    public <T> ActorFactory<T> getFactoryFor(final Class<T> aInterface)
    {
        final String interfaceFullName = aInterface.getName().replace('$', '.');

        final int interfaceId = interfaceFullName.hashCode();
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
        Class clazz = lookup(referenceFullName);
        if (clazz != null)
        {
            return clazz;
        }

        synchronized (aInterface)
        {
            // trying again from within the synchronized block.
            clazz = lookup(referenceFullName);
            if (clazz != null)
            {
                return clazz;
            }

            final ClassPool pool = classPool;

            final CtClass cc = pool.makeClass(referenceFullName);
            final CtClass ccInterface = pool.get(aInterface.getName());
            final CtClass ccActorReference = pool.get(ActorReference.class.getName());
            cc.setSuperclass(ccActorReference);
            cc.addInterface(ccInterface);
            cc.addConstructor(CtNewConstructor.make(new CtClass[]{pool.get(String.class.getName())}, null, "{ super($1); }", cc));

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

                final String methodSignature = methodName + "(" + Stream.of(parameterTypes).map(p -> p.getName()).collect(Collectors.joining(",")) + ")";
                final int methodId = methodSignature.hashCode();

                final String methodReferenceField = methodName + "_" + count;
                cc.addField(CtField.make("private static java.lang.reflect.Method " + methodReferenceField + " = null;", cc));

                // TODO: move this to a static initializer
                final String lazyMethodReferenceInit = "(" + methodReferenceField + "!=null) ? " + methodReferenceField + " : ( "
                        + methodReferenceField + "=" + aInterface.getName() + ".class.getMethod(\"" + methodName + "\",$sig) )";

                // TODO: remove the method parameter from the invoke, this could be an utility method of ActorReference
                final CtMethod newMethod = CtNewMethod.make(m.getReturnType(), methodName,
                        parameterTypes, m.getExceptionTypes(),
                        "{ return super.invoke(" + lazyMethodReferenceInit + ", " + oneWay + ", " + methodId + ", $args);  }",
                        cc);
                cc.addMethod(newMethod);
            }
            cc.addMethod(CtNewMethod.make("protected int _interfaceId() { return " + interfaceId + ";}", cc));
            cc.addMethod(CtNewMethod.make("protected Class  _interfaceClass() { return " + interfaceFullName + ".class;}", cc));
            return cc.toClass();
        }
    }

    public int getMethodId(Method method)
    {
        final Class<?>[] parameterTypes = method.getParameterTypes();
        final String methodSignature = method.getName() + "(" + Stream.of(parameterTypes).map(p -> p.getName()).collect(Collectors.joining(",")) + ")";
        return methodSignature.hashCode();
    }

    public ActorInvoker<Object> getInvokerFor(final Class<?> concreteClass)
    {
        try
        {
            final Class<?> aClass = makeInvokerClass(concreteClass, concreteClass.getName() + "$Invoker");
            return (ActorInvoker<Object>) aClass.newInstance();
        }
        catch (Exception e)
        {
            throw new UncheckedException(e);
        }
    }

    private <T> Class<?> makeInvokerClass(final Class<T> actorClass, final String invokerFullName)
            throws NotFoundException, CannotCompileException
    {
        Class clazz = lookup(invokerFullName);
        if (clazz != null)
        {
            return clazz;
        }

        synchronized (actorClass)
        {
            // trying again from within the synchronized block.
            clazz = lookup(invokerFullName);
            if (clazz != null)
            {
                return clazz;
            }
            ClassPool pool = classPool;

            final CtClass cc = pool.makeClass(invokerFullName);
            final CtClass ccInterface = pool.get(actorClass.getName());
            final CtClass ccActorInvoker = pool.get(ActorInvoker.class.getName());
            cc.setSuperclass(ccActorInvoker);

            final StringBuilder sb = new StringBuilder(2000);
            sb.append("public " + Task.class.getName() + " invoke(Object target, int methodId, Object[] params) {");
            final CtMethod[] declaredMethods = ccInterface.getMethods();
            sb.append(" switch(methodId) { ");
            for (final CtMethod m : declaredMethods)
            {
                if (!m.getReturnType().getName().equals(Task.class.getName()))
                {
                    continue;
                }
                final CtClass[] parameterTypes = m.getParameterTypes();
                final String methodSignature = m.getName() + "(" + Stream.of(parameterTypes)
                        .map(p -> p.getName())
                        .collect(Collectors.joining(",")) + ")";
                final int methodId = methodSignature.hashCode();
                sb.append("case " + methodId + ": return ((" + actorClass.getName() + ")target)." + m.getName() + "(");

                for (int i = 0; i < parameterTypes.length; i++)
                {


                    sb.append(i > 0 ? ",(" : "(");
                    if (parameterTypes[i].isPrimitive())
                    {
                        final CtPrimitiveType pt = (CtPrimitiveType) parameterTypes[i];
                        sb.append('(').append(pt.getWrapperName()).append(")params[").append(i).append("]).");
                        sb.append(pt.getGetMethodName()).append("()");
                    }
                    else
                    {
                        sb.append(parameterTypes[i].getName()).append(")params[").append(i).append("]");
                    }
                }
                sb.append("); ");
            }
            sb.append("default: ");
            sb.append("return super.invoke(target,methodId,params);} }");
            cc.addMethod(CtNewMethod.make(sb.toString(), cc));
            return cc.toClass();
        }
    }

    private Class lookup(String className)
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
            return classPool.getClassLoader().loadClass(className);
        }
        catch (final Exception ex2)
        {
            // ignore;
        }
        return null;
    }

}
