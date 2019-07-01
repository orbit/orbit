/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.type.check

import cloud.orbit.dsl.ast.CompilationUnit
import cloud.orbit.dsl.error.OrbitDslCompilationException

object OrbitDslTypeChecker {
    private val checks = listOf(
        ActorKeyTypeCheck(),
        VoidUsageCheck(),
        TypeArityCheck(
            mapOf(
                "list" to TypeDescriptor("list", 1),
                "map" to TypeDescriptor("map", 2)
            )
        )
    )

    fun checkTypes(compilationUnits: List<CompilationUnit>) {
        val errorListener = TypeErrorListener()
        val typeCheckingVisitor = TypeCheckingVisitor(
            checks,
            errorListener
        )

        compilationUnits.forEach {
            typeCheckingVisitor.visitCompilationUnit(it)
        }

        if (errorListener.typeErrors.isNotEmpty()) {
            throw OrbitDslCompilationException(errorListener.typeErrors)
        }
    }
}
