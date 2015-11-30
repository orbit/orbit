package com.ea.orbit.actors.annotation;

import java.lang.annotation.Annotation;

public interface IdGenerationStrategy
{
    int generateIdForClass(Annotation annotation, String classBinaryName);

    int generateIdForMethod(Annotation annotation, String methodSignature);

    int generateIdForField(Annotation annotation, String fieldSignature);
}
