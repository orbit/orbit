/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

import cloud.orbit.dsl.ast.AstVisitor
import cloud.orbit.dsl.ast.Type

/**
 * A type check for a single [Type] AST node.
 *
 * Derived classes should not visit type parameters. The invoking [TypeCheckingVisitor] is responsible for that.
 *
 */
abstract class TypeCheck : AstVisitor() {
    /**
     * Runs a check against a type.
     *
     * @param type the type to check.
     */
    abstract fun check(type: Type)

    override fun visitType(type: Type) {
        check(type)
    }
}
