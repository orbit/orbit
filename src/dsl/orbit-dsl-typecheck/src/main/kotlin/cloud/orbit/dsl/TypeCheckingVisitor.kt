/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

import cloud.orbit.dsl.ast.AstNode
import cloud.orbit.dsl.ast.AstVisitor
import cloud.orbit.dsl.ast.ErrorListener
import cloud.orbit.dsl.ast.Type

/**
 * An AST visitor that runs a collection of type checks against each type visited.
 *
 * Errors reported by type checks are propagated to any [ErrorListener] instances registered against this visitor.
 *
 * @param typeChecks the checks to run against each type visited in the AST.
 */
class TypeCheckingVisitor(private val typeChecks: Collection<TypeCheck>) : AstVisitor(), ErrorListener {
    init {
        typeChecks.forEach {
            it.addErrorListener(this)
        }
    }

    override fun visitType(type: Type) {
        typeChecks.forEach { it.visitNode(type) }
        super.visitType(type)
    }

    override fun onError(astNode: AstNode, message: String) {
        reportError(astNode, message)
    }
}
