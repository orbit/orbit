/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package orbit.util.reflect.jvm

object AnnotationUtils {
    /**
     * Searches the class, supertype and interfaces recursively for an annotation.
     *
     * @param clazz The class to search on.
     * @param annotation The annotation to search for.
     * @return The instance of the annotation or null if no instance found.
     */
    fun <A : Annotation> findAnnotationInTree(
        clazz: Class<*>,
        annotation: Class<A>
    ): A? = crawlHierarchy(clazz, annotation)

    /**
     * Searches the class, supertype and interfaces recursively for an annotation.
     *
     * @param clazz The class to search on.
     * @param annotation The annotation to search for.
     * @return True if the annotation is found. otherwise false.
     */
    fun <A : Annotation> isAnnotationInTree(
        clazz: Class<*>,
        annotation: Class<A>
    ): Boolean = crawlHierarchy(clazz, annotation) != null

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