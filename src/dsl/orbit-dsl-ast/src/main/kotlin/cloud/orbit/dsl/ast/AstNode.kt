/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.ast

abstract class AstNode {
    private val annotations = mutableMapOf<Class<*>, AstAnnotation>()

    fun annotate(annotation: AstAnnotation) {
        this.annotations[annotation.javaClass] = annotation
    }

    inline fun <reified T : AstAnnotation> getAnnotation(): T? = getAnnotation(T::class.java)

    @Suppress("UNCHECKED_CAST")
    fun <T : AstAnnotation> getAnnotation(type: Class<T>): T? = annotations[type] as T?
}

inline fun <reified T : AstNode> T.annotated(annotation: AstAnnotation) =
    this.also {
        this.annotate(annotation)
    }

