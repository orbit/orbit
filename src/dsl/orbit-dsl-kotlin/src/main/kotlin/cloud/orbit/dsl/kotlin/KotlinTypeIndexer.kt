/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.kotlin

import cloud.orbit.dsl.ast.AstVisitor
import cloud.orbit.dsl.ast.CompilationUnit
import cloud.orbit.dsl.ast.Declaration
import cloud.orbit.dsl.ast.TypeReference
import cloud.orbit.dsl.type.PrimitiveType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

internal class KotlinTypeIndexer : AstVisitor() {
    private val types = mutableMapOf(
        TypeReference(PrimitiveType.BOOLEAN) to Boolean::class.asTypeName(),
        TypeReference(PrimitiveType.DOUBLE) to Double::class.asTypeName(),
        TypeReference(PrimitiveType.FLOAT) to Float::class.asTypeName(),
        TypeReference(PrimitiveType.GUID) to java.util.UUID::class.asTypeName(),
        TypeReference(PrimitiveType.INT32) to Int::class.asTypeName(),
        TypeReference(PrimitiveType.INT64) to Long::class.asTypeName(),
        TypeReference(PrimitiveType.LIST) to List::class.asClassName(),
        TypeReference(PrimitiveType.MAP) to Map::class.asClassName(),
        TypeReference(PrimitiveType.STRING) to String::class.asTypeName(),
        TypeReference(PrimitiveType.VOID) to Unit::class.asTypeName()
    )

    private var packageName: String = ""

    fun visitCompilationUnits(compilationUnits: List<CompilationUnit>): Map<TypeReference, TypeName> {
        compilationUnits.forEach { visitCompilationUnit(it) }
        return types
    }

    override fun visitCompilationUnit(cu: CompilationUnit) {
        packageName = cu.packageName
        super.visitCompilationUnit(cu)
    }

    override fun visitDeclaration(declaration: Declaration) {
        types[TypeReference(declaration.name)] = ClassName(packageName, declaration.name)
    }
}
