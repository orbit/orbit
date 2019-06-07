/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.java

import cloud.orbit.core.actor.ActorWithGuidKey
import cloud.orbit.core.actor.ActorWithInt32Key
import cloud.orbit.core.actor.ActorWithInt64Key
import cloud.orbit.core.actor.ActorWithNoKey
import cloud.orbit.core.actor.ActorWithStringKey
import cloud.orbit.dsl.ast.ActorDeclaration
import cloud.orbit.dsl.ast.AstVisitor
import cloud.orbit.dsl.ast.CompilationUnit
import cloud.orbit.dsl.ast.DataDeclaration
import cloud.orbit.dsl.ast.EnumDeclaration
import cloud.orbit.dsl.ast.TypeReference
import cloud.orbit.dsl.type.PrimitiveType
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import java.util.concurrent.CompletableFuture
import javax.lang.model.element.Modifier

internal class JavaCodeGenerator(private val knownTypes: Map<TypeReference, TypeName>) : AstVisitor() {
    private val completableFutureClassName = ClassName.get(CompletableFuture::class.java)

    private var packageName = ""
    private var generatedTypes = mutableListOf<JavaCompiledType>()

    fun visitCompilationUnits(compilationUnits: List<CompilationUnit>): List<JavaCompiledType> {
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

        generatedTypes.add(JavaCompiledType(packageName, enumSpec.build()))
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

        generatedTypes.add(JavaCompiledType(packageName, classSpec.build()))
    }

    override fun visitActorDeclaration(actor: ActorDeclaration) {
        val classSpec = TypeSpec
            .interfaceBuilder(actor.name)
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(orbitActorKeyInterface(actor.keyType))
            .addMethods(actor.methods.map {
                MethodSpec.methodBuilder(it.name)
                    .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                    .returns(ParameterizedTypeName.get(completableFutureClassName, typeName(it.returnType).box()))
                    .addParameters(it.params
                        .asSequence()
                        .map { p -> ParameterSpec.builder(typeName(p.type), p.name).build() }
                        .toList())
                    .build()
            })
            .build()

        generatedTypes.add(JavaCompiledType(packageName, classSpec))
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

    private fun orbitActorKeyInterface(keyType: TypeReference): TypeName =
        when (keyType.name) {
            PrimitiveType.GUID -> ActorWithGuidKey::class.java
            PrimitiveType.INT32 -> ActorWithInt32Key::class.java
            PrimitiveType.INT64 -> ActorWithInt64Key::class.java
            PrimitiveType.STRING -> ActorWithStringKey::class.java
            PrimitiveType.VOID -> ActorWithNoKey::class.java
            else -> throw IllegalStateException("Illegal actor key type '${keyType.name}'")
        }.let {
            ClassName.get(it)
        }
}
