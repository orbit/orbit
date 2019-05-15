/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.java

import cloud.orbit.dsl.ast.AstVisitor
import cloud.orbit.dsl.ast.CompilationUnit
import cloud.orbit.dsl.ast.Declaration
import cloud.orbit.dsl.ast.TypeReference
import cloud.orbit.dsl.type.PrimitiveType
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName

internal class TypeIndexer : AstVisitor() {
    private val types = mutableMapOf(
        TypeReference(PrimitiveType.BOOLEAN) to TypeName.BOOLEAN,
        TypeReference(PrimitiveType.DOUBLE) to TypeName.DOUBLE,
        TypeReference(PrimitiveType.FLOAT) to TypeName.FLOAT,
        TypeReference(PrimitiveType.INT32) to TypeName.INT,
        TypeReference(PrimitiveType.INT64) to TypeName.LONG,
        TypeReference(PrimitiveType.LIST) to ClassName.get(java.util.List::class.java),
        TypeReference(PrimitiveType.MAP) to ClassName.get(java.util.Map::class.java),
        TypeReference(PrimitiveType.STRING) to ClassName.get(String::class.java),
        TypeReference(PrimitiveType.VOID) to ClassName.get(Void::class.java)
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
        types[TypeReference(declaration.name)] = ClassName.get(packageName, declaration.name)
    }
}
