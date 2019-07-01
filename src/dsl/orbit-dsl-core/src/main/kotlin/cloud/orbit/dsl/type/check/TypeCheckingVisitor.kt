/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.type.check

import cloud.orbit.dsl.ast.ActorDeclaration
import cloud.orbit.dsl.ast.ActorMethod
import cloud.orbit.dsl.ast.AstVisitor
import cloud.orbit.dsl.ast.DataField
import cloud.orbit.dsl.ast.ErrorListener
import cloud.orbit.dsl.ast.MethodParameter
import cloud.orbit.dsl.ast.TypeReference

/**
 * An AST visitor that runs a collection of type checks against each type reference in the AST.
 *
 * @param typeChecks the type checks to run.
 */
class TypeCheckingVisitor(
    private val typeChecks: Collection<TypeCheck>,
    private val errorListener: ErrorListener = ErrorListener.DEFAULT
) : AstVisitor(errorListener) {
    override fun visitDataField(field: DataField) {
        typeChecks.forEach {
            it.check(field.type, TypeCheck.Context.DATA_FIELD, errorReporter = this)
        }

        super.visitDataField(field)
    }

    override fun visitActorDeclaration(actor: ActorDeclaration) {
        typeChecks.forEach {
            it.check(actor.keyType, TypeCheck.Context.ACTOR_KEY, errorReporter = this)
        }

        super.visitActorDeclaration(actor)
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

    override fun visitTypeReference(typeReference: TypeReference) {
        // Don't check typeReference itself since we know it's already been checked by a parent visit method
        typeReference.of.forEach { typeParameter ->
            typeChecks.forEach {
                it.check(typeParameter, TypeCheck.Context.TYPE_PARAMETER, errorReporter = this)
            }
        }

        super.visitTypeReference(typeReference)
    }
}
