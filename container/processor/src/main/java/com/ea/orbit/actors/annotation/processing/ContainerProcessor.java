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

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SupportedAnnotationTypes({
        "*"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ContainerProcessor extends AbstractProcessor
{
    private static final String JAVA_LANG_OBJECT = java.lang.Object.class.getName();
    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;

    private Map<String, TypeInfo> map = new LinkedHashMap<>();

    private static class TypeInfo
    {
        String packageName;
        List<String> directAnnotations = new ArrayList<>();
        List<String> directInterfaces = new ArrayList<>();
        LinkedHashSet<String> superClasses = new LinkedHashSet<>();
        List<String> annotations = new ArrayList<>();
        Set<String> interfaces = new LinkedHashSet<>();
        String simpleName;
        TypeElement element;
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv)
    {
        this.filer = processingEnv.getFiler();
        this.typeUtils = processingEnv.getTypeUtils();
        this.elementUtils = processingEnv.getElementUtils();
        for (Element e : roundEnv.getRootElements())
        {
            if (e instanceof TypeElement)
            {
                addElement((TypeElement) e);
            }
        }
        if (!roundEnv.processingOver())
        {
            return false;
        }

        Map<String, TypeInfo> modules = new LinkedHashMap<>();

        for (final Map.Entry<String, TypeInfo> e : map.entrySet())
        {
            if (e.getValue().superClasses.contains("com.ea.orbit.container.Module"))
            {
                modules.put(e.getKey(), e.getValue());
            }
        }

        /// looking for older modules
        try
        {
            final FileObject root = filer.getResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/orbit/_");
            final File rootFile = new File(root.toUri()).getParentFile();

            final String[] list = rootFile !=null ? rootFile.list() : null;
            if (list != null)
            {
                for (String className : list)
                {
                    if (!modules.containsKey(className))
                    {
                        TypeElement e = elementUtils.getTypeElement(className);
                        if (e != null)
                        {
                            addElement(e);
                        }
                    }
                }
            }
            for (final Map.Entry<String, TypeInfo> e : map.entrySet())
            {
                if (e.getValue().superClasses.contains("com.ea.orbit.container.Module"))
                {
                    modules.put(e.getKey(), e.getValue());
                }
            }

        }
        catch (Exception e)
        {
            // ignore
        }

        for (final TypeInfo typeInfo : map.values())
        {
            if (typeInfo.superClasses.contains("com.ea.orbit.container.Module"))
            {
                try
                {
                    Set<String> classes = new LinkedHashSet<>();
                    for (TypeInfo e2 : map.values())
                    {
                        if (e2.packageName.startsWith(typeInfo.packageName))
                        {
                            classes.add(elementUtils.getBinaryName(e2.element).toString());
                        }
                    }
                    // works around partial compilation
                    try
                    {
                        final FileObject old = filer.getResource(StandardLocation.CLASS_OUTPUT, typeInfo.packageName, typeInfo.simpleName + ".moduleClasses");
                        try (BufferedReader br = new BufferedReader(old.openReader(true)))
                        {
                            br.lines().filter(x -> !classes.contains(x)).forEach(x -> {

                                try
                                {
                                    if (elementUtils.getTypeElement(x) != null)
                                    {
                                        int i = x.lastIndexOf(".");
                                        final FileObject oldClass = filer.getResource(StandardLocation.CLASS_OUTPUT, i > 0 ? x.substring(0, i) : "", (i > 0 ? x.substring(i + 1) : x) + ".class");
                                        if (oldClass != null)
                                        {
                                            try (InputStream in = oldClass.openInputStream())
                                            {
                                                if (in.read() > 0)
                                                {
                                                    classes.add(x);
                                                }
                                            }
                                        }
                                    }
                                }
                                catch (IOException e1)
                                {
                                    // ignore
                                    processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, e1.getMessage(), typeInfo.element);
                                }
                            });
                        }
                    }
                    catch (Exception ex)
                    {
                        // ignore
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, ex.getMessage(), typeInfo.element);
                    }
                    final FileObject res = filer.createResource(StandardLocation.CLASS_OUTPUT, typeInfo.packageName, typeInfo.simpleName + ".moduleClasses", typeInfo.element);
                    try (final PrintWriter writer = new PrintWriter(res.openWriter()))
                    {
                        classes.forEach(x -> writer.println(x));
                    }
                    final FileObject res2 = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/orbit/" + typeInfo.element.getQualifiedName(), typeInfo.element);
                    try (final PrintWriter writer = new PrintWriter(res2.openWriter()))
                    {
                        writer.append(typeInfo.element.getQualifiedName());
                    }
                }
                catch (IOException e1)
                {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error processing " + e1.getMessage(), typeInfo.element);
                }
            }
        }
        return false;
    }

    private void addElement(final TypeElement e)
    {
        final TypeInfo typeInfo = new TypeInfo();
        typeInfo.packageName = elementUtils.getPackageOf(e).getQualifiedName().toString();
        typeInfo.simpleName = e.getSimpleName().toString();
        typeInfo.element = e;
        for (AnnotationMirror am : e.getAnnotationMirrors())
        {
            typeInfo.directAnnotations.add(((TypeElement) am.getAnnotationType().asElement()).getQualifiedName().toString());
        }
        for (AnnotationMirror am : elementUtils.getAllAnnotationMirrors(e))
        {
            typeInfo.annotations.add(((TypeElement) am.getAnnotationType().asElement()).getQualifiedName().toString());
        }

        for (TypeMirror i : e.getInterfaces())
        {
            final Element ie = typeUtils.asElement(i);
            if (ie instanceof TypeElement)
            {
                typeInfo.directInterfaces.add(((TypeElement) ie).getQualifiedName().toString());
            }
            captureInterfaces(typeInfo, i);
        }

        for (TypeMirror s = e.getSuperclass(); s != null; )
        {
            final Element es = typeUtils.asElement(s);
            if (es instanceof TypeElement)
            {
                final TypeElement tes = (TypeElement) es;
                final String qName = tes.getQualifiedName().toString();
                if (!JAVA_LANG_OBJECT.equals(qName))
                {
                    typeInfo.superClasses.add(qName);
                    s = tes.getSuperclass();
                    continue;
                }
            }
            break;
        }

        map.put(e.getQualifiedName().toString(), typeInfo);

        for (Element sub : e.getEnclosedElements())
        {
            if (sub instanceof TypeElement)
            {
                addElement((TypeElement) sub);
            }
        }
    }

    private void captureInterfaces(final TypeInfo typeInfo, final TypeMirror i)
    {
        final Element e = typeUtils.asElement(i);
        if (e instanceof TypeElement)
        {
            final TypeElement te = (TypeElement) e;
            final String qualifiedName = te.getQualifiedName().toString();
            if (!typeInfo.interfaces.contains(qualifiedName))
            {
                typeInfo.interfaces.add(qualifiedName);

                for (TypeMirror sub : te.getInterfaces())
                {
                    captureInterfaces(typeInfo, sub);
                }
            }
        }
    }

}
