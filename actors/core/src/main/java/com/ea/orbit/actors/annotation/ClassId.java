package com.ea.orbit.actors.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@IdStrategy(ExplicitStrategyId.class)
public @interface ClassId
{
    int value();
}
