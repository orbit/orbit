/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.java

import cloud.orbit.dsl.ast.ActorDeclaration
import cloud.orbit.dsl.ast.ActorKeyType
import cloud.orbit.dsl.ast.AstVisitor
import cloud.orbit.dsl.ast.CompilationUnit
import cloud.orbit.dsl.ast.DataDeclaration
import cloud.orbit.dsl.ast.EnumDeclaration
import cloud.orbit.dsl.ast.TypeReference
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

internal class JavaCodeGenerator(private val knownTypes: Map<TypeReference, TypeName>) : AstVisitor() {
    private val completableFutureClass =
        ClassName.get("java.util.concurrent", "CompletableFuture")

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
            val fieldType = typeName(it.type)
            val varName = fieldToVariableName(it.name)

            // Variable backing the data field
            classSpec.addField(fieldType, varName, Modifier.PRIVATE, Modifier.FINAL)

            // Constructor parameter and assignment for the field
            ctor.addParameter(fieldType, varName)
                .addStatement("this.\$L = \$L", varName, varName)

            // Getter for this field
            val getterName = "get${it.name.capitalize()}"
            classSpec.addMethod(
                MethodSpec
                    .methodBuilder(getterName)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(fieldType)
                    .addStatement("return \$L", varName)
                    .build()
            )
        }

        classSpec.addMethod(ctor.build())

        generatedTypes.add(CompiledType(packageName, classSpec.build()))
    }

    override fun visitActorDeclaration(actor: ActorDeclaration) {
        val classSpec = TypeSpec
            .interfaceBuilder(actor.name)
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(orbitActorKeyInterface(actor.keyType))
            .addMethods(actor.methods.map {
                MethodSpec.methodBuilder(it.name)
                    .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                    .returns(ParameterizedTypeName.get(completableFutureClass, typeName(it.returnType).box()))
                    .addParameters(it.params
                        .asSequence()
                        .map { p -> ParameterSpec.builder(typeName(p.type), p.name).build() }
                        .toList())
                    .build()
            })
            .build()

        generatedTypes.add(CompiledType(packageName, classSpec))
    }

    private fun fieldToVariableName(fieldName: String) = fieldName.decapitalize()

    private fun typeName(type: TypeReference): TypeName =
        if (!type.isGeneric) {
            knownTypes.getValue(TypeReference(type.name))
        } else {
            ParameterizedTypeName.get(
                knownTypes.getValue(TypeReference(type.name)) as ClassName,
                *type.of
                    .map(::typeName)
                    .map(TypeName::box)
                    .toTypedArray()
            )
        }

    private fun orbitActorKeyInterface(keyType: ActorKeyType): TypeName =
        when (keyType) {
            ActorKeyType.NO_KEY -> "ActorWithNoKey"
            ActorKeyType.STRING -> "ActorWithStringKey"
            ActorKeyType.INT32 -> "ActorWithInt32Key"
            ActorKeyType.INT64 -> "ActorWithInt64Key"
            ActorKeyType.GUID -> "ActorWithGuidKey"
        }.let {
            ClassName.get("cloud.orbit.core.actor", it)
        }
}
