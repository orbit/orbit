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
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName

internal class TypeIndexer : AstVisitor() {
    private val types = mutableMapOf(
        TypeReference("boolean") to TypeName.BOOLEAN,
        TypeReference("double") to TypeName.DOUBLE,
        TypeReference("float") to TypeName.FLOAT,
        TypeReference("int32") to TypeName.INT,
        TypeReference("int64") to TypeName.LONG,
        TypeReference("string") to ClassName.get(String::class.java),
        TypeReference("list") to ClassName.get(java.util.List::class.java),
        TypeReference("map") to ClassName.get(java.util.Map::class.java)
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
