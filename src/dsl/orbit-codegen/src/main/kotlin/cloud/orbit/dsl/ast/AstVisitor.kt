/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.ast

abstract class AstVisitor {
    open fun visitCompilationUnit(cu: CompilationUnit) {
        (cu.enums.asSequence() + cu.data.asSequence() + cu.actors.asSequence())
            .forEach { visitNode(it) }
    }

    open fun visitNode(node: AstNode) {
        when (node) {
            is Declaration -> visitDeclaration(node)
            is EnumMember -> visitEnumMember(node)
            is DataField -> visitDataField(node)
            is ActorMethod -> visitActorMethod(node)
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
    }

    open fun visitActorDeclaration(actor: ActorDeclaration) {
        actor.methods.forEach { visitNode(it) }
    }

    open fun visitActorMethod(method: ActorMethod) {
    }
}
