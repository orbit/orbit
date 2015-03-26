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

package com.ea.orbit.actors.annotation.processing;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes({
        "*"
})

@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ActorProcessor extends AbstractProcessor
{
    public static final String ACTOR_INTERFACE = "com.ea.orbit.actors.IActor";

    private static final String ACTOR_OBSERVER_INTERFACE = "com.ea.orbit.actors.IActorObserver";

    private static final String GENERIC_TASK = "com.ea.orbit.concurrent.Task";
    private static final String VOID_TASK = "com.ea.orbit.concurrent.Task<java.lang.Void>";

    private static final String AT_JAVA_LANG_OVERRIDE = "@java.lang.Override";
    private static final String AT_ONE_WAY_ANNOTATION = "@com.ea.orbit.actors.annotation.OneWay";

    private static final String GENERATED_ANNOTATION = "com.ea.orbit.annotation.Generated";
    private static final String ORBIT_GENERATED_ANNOTATION = "com.ea.orbit.actors.annotation.OrbitGenerated";
    private static final String STATELESS_WORKER_ANNOTATION = "com.ea.orbit.actors.annotation.StatelessWorker";


    private Elements elementUtils;
    private Filer filer;
    private VelocityEngine velocityEngine;
    private Template actorFactoryTemplate;


    public class ClassFile
    {
        TypeElement original;
        CharSequence packageName;
        CharSequence simpleName;
        List<CharSequence> _extends = new ArrayList<CharSequence>();
        List<CharSequence> _implements = new ArrayList<CharSequence>();
        List<String> annotations = new ArrayList<String>();
        List<MethodDefinition> methods = new ArrayList<>();
        String javaDoc;

        public CharSequence getPackageName()
        {
            return packageName;
        }

        public List<MethodDefinition> getMethods()
        {
            return methods;
        }

        public ClassFile(final TypeElement clazz, final List<ExecutableElement> methods)
        {
            annotations.add("@" + GENERATED_ANNOTATION);
            original = clazz;
            packageName = elementUtils.getPackageOf(clazz).getQualifiedName().toString();
            javaDoc = elementUtils.getDocComment(clazz);
            for (ExecutableElement method : methods)
            {
                MethodDefinition methodDefinition = new MethodDefinition();
                methodDefinition.original = method;
                methodDefinition.name = method.getSimpleName().toString();
                methodDefinition.javaDoc = elementUtils.getDocComment(method);
                methodDefinition.returnType = method.getReturnType().toString();
                int index = 0;
                for (VariableElement parameter : method.getParameters())
                {
                    final ParameterDefinition parameterDefinition = new ParameterDefinition();
                    parameterDefinition.original = parameter;
                    parameterDefinition.name = parameter.getSimpleName();
                    parameterDefinition.type = parameter.asType().toString();
                    parameterDefinition.index = index++;
                    methodDefinition.parameters.add(parameterDefinition);
                }
                this.methods.add(methodDefinition);
            }
        }


        public void addDoc(final String s)
        {
            javaDoc = (javaDoc == null) ? s : javaDoc + "\r\n" + s;
        }

        public String fullName()
        {
            return packageName + "." + simpleName;
        }
    }

    public static class MethodDefinition
    {
        ExecutableElement original;
        CharSequence name;
        String returnType;
        List<ParameterDefinition> parameters = new ArrayList<ParameterDefinition>();
        String javaDoc;
        List<String> annotations = new ArrayList<>();
        boolean oneway;

        public String methodId;

        public void addDoc(final String s)
        {
            javaDoc = (javaDoc == null) ? s : javaDoc + "\r\n" + s;
        }

        public CharSequence getReturnType()
        {
            return returnType;
        }

        public CharSequence getName()
        {
            return name;
        }

        public String getMethodId()
        {
            return methodId;
        }

        public String wrapParams()
        {
            return StringUtils.join(parameters.stream().map(p -> p.name).collect(Collectors.toList()), ", ");
        }

        public String unwrapParams(String varName)
        {
            return StringUtils.join(parameters.stream().map(
                    p -> "(" + p.type + ") (" + varName + "[" + p.index + "])").collect(Collectors.toList()), ", ");
        }

        public String paramsList()
        {
            return StringUtils.join(parameters.stream().map(p -> p.type + " " + p.name).collect(Collectors.toList()), ", ");
        }

        public boolean isOneway()
        {
            return oneway;
        }
    }

    private static class ParameterDefinition
    {
        public CharSequence name;
        public CharSequence type;
        public VariableElement original;
        public int index;
    }


    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv)
    {
        this.filer = processingEnv.getFiler();
        this.elementUtils = processingEnv.getElementUtils();
        processActors(roundEnv);
        return false;
    }

    private void processActors(final RoundEnvironment roundEnv)
    {
        final Set<? extends Element> rootElements = roundEnv.getRootElements();
        final List<TypeElement> classes = new ArrayList<>();
        final List<TypeElement> interfaces = new ArrayList<>();
        final Queue<Element> queue = new LinkedList<>(rootElements);
        while (queue.size() > 0)
        {
            final Element e = queue.remove();
            if (e instanceof TypeElement && !isAnnotatedWith(e, ORBIT_GENERATED_ANNOTATION))
            {
                if (e.getKind() == ElementKind.CLASS)
                {
                    classes.add((TypeElement) e);
                }
                else if (e.getKind() == ElementKind.INTERFACE)
                {
                    interfaces.add((TypeElement) e);
                }
                queue.addAll(e.getEnclosedElements());
            }
        }

        for (final TypeElement e : classes)
        {
            if (!isAnnotatedWith(e, ORBIT_GENERATED_ANNOTATION) && implementsIActor(e))
            {
                processActorClass(e);
            }
        }
        for (final TypeElement e : interfaces)
        {
            if (implementsIActor(e))
            {
                processActorInterface(e, true);
            }
            else if (implementsIActorObserver(e))
            {
                processActorInterface(e, false);
            }
        }

        final TypeElement statelessWorker = processingEnv.getElementUtils().getTypeElement(STATELESS_WORKER_ANNOTATION);
        for (Element e : roundEnv.getElementsAnnotatedWith(statelessWorker))
        {
            if (!isAnnotatedWith(e, ORBIT_GENERATED_ANNOTATION)
                    && (e.getKind() != ElementKind.INTERFACE || !implementsIActor((TypeElement) e)))
            {
                processingEnv.getMessager().printMessage(Kind.ERROR, "The @StatelessWorker annotation must be used only with the actor interfaces", e);
            }
        }

    }

    private boolean isAnnotatedWith(final Element e, final String annotation)
    {
        return e.getAnnotationMirrors().stream()
                .filter(r -> {
                    return annotation.equals(String.valueOf(((TypeElement) r.getAnnotationType().asElement()).getQualifiedName()));
                }).findAny().isPresent();
    }

    private void processActorInterface(final TypeElement clazz, final boolean isActor)
    {

        ClassFile classFile = new ClassFile(clazz,
                allMethods(clazz).stream()
                        .collect(Collectors.toList()));

        String baseName = clazz.getSimpleName().toString().replaceAll("^I", "");
        classFile.simpleName = clazz.getSimpleName();
        classFile.addDoc(" @see " + clazz.getQualifiedName());
        classFile.methods.forEach(m -> {
            m.parameters = m.parameters.stream()
                    .collect(Collectors.toList());

            String remoteMethodSignature = m.original.getSimpleName() + "(" + StringUtils.join(
                    m.parameters.stream().map(p -> p.original.asType().toString()).collect(Collectors.toList()),
                    ",") + ")";
            m.methodId = "" + remoteMethodSignature.hashCode();

            m.addDoc(" @see " + m.original.getEnclosingElement() + "#" + m.name);
            m.annotations = copyAnnotations(m.original).stream()
                    .filter(a -> !a.contains(AT_JAVA_LANG_OVERRIDE))
                    .collect(Collectors.toList());

            if ("void".equals(m.returnType.toString()))
            {
                processingEnv.getMessager().printMessage(Kind.ERROR, "void methods are not allowed, use Task<Void> instead", m.original);
            }
            if (m.annotations.contains(AT_ONE_WAY_ANNOTATION))
            {
                m.oneway = true;
                if (!VOID_TASK.equals(m.returnType) && !GENERIC_TASK.equals(m.returnType))
                {
                    processingEnv.getMessager().printMessage(Kind.ERROR, "@Oneway methods are not allowed, use typed Tasks, use Task<Void> instead", m.original);
                }
            }
            if (!m.returnType.startsWith(GENERIC_TASK))
            {
                processingEnv.getMessager().printMessage(Kind.ERROR, "remote actor methods must return " + GENERIC_TASK, m.original);
            }
        });
        classFile.annotations = copyAnnotations(classFile.original).stream()
                .collect(Collectors.toList());

        classFile.annotations.add("@" + GENERATED_ANNOTATION);

        try
        {
            String factoryName = baseName + "Factory";
            {
                JavaFileObject res = filer.createSourceFile(classFile.packageName + "." + factoryName, classFile.original);


                VelocityContext context = new VelocityContext();
                String interfaceFullName = classFile.original.getQualifiedName().toString();
                context.put("clazz", classFile);
                context.put("interfaceId", interfaceFullName.hashCode());
                context.put("methods", classFile.methods);
                context.put("referenceName", baseName + "Reference");
                context.put("invokerName", baseName + "Invoker");
                context.put("factoryName", factoryName);
                context.put("interfaceFullName", interfaceFullName);
                context.put("isActor", isActor);
                StringWriter writer = new StringWriter();

                getActorFactoryTemplate().merge(context, writer);

                try (Writer w = res.openWriter())
                {
                    w.append(writer.toString());
                }
            }
            // write actor list
            {
                String output = elementUtils.getBinaryName(clazz).toString();
                FileObject res = filer.createResource(
                        StandardLocation.CLASS_OUTPUT,
                        "",
                        "META-INF/orbit/actors/interfaces/" + output,
                        clazz);
                try (Writer w = res.openWriter())
                {
                    w.append(classFile.getPackageName() + "." + factoryName);
                }
            }
        }
        catch (IOException e1)
        {
            processingEnv.getMessager().printMessage(Kind.ERROR, e1.getLocalizedMessage(), clazz);
        }
    }

    private synchronized Template getActorFactoryTemplate()
    {
        if (actorFactoryTemplate == null)
        {
            actorFactoryTemplate = getVelocityEngine().getTemplate("/" + getClass().getPackage().getName().replace('.', '/') + "/ActorFactoryTemplate.vm");
        }
        return actorFactoryTemplate;
    }


    private synchronized VelocityEngine getVelocityEngine()
    {
        if (velocityEngine == null)
        {
            velocityEngine = new VelocityEngine();
            velocityEngine.setProperty("file.resource.loader.class", ClasspathResourceLoader.class.getName());
            velocityEngine.init();
        }
        return velocityEngine;
    }

    private void processActorClass(final TypeElement e)
    {
        final TypeElement clazz = (TypeElement) e;
        try
        {
            final String binaryName = elementUtils.getBinaryName(clazz).toString();
            String output = binaryName;
            FileObject res = filer.createResource(
                    StandardLocation.CLASS_OUTPUT,
                    "",
                    "META-INF/orbit/actors/classes/" + output,
                    clazz);
            try (Writer w = res.openWriter())
            {
                w.append(binaryName);
            }
        }
        catch (IOException e1)
        {
            processingEnv.getMessager().printMessage(Kind.ERROR, e1.getLocalizedMessage(), clazz);
        }
    }

    private boolean implementsIActor(final TypeElement e)
    {
        return implementsInterface(e, ACTOR_INTERFACE);
    }

    private boolean implementsIActorObserver(final TypeElement e)
    {
        return implementsInterface(e, ACTOR_OBSERVER_INTERFACE);
    }

    private boolean implementsInterface(final TypeElement e, String interfaceName)
    {
        for (TypeMirror i : e.getInterfaces())
        {
            if ((i instanceof DeclaredType)
                    && interfaceName.equals(((TypeElement) ((DeclaredType) i).asElement()).getQualifiedName().toString()))
            {
                return true;
            }
        }
        for (TypeMirror i : e.getInterfaces())
        {
            if (i instanceof DeclaredType
                    && implementsInterface((TypeElement) ((DeclaredType) i).asElement(), interfaceName))
            {
                return true;
            }
        }
        return false;
    }

    private List<ExecutableElement> allMethods(final TypeElement clazz)
    {
        List<ExecutableElement> res = new ArrayList<>();
        for (Element e : elementUtils.getAllMembers(clazz))
        {
            if (e.getKind() == ElementKind.METHOD && !e.getEnclosingElement().getSimpleName().contentEquals("Object"))
            {
                res.add((ExecutableElement) e);
            }
        }
        return res;
    }

    private List<String> copyAnnotations(final Element e)
    {
        return e.getAnnotationMirrors().stream()
                .map(a -> a.toString()).collect(Collectors.toCollection(ArrayList::new));
    }

}
