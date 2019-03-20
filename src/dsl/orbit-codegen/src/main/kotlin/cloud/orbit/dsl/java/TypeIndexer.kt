/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.java

import cloud.orbit.dsl.ast.AstVisitor
import cloud.orbit.dsl.ast.CompilationUnit
import cloud.orbit.dsl.ast.Declaration
import cloud.orbit.dsl.ast.Type
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName

internal class TypeIndexer : AstVisitor() {
    private val types = mutableMapOf(
        Type("boolean") to TypeName.BOOLEAN,
        Type("double") to TypeName.DOUBLE,
        Type("float") to TypeName.FLOAT,
        Type("int32") to TypeName.INT,
        Type("int64") to TypeName.LONG,
        Type("string") to ClassName.get(String::class.java)
    )

    private var packageName: String = ""

    fun visitCompilationUnits(compilationUnits: List<CompilationUnit>): Map<Type, TypeName> {
        compilationUnits.forEach { visitCompilationUnit(it) }
        return types;
    }

    override fun visitCompilationUnit(cu: CompilationUnit) {
        packageName = cu.packageName
        super.visitCompilationUnit(cu)
    }

    override fun visitDeclaration(declaration: Declaration) {
        types[Type(declaration.name)] = ClassName.get(packageName, declaration.name)
    }
}
