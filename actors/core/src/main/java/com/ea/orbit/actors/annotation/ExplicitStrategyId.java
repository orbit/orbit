package com.ea.orbit.actors.annotation;

import com.ea.orbit.exception.NotImplementedException;

import java.lang.annotation.Annotation;


public class ExplicitStrategyId implements IdGenerationStrategy
{
    @Override
    public int generateIdForClass(final Annotation annotation, final String classBinaryName)
    {
        return ((ClassId) annotation).value();
    }

    @Override
    public int generateIdForMethod(final Annotation annotation, final String methodSignature)
    {
        // TODO: return ((MethodId) annotation).value();
        throw new NotImplementedException("generateIdForMethod");
    }

    @Override
    public int generateIdForField(final Annotation annotation, final String fieldSignature)
    {
        // TODO: return ((FieldId) annotation).value();
        throw new NotImplementedException("generateIdForField");
    }
}
