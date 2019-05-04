/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl

import cloud.orbit.dsl.ast.ActorMethod
import cloud.orbit.dsl.ast.AstVisitor
import cloud.orbit.dsl.ast.DataField
import cloud.orbit.dsl.ast.MethodParameter
import cloud.orbit.dsl.ast.Type

/**
 * An AST visitor that runs a collection of type checks against each type reference in the AST.
 *
 * @param typeChecks the type checks to run.
 */
class TypeCheckingVisitor(private val typeChecks: Collection<TypeCheck>) : AstVisitor() {
    override fun visitDataField(field: DataField) {
        typeChecks.forEach {
            it.check(field.type, TypeCheck.Context.DATA_FIELD, errorReporter = this)
        }

        super.visitDataField(field)
    }

    override fun visitActorMethod(method: ActorMethod) {
        typeChecks.forEach {
            it.check(method.returnType, TypeCheck.Context.METHOD_RETURN, errorReporter = this)
        }

        super.visitActorMethod(method)
    }

    override fun visitMethodParameter(methodParameter: MethodParameter) {
        typeChecks.forEach {
            it.check(methodParameter.type, TypeCheck.Context.METHOD_PARAMETER, errorReporter = this)
        }

        super.visitMethodParameter(methodParameter)
    }

    override fun visitType(type: Type) {
        // Don't check type itself since we know it's already been checked by a parent visit method
        type.of.forEach { typeParameter ->
            typeChecks.forEach {
                it.check(typeParameter, TypeCheck.Context.TYPE_PARAMETER, errorReporter = this)
            }
        }

        super.visitType(type)
    }
}
