/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.common.ref

import java.lang.ref.SoftReference as JDKSoftReference
import java.lang.ref.WeakReference as JDKWeakReference

/**
 * The type of reference.
 * References have different runtime behaviors on the garbage collector.
 *
 * @see Reference
 */
enum class ReferenceType {
    /**
     * A hard reference.
     * Prevents this object being GCed.
     *
     * @see Reference.hardReference
     */
    HARD_REFERENCE,

    /**
     * A weak reference.
     * The object may be GCed if only weak references exist.
     *
     * @see Reference.weakReference
     */
    WEAK_REFERENCE,

    /**
     * A soft reference.
     * Object will typically not be GCed but may be during resource contention (such as high memory usage).
     *
     * @see Reference.softReference
     */
    SOFT_REFERENCE
}

/**
 * A reference that determines runtime behavior of the garbage collector.
 *
 * @see ReferenceType
 */
sealed class Reference<T> {
    /**
     * The type of reference.
     */
    abstract val referenceType: ReferenceType

    /**
     * Gets the object.
     *
     * @return The object or null if reference is no longer valid
     */
    abstract fun get(): T?

    private class HardReference<T>(private val obj: T) : Reference<T>() {
        override val referenceType = ReferenceType.HARD_REFERENCE
        override fun get(): T? = obj
    }

    private class WeakReference<T>(obj: T) : Reference<T>() {
        override val referenceType = ReferenceType.WEAK_REFERENCE
        override fun get(): T? = weakRef.get()

        private val weakRef = JDKWeakReference(obj)
    }

    private class SoftReference<T>(obj: T) : Reference<T>() {
        override val referenceType = ReferenceType.SOFT_REFERENCE
        override fun get(): T? = softRef.get()

        private val softRef = JDKSoftReference(obj)
    }

    companion object {
        /**
         * Creates a reference with the given type and value.
         *
         * @param T The type of object the reference refers to.
         * @param obj The object.
         * @return The reference.
         * @see ReferenceType.HARD_REFERENCE
         */
        @JvmStatic
        fun <T> hardReference(obj: T): Reference<T> = HardReference(obj)

        /**
         * Creates a reference with the given type and value.
         *
         * @param T The type of object the reference refers to.
         * @param obj The object.
         * @return The reference.
         * @see ReferenceType.WEAK_REFERENCE
         */
        @JvmStatic
        fun <T> weakReference(obj: T): Reference<T> = WeakReference(obj)

        /**
         * Creates a reference with the given type and value.
         *
         * @param T The type of object the reference refers to.
         * @param pbj The object.
         * @return The reference.
         * @see ReferenceType.SOFT_REFERENCE
         */
        @JvmStatic
        fun <T> softReference(pbj: T): Reference<T> = SoftReference(pbj)
    }
}