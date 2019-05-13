/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.ast

import cloud.orbit.dsl.ast.error.ErrorListener
import cloud.orbit.dsl.ast.error.ErrorReporter

abstract class AstVisitor : ErrorReporter {
    private val errorListeners = mutableSetOf<ErrorListener>()

    open fun visitCompilationUnit(cu: CompilationUnit) {
        cu.enums.forEach { visitNode(it) }
        cu.data.forEach { visitNode(it) }
        cu.actors.forEach { visitNode(it) }
    }

    open fun visitNode(node: AstNode) {
        when (node) {
            is Declaration -> visitDeclaration(node)
            is EnumMember -> visitEnumMember(node)
            is DataField -> visitDataField(node)
            is ActorMethod -> visitActorMethod(node)
            is MethodParameter -> visitMethodParameter(node)
            is TypeReference -> visitTypeReference(node)
        }
    }

    open fun visitDeclaration(declaration: Declaration) {
        when (declaration) {
            is EnumDeclaration -> visitEnumDeclaration(declaration)
            is DataDeclaration -> visitDataDeclaration(declaration)
            is ActorDeclaration -> visitActorDeclaration(declaration)
        }
    }

    open fun visitEnumDeclaration(enum: EnumDeclaration) {
        enum.members.forEach { visitNode(it) }
    }

    open fun visitEnumMember(member: EnumMember) {
    }

    open fun visitDataDeclaration(data: DataDeclaration) {
        data.fields.forEach { visitNode(it) }
    }

    open fun visitDataField(field: DataField) {
        visitNode(field.type)
    }

    open fun visitActorDeclaration(actor: ActorDeclaration) {
        actor.methods.forEach { visitNode(it) }
    }

    open fun visitActorMethod(method: ActorMethod) {
        visitNode(method.returnType)
        method.params.forEach { visitNode(it) }
    }

    open fun visitMethodParameter(methodParameter: MethodParameter) {
        visitNode(methodParameter.type)
    }

    open fun visitTypeReference(typeReference: TypeReference) {
        typeReference.of.forEach { visitNode(it) }
    }

    fun addErrorListener(errorListener: ErrorListener) {
        errorListeners.add(errorListener)
    }

    fun removeErrorListener(errorListener: ErrorListener) {
        errorListeners.remove(errorListener)
    }

    override fun reportError(astNode: AstNode, message: String) {
        errorListeners.forEach {
            it.onError(astNode, message)
        }
    }
}
