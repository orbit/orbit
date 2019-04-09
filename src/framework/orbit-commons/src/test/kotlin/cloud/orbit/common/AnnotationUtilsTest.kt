/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.common

import cloud.orbit.common.util.AnnotationUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AnnotationUtilsTest {
    annotation class CustomAnnotation
    private class ClassNoAnnotation
    private interface InterfaceNoAnnotation
    @CustomAnnotation
    private open class BasicAnnotated

    private class SuperClassAnnotated : BasicAnnotated()
    @CustomAnnotation
    private interface InterfaceAnnotated

    private class InheritedInterfaceAnnotated : InterfaceAnnotated

    @Test
    fun `check fails class no annotation`() {
        val result = AnnotationUtils.findAnnotationInTree(ClassNoAnnotation::class.java, CustomAnnotation::class.java)
        assertThat(result).isNull()
    }

    @Test
    fun `check fails interface no annotation`() {
        val result =
            AnnotationUtils.findAnnotationInTree(InterfaceNoAnnotation::class.java, CustomAnnotation::class.java)
        assertThat(result).isNull()
    }

    @Test
    fun `check passes basic class annotation`() {
        val result = AnnotationUtils.findAnnotationInTree(BasicAnnotated::class.java, CustomAnnotation::class.java)
        assertThat(result).isNotNull
    }

    @Test
    fun `check passes basic interface annotation`() {
        val result = AnnotationUtils.findAnnotationInTree(InterfaceAnnotated::class.java, CustomAnnotation::class.java)
        assertThat(result).isNotNull
    }

    @Test
    fun `check passes superclass annotation`() {
        val result = AnnotationUtils.findAnnotationInTree(SuperClassAnnotated::class.java, CustomAnnotation::class.java)
        assertThat(result).isNotNull
    }

    @Test
    fun `check passes inherited interface annotation`() {
        val result =
            AnnotationUtils.findAnnotationInTree(InheritedInterfaceAnnotated::class.java, CustomAnnotation::class.java)
        assertThat(result).isNotNull
    }
}