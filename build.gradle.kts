/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

val grpcVersion by extra("1.23.0")
val grpcKotlinVersion by extra("0.1.1")
val protobufVersion by extra("3.9.1")
val kotlinCoroutinesVersion by extra("1.3.1")
val slf4jVersion by extra("1.7.26")
val jetcdVersion by extra("0.3.1-SNAPSHOT")
val guavaVersion by extra("28.1")

val orbitGroup by extra("cloud.orbit")
val orbitVersion by extra(project.properties["orbit.version"]!!)
val orbitUrl by extra("https://www.orbit.cloud")
val orbitScmUrl by extra("https://github.com/orbit/orbit.git")
val orbitDescription by extra("Orbit is a system to make building highly scalable realtime services easier.")
val orgName by extra("Electronic Arts")
val orgUrl by extra("https://www.ea.com")
val orgEmail by extra("orbit@ea.com")
val licenseName by extra("The BSD 3-Clause License")
val licenseUrl by extra("http://opensource.org/licenses/BSD-3-Clause")

plugins {
    val kotlinVersion = "1.3.50"

    base
    kotlin("multiplatform") version kotlinVersion apply false
    id("org.jetbrains.dokka") version "0.10.0" apply false
}

allprojects {
    group = orbitGroup
    version = orbitVersion
    description = orbitDescription

    repositories {
        mavenCentral()
        jcenter()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

subprojects {
    plugins.withType<KotlinPluginWrapper> {
        dependencies {
            "implementation"(kotlin("stdlib-jdk8"))

            "implementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
            "implementation"("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinCoroutinesVersion")

            "testRuntimeOnly"("org.slf4j:slf4j-simple:$slf4jVersion")
            "testImplementation"(kotlin("test-junit"))
        }

        tasks.withType<KotlinJvmCompile> {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    plugins.withType<MavenPublishPlugin> {
        plugins.apply("org.jetbrains.dokka")

        tasks.creating(DokkaTask::class) {
            outputFormat = "html"
            outputDirectory = "$buildDir/javadoc"
        }

        val dokkaJar by tasks.creating(Jar::class) {
            group = JavaBasePlugin.DOCUMENTATION_GROUP
            description = "Assembles Kotlin docs with Dokka"
            classifier = "javadoc"
            from(tasks.withType<DokkaTask>())
        }

        val sourcesJar by tasks.creating(Jar::class) {
            dependsOn(JavaPlugin.CLASSES_TASK_NAME)
            classifier = "sources"

            val sourceSets: SourceSetContainer by project
            from(sourceSets["main"].allSource)
        }

        extensions.configure<PublishingExtension> {
            plugins.apply("signing")

            publications {
                create<MavenPublication>("default") {
                    from(components["java"])
                    artifact(dokkaJar)
                    artifact(sourcesJar)

                    pom {
                        name.set(project.name)
                        description.set(project.description)
                        url.set(orbitUrl)

                        licenses {
                            license {
                                name.set(licenseName)
                                url.set(licenseUrl)
                            }
                        }

                        scm {
                            url.set(orbitUrl)
                            connection.set(orbitScmUrl)
                            developerConnection.set(orbitScmUrl)
                        }

                        organization {
                            name.set(orgName)
                            url.set(orgUrl)
                        }

                        developers {
                            developer {
                                name.set(orgName)
                                email.set(orgEmail)
                            }
                        }
                    }
                }
            }

            repositories {
                maven {
                    url = uri("$buildDir/repository")
                }
            }

            extensions.configure<SigningExtension> {
                sign(publications["default"])
            }
        }
    }
}

