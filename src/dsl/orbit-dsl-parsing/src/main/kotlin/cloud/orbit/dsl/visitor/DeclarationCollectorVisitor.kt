/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.visitor

import cloud.orbit.dsl.OrbitDslBaseVisitor
import cloud.orbit.dsl.OrbitDslParser
import cloud.orbit.dsl.OrbitDslVisitor
import cloud.orbit.dsl.ast.Declaration

class DeclarationCollectorVisitor<T : Declaration>(
    private val declarationVisitor: OrbitDslVisitor<T>
) : OrbitDslBaseVisitor<Collection<T>>() {
    override fun defaultResult() = emptyList<T>()

    override fun aggregateResult(aggregate: Collection<T>, nextResult: Collection<T>) =
        aggregate + nextResult

    override fun visitDeclaration(ctx: OrbitDslParser.DeclarationContext): Collection<T> {
        val result = ctx.accept(declarationVisitor)

        return if (result != null) {
            listOf(result)
        } else {
            emptyList()
        }
    }
}
