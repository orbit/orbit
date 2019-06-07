/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.dsl.kotlin

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
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import kotlinx.coroutines.Deferred

class KotlinCodeGenerator(
    private val knownTypes: Map<TypeReference, TypeName>
) : AstVisitor() {
    private val deferredClassName = Deferred::class.asClassName()

    private var packageName = ""
    private var generatedTypes = mutableListOf<KotlinCompiledType>()

    fun visitCompilationUnits(compilationUnits: List<CompilationUnit>): List<KotlinCompiledType> {
        compilationUnits.forEach { visitCompilationUnit(it) }
        return generatedTypes
    }

    override fun visitCompilationUnit(cu: CompilationUnit) {
        packageName = cu.packageName
        super.visitCompilationUnit(cu)
    }

    override fun visitEnumDeclaration(enum: EnumDeclaration) {
        val enumSpec = TypeSpec.enumBuilder(enum.name)

        enum.members.forEach { enumSpec.addEnumConstant(it.name) }

        generatedTypes.add(KotlinCompiledType(packageName, enumSpec.build()))
    }

    override fun visitDataDeclaration(data: DataDeclaration) {
        val classSpec = TypeSpec.classBuilder(data.name)
            .addModifiers(KModifier.DATA)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameters(data.fields.map {
                        ParameterSpec.builder(it.name.decapitalize(), typeName(it.type))
                            .build()
                    })
                    .build()
            )
            .addProperties(data.fields.map {
                val name = it.name.decapitalize()
                PropertySpec.builder(name, typeName(it.type))
                    .initializer(name)
                    .build()
            })

        generatedTypes.add(KotlinCompiledType(packageName, classSpec.build()))
    }

    override fun visitActorDeclaration(actor: ActorDeclaration) {
        val classSpec = TypeSpec.interfaceBuilder(actor.name)
            .addSuperinterface(orbitActorKeyInterface(actor.keyType))
            .addFunctions(actor.methods.map {
                FunSpec.builder(it.name)
                    .addModifiers(KModifier.ABSTRACT)
                    .returns(deferredClassName.parameterizedBy(typeName(it.returnType)))
                    .addParameters(it.params.map { p ->
                        ParameterSpec.builder(p.name, typeName(p.type)).build()
                    })
                    .build()
            })
            .build()

        generatedTypes.add(KotlinCompiledType(packageName, classSpec))
    }

    private fun typeName(type: TypeReference): TypeName =
        if (!type.isGeneric) {
            knownTypes.getValue(TypeReference(type.name))
        } else {
            (knownTypes.getValue(TypeReference(type.name)) as ClassName)
                .parameterizedBy(
                    *type.of
                        .map(::typeName)
                        .toTypedArray()
                )
        }

    private fun orbitActorKeyInterface(keyType: TypeReference): TypeName =
        when (keyType.name) {
            PrimitiveType.GUID -> ActorWithGuidKey::class
            PrimitiveType.INT32 -> ActorWithInt32Key::class
            PrimitiveType.INT64 -> ActorWithInt64Key::class
            PrimitiveType.STRING -> ActorWithStringKey::class
            PrimitiveType.VOID -> ActorWithNoKey::class
            else -> throw IllegalStateException("Illegal actor key type '${keyType.name}'")
        }.asClassName()
}
