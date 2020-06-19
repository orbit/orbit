/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

// Shared dependencies
val grpcVersion by extra("1.29.0")
val grpcKotlinVersion by extra("0.1.4")
val protobufVersion by extra("3.11.1")
val kotlinCoroutinesVersion by extra("1.3.5")
val slf4jVersion by extra("1.7.30")
val jacksonVersion by extra("2.10.2")
val kotestVersion by extra ("3.4.2")
val mokitoVersion by extra("3.3.3")
val mockitoKotlin2Version by extra("2.2.0")
val micrometerVersion by extra("1.3.5")

// Publishing info
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
    val kotlinVersion = "1.3.72"

    base
    kotlin("jvm") version kotlinVersion apply false
    id("org.jetbrains.dokka") version "0.10.0" apply false
}

allprojects {
    group = orbitGroup
    version = orbitVersion
    description = orbitDescription

    repositories {
        mavenCentral()
        jcenter()
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
            "testImplementation"("io.kotlintest:kotlintest-runner-junit5:$kotestVersion")
            "testImplementation"("org.mockito:mockito-core:$mokitoVersion")
            "testImplementation"("com.nhaarman.mockitokotlin2:mockito-kotlin:$mockitoKotlin2Version")
        }

        tasks.withType<KotlinJvmCompile> {
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs += listOf("-Xopt-in=kotlin.RequiresOptIn")
            }
        }

        tasks.withType<Test> {
            testLogging {
                events = setOf(TestLogEvent.FAILED)
                showCauses = true
                showExceptions = true
                exceptionFormat = TestExceptionFormat.FULL
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
            archiveClassifier.set("javadoc")
            from(tasks.withType<DokkaTask>())
        }

        val sourcesJar by tasks.creating(Jar::class) {
            dependsOn(JavaPlugin.CLASSES_TASK_NAME)
            archiveClassifier.set("sources")

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
                    val remotePublish = project.properties["remotePublish"]?.toString() == "true"
                    var publicationUrl = "$buildDir/repository"

                    if (remotePublish) {
                        publicationUrl = project.properties["publish.url"]?.toString() ?: publicationUrl

                        val publicationUsername = project.properties["publish.username"]?.toString()
                        val publicationPassword = project.properties["publish.password"]?.toString()

                        credentials {
                            username = publicationUsername
                            password = publicationPassword
                        }
                    }
                    url = uri(publicationUrl)

                }
            }

            extensions.configure<SigningExtension> {
                val inMemoryKey = project.properties["inMemoryKey"]?.toString() == "true"

                if (inMemoryKey) {
                    val signingKey = findProperty("signingKey")?.toString()
                    val signingPassword = findProperty("signingPassword")?.toString()
                    useInMemoryPgpKeys(signingKey, signingPassword)
                }
                sign(publications["default"])
            }
        }
    }
}

