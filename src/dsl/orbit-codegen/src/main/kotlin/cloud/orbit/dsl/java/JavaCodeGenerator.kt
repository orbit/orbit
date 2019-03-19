/*
 Copyright (C) 2015 - 2018 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <http://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.java

import cloud.orbit.dsl.ast.*
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

internal class JavaCodeGenerator(private val knownTypes: Map<Type, TypeName>) : AstVisitor() {
    private val orbitActorInterface = ClassName.get("cloud.orbit.actors", "Actor")
    private val orbitTaskClass = ClassName.get("cloud.orbit.concurrent", "Task")

    private var packageName = ""
    private var generatedTypes = mutableListOf<CompiledType>()

    fun visitCompilationUnits(compilationUnits: List<CompilationUnit>): List<CompiledType> {
        compilationUnits.forEach { visitCompilationUnit(it) }
        return generatedTypes
    }

    override fun visitCompilationUnit(cu: CompilationUnit) {
        packageName = cu.packageName
        super.visitCompilationUnit(cu)
    }

    override fun visitEnumDeclaration(enum: EnumDeclaration) {
        val enumSpec = TypeSpec.enumBuilder(enum.name)
            .addModifiers(Modifier.PUBLIC)

        enum.members.forEach { enumSpec.addEnumConstant(it.name) }

        generatedTypes.add(CompiledType(packageName, enumSpec.build()))
    }

    override fun visitDataDeclaration(data: DataDeclaration) {
        val classSpec = TypeSpec.classBuilder(data.name)
            .addModifiers(Modifier.PUBLIC)

        val ctor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)

        data.fields.forEach {
            val fieldType = knownTypes[it.type]
            val varName = fieldToVariableName(it.name)

            // Variable backing the data field
            classSpec.addField(fieldType, varName, Modifier.PRIVATE, Modifier.FINAL)

            // Constructor parameter and assignment for the field
            ctor.addParameter(fieldType, varName)
                .addStatement("this.\$L = \$L", varName, varName)

            // Getter for this field
            classSpec.addMethod(
                MethodSpec
                    .methodBuilder("get${it.name}")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(fieldType)
                    .addStatement("return \$L", varName)
                    .build()
            )
        }

        classSpec.addMethod(ctor.build())

        generatedTypes.add(CompiledType(packageName, classSpec.build()))
    }

    override fun visitGrainDeclaration(grain: GrainDeclaration) {
        val classSpec = TypeSpec
            .interfaceBuilder(grain.name)
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(orbitActorInterface)
            .addMethods(grain.methods.map {
                MethodSpec.methodBuilder(it.name)
                    .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                    .returns(ParameterizedTypeName.get(orbitTaskClass, knownTypes[it.returnType]!!.box()))
                    .addParameters(it.params
                        .asSequence()
                        .map { p -> ParameterSpec.builder(knownTypes[p.type], p.name).build() }
                        .toList())
                    .build()
            })
            .build()

        generatedTypes.add(CompiledType(packageName, classSpec))
    }

    private fun fieldToVariableName(fieldName: String) = fieldName.decapitalize()
}
