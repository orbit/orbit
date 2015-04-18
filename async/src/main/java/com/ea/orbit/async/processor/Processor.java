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

package com.ea.orbit.async.processor;

import com.ea.orbit.async.Async;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import java.util.Set;

/**
 * Annotation processor to detect misuses of {@literal@}Async
 */
@SupportedAnnotationTypes({ "com.orbit.async.Async" })
public class Processor extends AbstractProcessor
{
    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv)
    {
        final Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Async.class);
        for (Element e : elements)
        {
            if (e instanceof ExecutableElement)
            {
                ExecutableElement m = (ExecutableElement) e;
                final TypeMirror returnType = m.getReturnType();
                switch (returnType.getKind())
                {
                    case DECLARED:
                        final String str = returnType.toString();
                        if (!str.startsWith("java.lang.concurrent.CompletableFuture")
                                && !str.startsWith("com.ea.orbit.concurrent.Task"))
                        {
                            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                    "Methods annotated with @Async must return CompletableFuture or com.ea.orbit.concurrent.Task", e);
                        }
                        break;
                    default:
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                "Methods annotated with @Async must return CompletableFuture or com.ea.orbit.concurrent.Task", e);
                }
            }
        }

        return false;
    }
}
