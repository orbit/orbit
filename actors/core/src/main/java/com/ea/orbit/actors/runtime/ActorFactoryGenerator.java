package com.ea.orbit.actors.runtime;

import com.ea.orbit.actors.annotation.OneWay;
import com.ea.orbit.actors.transactions.TransactionalEvent;
import com.ea.orbit.actors.transactions.TransactionalState;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;

import com.googlecode.gentyref.GenericTypeReflector;

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
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private final static Map<Type, Class<?>> stateClasses = new ConcurrentHashMap<>();


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
                unwrapParams(sb, parameterTypes, "params");
                sb.append("); ");
            }
            sb.append("default: ");
            sb.append("return super.invoke(target,methodId,params);} }");
            cc.addMethod(CtNewMethod.make(sb.toString(), cc));
            return cc.toClass();
        }
    }

    protected static Class<?> makeStateClass(Class<? extends AbstractActor> actorClass)
    {
        final Type stateType = GenericTypeReflector.getTypeParameter(actorClass,
                AbstractActor.class.getTypeParameters()[0]);

        if (stateType == null)
        {
            return LinkedHashMap.class;
        }

        Class<?> c = stateClasses.get(stateType);
        if (c != null)
        {
            return c;
        }
        synchronized (stateClasses)
        {
            c = stateClasses.get(stateType);
            if (c != null)
            {
                return c;
            }
            try
            {
                final Class<?> erased = GenericTypeReflector.erase(stateType);
                Class<?> baseClass = erased.isInterface() ? Object.class : erased;
                final String genericSignature = GenericUtils.toGenericSignature(stateType);

                final ClassPool pool = classPool;
                final CtClass cc = pool.makeClass(actorClass.getName() + "$ActorState");
                cc.setGenericSignature(genericSignature);
                final CtClass baseStateClass = pool.get(baseClass.getName());
                cc.setSuperclass(baseStateClass);

                final CtConstructor cons = CtNewConstructor.make(null, new CtClass[0], cc);
                cc.addConstructor(cons);

                cc.addInterface(pool.get(ActorState.class.getName()));


                if (TransactionalState.class.isAssignableFrom(erased))
                {
                    StringBuilder invokerBody = new StringBuilder();
                    invokerBody.append("{ switch($1.hashCode()) {");
                    int count = 0;
                    for (CtMethod m : baseStateClass.getMethods())
                    {
                        if (m.hasAnnotation(TransactionalEvent.class))
                        {
                            count++;
                            final String methodName = m.getName();
                            final CtClass[] parameterTypes = m.getParameterTypes();
                            final boolean isVoid = CtClass.voidType.equals(m.getReturnType());

                            //
                            // part of the invoker switch case:
                            // equivalent to:
                            //   case "methodName" : return super.methodName(args);
                            //
                            invokerBody.append("case ").append(m.getName().hashCode()).append(": "
                                    + "if($1.equals(\"" + m.getName() + "\")) { "
                                    + (isVoid ? " " : " return ($w)") + "super." + methodName + "(");
                            unwrapParams(invokerBody, parameterTypes, "$2");
                            invokerBody.append(");");
                            if (isVoid)
                            {
                                invokerBody.append("return null;");
                            }
                            invokerBody.append("} break;");

                            //
                            // replacement method, intercepts the event call and forwards it to the actor.
                            //
                            final String methodReferenceField = methodName + "_" + count;
                            cc.addField(CtField.make("private static java.lang.reflect.Method " + methodReferenceField + " = null;", cc));

                            final String lazyMethodReferenceInit = "(" + methodReferenceField + "!=null) ? " + methodReferenceField + " : ( "
                                    + methodReferenceField + "=" + baseStateClass.getName() + ".class.getDeclaredMethod(\"" + methodName + "\",$sig) )";


                            final CtMethod newMethod = CtNewMethod.make(
                                    m.getReturnType(), methodName, parameterTypes, m.getExceptionTypes(),
                                    "{ " + ActorTaskContext.class.getName() + " taskContext = " + ActorTaskContext.class.getName() + ".current();"
                                            + "if(taskContext !=null) {"
                                            + AbstractActor.class.getName() + " actor = taskContext.getActor();"
                                            + "if(actor != null) { "
                                            + " return ($r) actor.interceptStateMethod("
                                            + lazyMethodReferenceInit + ", \"" + methodName + "\", $args); "
                                            + "}}"
                                            + "throw new java.lang.IllegalStateException(\"Actor state is not ready\");"
                                            + "}",
                                    cc);
                            cc.addMethod(newMethod);
                        }
                    }
                    invokerBody.append("} return ((" + ActorState.class.getName() + ")super).invokeEvent($1, $2); }");
                    final CtMethod invoker = CtNewMethod.make(
                            pool.get(Object.class.getName()), "invokeEvent",
                            new CtClass[]{pool.get(String.class.getName()), pool.get(Object[].class.getName())},
                            new CtClass[0],
                            invokerBody.toString(), cc);

                    cc.addMethod(invoker);
                }
                c = cc.toClass();
                final Class<?> old = stateClasses.putIfAbsent(stateType, c);

                return (old != null) ? old : c;
            }
            catch (Exception ex)
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
