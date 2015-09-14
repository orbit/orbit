package com.ea.orbit.actors.runtime;

import com.ea.orbit.actors.annotation.OneWay;
import com.ea.orbit.actors.transactions.TransactionalState;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.exception.UncheckedException;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.googlecode.gentyref.GenericTypeReflector;

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
import javassist.util.proxy.ProxyFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
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

    protected static Class<?> makeStateClass(Class<? extends AbstractActor> actorClass)
    {
        final Type stateType = GenericTypeReflector.getTypeParameter(actorClass,
                AbstractActor.class.getTypeParameters()[0]);

        if (stateType == null)
        {
            return LinkedHashMap.class;
        }

        Class<?> c = stateClasses.get(stateType);
        if (c == null)
        {
            if (stateType instanceof Class)
            {
                c = (Class<?>) stateType;
            }
            else if (stateType instanceof ParameterizedType)
            {
                c = createSubclass(actorClass, (ParameterizedType) stateType);
            }
            else
            {
                throw new IllegalArgumentException("Don't know how to handler state type: " + stateType);
            }
            if(TransactionalState.class.isAssignableFrom(c))
            {
                // this is injecting fields that will cause problems with the serializers.
                final ProxyFactory pf = new ProxyFactory();
                pf.setSuperclass(c);
                c = pf.createClass();
            }
            final Class<?> old = stateClasses.putIfAbsent(stateType, c);

            if (old != null) c = old;
        }
        return c;
    }

    private static Class createSubclass(Class<? extends AbstractActor> actorClass, Type type)
    {
        final Class<?> erased = GenericTypeReflector.erase(type);
        Class<?> baseClass = erased.isInterface() ? Object.class : erased;
        final String genericSignature = GenericUtils.toGenericSignature(type);

        org.objectweb.asm.Type superType = org.objectweb.asm.Type.getType(baseClass);
        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        final String actorSig = actorClass.getName().replace('.', '/');
        final String simpleName = "State" + (genericSignature.hashCode() & 0xffff);
        String name = actorSig + "$" + simpleName;


        String superName = superType.getInternalName();
        cw.visit(Opcodes.V1_8, Opcodes.ACC_SUPER | Opcodes.ACC_PUBLIC, name, genericSignature, superName, null);


        cw.visitInnerClass(superName, actorSig, simpleName, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
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
                super(actorClass.getClassLoader());
            }

            public Class<?> define(final String o, final byte[] bytes)
            {
                return super.defineClass(o, bytes, 0, bytes.length);
            }
        }
        return new Loader().define(null, bytes);
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
