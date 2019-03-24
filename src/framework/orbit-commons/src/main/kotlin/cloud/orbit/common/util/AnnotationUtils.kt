/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.common.util

object AnnotationUtils {
    /**
     * Searches the class, supertype and interfaces recursively for an annotation.
     *
     * @param clazz The class to search on.
     * @param annotation The annotation to search for.
     * @return The instance of the annotation or null if no instance found.
     */
    @Suppress("UNCHECKED_CAST")
    fun <A : Annotation> findAnnotation(
        clazz: Class<*>,
        annotation: Class<A>
    ): A? = crawlHierarchy(clazz, annotation)

    private fun <A : Annotation> crawlHierarchy(clazz: Class<*>, annotation: Class<A>): A? {
        clazz.getAnnotation(annotation)?.also {
            return it
        }

        clazz.superclass?.also { nestedClazz ->
            crawlHierarchy(nestedClazz, annotation)?.also {
                return it
            }
        }

        clazz.interfaces.forEach { nestedClazz ->
            crawlHierarchy(nestedClazz, annotation)?.also {
                return it
            }
        }

        return null
    }
}