package com.ea.orbit.actors.runtime;

import com.ea.orbit.actors.annotation.OneWay;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.CtPrimitiveType;
import javassist.NotFoundException;

import java.lang.reflect.Constructor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class to create actor factories, actor references and actor invokers.
 * <p/>
 * This class allows the orbit actors to work without compile time code generation.
 * <ul>
 * <li>This potentially enables other jvm languages to use orbit.</li>
 * <li>It also makes it easier to use orbit with Eclipse (without configuring the annotation processors)</li>
 * </ul>
 * However, it's advisable to enable the annotation processor when possible make debugging easier.
 */
public class ActorFactoryGenerator
{
    private static ClassPool classPool;

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
        final String packageName = aInterface.getPackage().getName();
        final String baseName = aInterface.getSimpleName().replaceAll("^I", "");
        final int interfaceId = interfaceFullName.hashCode();
        final String referenceName = baseName + "Reference";
        final String invokerName = baseName + "Invoker";
        final String factoryName = baseName + "Factory";
        final String factoryFullName = packageName + "." + factoryName;
        final String referenceFullName = factoryFullName + "$" + referenceName;

        try
        {
            final Class<T> referenceClass = makeReferenceClass(aInterface, interfaceFullName, interfaceId, referenceFullName);
            final Class<ActorInvoker<T>> invokerClass = (Class<ActorInvoker<T>>) makeInvokerClass(aInterface, factoryFullName + "$" + invokerName);

            final GenericActorFactory<T> dyn = new GenericActorFactory<>();
            dyn.interfaceId = interfaceId;
            dyn.interfaceClass = aInterface;
            dyn.referenceConstructor = referenceClass.getConstructor(String.class);
            dyn.invoker = invokerClass.newInstance();
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
        try
        {
            return (Class<T>) Class.forName(referenceFullName);
        }
        catch (final Exception ex)
        {
            // ignore;
        }
        final ClassPool pool = getClassPool();
        try
        {
            return (Class<T>) pool.getClassLoader().loadClass(referenceFullName);
        }
        catch (final Exception ex2)
        {
            // ignore;
        }

        if ("ISomeChatObserver".equals(aInterface.getSimpleName()))
        {
            System.out.println();
        }

        synchronized (aInterface)
        {
            final CtClass cc = pool.makeClass(referenceFullName);
            final CtClass ccInterface = pool.get(aInterface.getName());
            final CtClass ccActorReference = pool.get(ActorReference.class.getName());
            cc.setSuperclass(ccActorReference);
            cc.addInterface(ccInterface);
            cc.addConstructor(CtNewConstructor.make(new CtClass[]{pool.get(String.class.getName())}, null, "{ super($1); }", cc));

            for (final CtMethod m : ccInterface.getMethods())
            {
                if (!m.getDeclaringClass().isInterface() || !m.getReturnType().getName().equals(Task.class.getName()))
                {
                    continue;
                }

                final boolean oneWay = m.hasAnnotation(OneWay.class);
                final String methodSignature = m.getName() + "(" +
                        Stream.of(m.getParameterTypes()).map(p -> p.getName()).collect(Collectors.joining(","))
                        + ")";
                final int methodId = methodSignature.hashCode();
                final CtMethod newMethod = CtNewMethod.make(m.getReturnType(), m.getName(),
                        m.getParameterTypes(), m.getExceptionTypes(),
                        "{ return super.invoke(" + oneWay + ", " + methodId + ", $args);  }",
                        cc);
                cc.addMethod(newMethod);
            }
            cc.addMethod(CtNewMethod.make("protected int _interfaceId() { return " + interfaceId + ";}", cc));
            cc.addMethod(CtNewMethod.make("protected Class  _interfaceClass() { return " + interfaceFullName + ".class;}", cc));
            return cc.toClass();
        }
    }

    private <T> Class<?> makeInvokerClass(final Class<T> aInterface, final String invokerFullName) throws NotFoundException, CannotCompileException
    {
        try
        {
            return Class.forName(invokerFullName);
        }
        catch (final Exception ex)
        {
            // ignore;
        }
        final ClassPool pool = getClassPool();
        try
        {
            return pool.getClassLoader().loadClass(invokerFullName);
        }
        catch (final Exception ex2)
        {
            // ignore;
        }

        synchronized (aInterface)
        {
            final CtClass cc = pool.makeClass(invokerFullName);
            final CtClass ccInterface = pool.get(aInterface.getName());
            final CtClass ccActorInvoker = pool.get(ActorInvoker.class.getName());
            cc.setSuperclass(ccActorInvoker);

            final StringBuilder sb = new StringBuilder();
            sb.append("public " + Task.class.getName() + " invoke(Object target, int methodId, Object[] params) {");
            final CtMethod[] declaredMethods = ccInterface.getMethods();
            sb.append(" switch(methodId) { ");
            for (final CtMethod m : declaredMethods)
            {
                if (!m.getDeclaringClass().isInterface() || !m.getReturnType().getName().equals(Task.class.getName()))
                {
                    continue;
                }
                final CtClass[] parameterTypes = m.getParameterTypes();
                final String methodSignature = m.getName() + "(" + Stream.of(parameterTypes)
                        .map(p -> p.getName())
                        .collect(Collectors.joining(",")) + ")";
                final int methodId = methodSignature.hashCode();
                sb.append("case " + methodId + ": return ((" + aInterface.getName() + ")target)." + m.getName() + "(");

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

    private static synchronized ClassPool getClassPool()
    {
        if (classPool == null)
        {
            classPool = new ClassPool(null);
            classPool.appendSystemPath();
            classPool.appendClassPath(new ClassClassPath(ActorFactoryGenerator.class));
        }

        return classPool;
    }

}
