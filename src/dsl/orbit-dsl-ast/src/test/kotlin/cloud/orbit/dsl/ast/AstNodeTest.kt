/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.ast

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class AstNodeTest {
    private class TestAnnotation : AstAnnotation
    private class AnotherTestAnnotation : AstAnnotation

    @Test
    fun astNodeCanBeAnnotated() {
        val astNode = object : AstNode() {}
        val annotation = TestAnnotation()

        astNode.annotate(annotation)

        Assertions.assertSame(annotation, astNode.getAnnotation<TestAnnotation>())
        Assertions.assertSame(annotation, astNode.getAnnotation(TestAnnotation::class.java))
    }

    @Test
    fun supportsSingleAnnotationPerType() {
        val astNode = object : AstNode() {}
        val annotation1 = TestAnnotation()
        val annotation2 = TestAnnotation()
        val annotation3 = AnotherTestAnnotation()

        astNode.annotate(annotation1)
        astNode.annotate(annotation2)
        astNode.annotate(annotation3)

        Assertions.assertSame(annotation2, astNode.getAnnotation<TestAnnotation>())
        Assertions.assertSame(annotation3, astNode.getAnnotation<AnotherTestAnnotation>())
    }

    @Test
    fun getAnnotationReturnsNullWhenAstNodeNotAnnotatedWithType() {
        val astNode = object : AstNode() {}

        Assertions.assertNull(astNode.getAnnotation<TestAnnotation>())
    }

    @Test
    fun annotatedReturnsSameNodeWithAnnotation() {
        val astNode = object : AstNode() {}
        val annotation = TestAnnotation()

        val returnedNode = astNode.annotated(annotation)

        Assertions.assertSame(astNode, returnedNode)
        Assertions.assertSame(annotation, astNode.getAnnotation<TestAnnotation>())
    }
}
