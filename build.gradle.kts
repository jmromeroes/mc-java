/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

@file:Suppress("RemoveRedundantQualifierName") // To prevent IDEA replacing FQN imports.

import com.google.common.io.Files.createParentDirs
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import io.spine.internal.dependency.CheckerFramework
import io.spine.internal.dependency.ErrorProne
import io.spine.internal.dependency.FindBugs
import io.spine.internal.dependency.Grpc
import io.spine.internal.dependency.Guava
import io.spine.internal.dependency.JUnit
import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.Spine
import io.spine.internal.dependency.Truth
import io.spine.internal.gradle.IncrementGuard
import io.spine.internal.gradle.RunBuild
import io.spine.internal.gradle.VersionWriter
import io.spine.internal.gradle.applyStandard
import io.spine.internal.gradle.checkstyle.CheckStyleConfig
import io.spine.internal.gradle.excludeProtobufLite
import io.spine.internal.gradle.forceVersions
import io.spine.internal.gradle.javac.configureErrorProne
import io.spine.internal.gradle.javac.configureJavac
import io.spine.internal.gradle.javadoc.JavadocConfig
import io.spine.internal.gradle.kotlin.applyJvmToolchain
import io.spine.internal.gradle.kotlin.setFreeCompilerArgs
import io.spine.internal.gradle.publish.Publish.Companion.publishProtoArtifact
import io.spine.internal.gradle.publish.PublishExtension
import io.spine.internal.gradle.publish.PublishingRepos
import io.spine.internal.gradle.publish.PublishingRepos.gitHub
import io.spine.internal.gradle.publish.spinePublishing
import io.spine.internal.gradle.report.coverage.JacocoConfig
import io.spine.internal.gradle.report.license.LicenseReporter
import io.spine.internal.gradle.report.pom.PomGenerator
import io.spine.internal.gradle.testing.configureLogging
import io.spine.internal.gradle.testing.registerTestTasks
import io.spine.internal.gradle.testing.exposeTestArtifacts
import java.time.Duration
import java.util.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    idea
    id(io.spine.internal.dependency.Protobuf.GradlePlugin.id)
    id(io.spine.internal.dependency.ErrorProne.GradlePlugin.id)
    kotlin("jvm")
    with(io.spine.internal.dependency.Spine.ProtoData) {
        id(pluginId) version version
    }
}

spinePublishing {
    projectsToPublish.addAll(subprojects.map { it.path })
    targetRepositories.addAll(
        PublishingRepos.cloudRepo,
        PublishingRepos.cloudArtifactRegistry,
        gitHub("mc-java")
    )
    spinePrefix.set(true)
}

allprojects {
    apply {
        plugin("jacoco")
        plugin("idea")
        plugin("project-report")
        apply(from = "$rootDir/version.gradle.kts")
    }

    group = "io.spine.tools"
    version = extra["versionToPublish"]!!

    repositories {
        gitHub("base")
        gitHub("tool-base")
        gitHub("model-compiler")
        applyStandard()
    }
}

subprojects {
    apply {
        plugin("java-library")
        plugin("kotlin")
        plugin("net.ltgt.errorprone")
        plugin("pmd-settings")
        plugin(Protobuf.GradlePlugin.id)
        plugin("io.spine.proto-data")
    }

    val validation = Spine(project).validation

    dependencies {
        errorprone(ErrorProne.core)

        protoData(validation.java)

        compileOnlyApi(FindBugs.annotations)
        compileOnlyApi(CheckerFramework.annotations)
        ErrorProne.annotations.forEach { compileOnlyApi(it) }

        implementation(Guava.lib)

        testImplementation(Guava.testLib)
        JUnit.api.forEach { testImplementation(it) }
        Truth.libs.forEach { testImplementation(it) }
        testRuntimeOnly(JUnit.runner)

        testImplementation(validation.runtime)
    }

    val baseVersion: String by extra
    val coreVersion: String by extra
    val toolBaseVersion: String by extra
    with(configurations) {
        forceVersions()
        excludeProtobufLite()
        all {
            resolutionStrategy {
                force(
                    "io.spine:spine-base:$baseVersion",
                    "io.spine:spine-server:$coreVersion",
                    "io.spine.tools:spine-testlib:$baseVersion",
                    "io.spine.tools:spine-tool-base:$toolBaseVersion",
                    "io.spine.tools:spine-plugin-base:$toolBaseVersion"
                )
            }
        }
    }

    java {
        exposeTestArtifacts()
    }

    tasks.withType<JavaCompile> {
        configureJavac()
        configureErrorProne()
    }

    JavadocConfig.applyTo(project)
    CheckStyleConfig.applyTo(project)

    val javaVersion = JavaVersion.VERSION_11.toString()
    kotlin {
        applyJvmToolchain(javaVersion)
        explicitApi()
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = javaVersion
        setFreeCompilerArgs()
    }

    tasks {
        registerTestTasks()
        test {
            useJUnitPlatform()
            configureLogging()
        }
    }

    val generatedDir = "$projectDir/generated"
    val generatedResources = "$generatedDir/main/resources"

    tasks.create<DefaultTask>(name = "prepareProtocConfigVersions") {
        description = "Prepares the versions.properties file."

        val propertiesFile = file("$generatedResources/versions.properties")
        outputs.file(propertiesFile)

        val versions = Properties()
        with(versions) {
            setProperty("baseVersion", baseVersion)
            setProperty("protobufVersion", Protobuf.version)
            setProperty("gRPCVersion", Grpc.version)
        }

        @Suppress("UNCHECKED_CAST")
        inputs.properties(HashMap(versions) as MutableMap<String, *>)

        doLast {
            createParentDirs(propertiesFile)
            propertiesFile.createNewFile()
            propertiesFile.outputStream().use {
                versions.store(it,
                    "Versions of dependencies of the Spine Model Compiler for Java plugin and" +
                            " the Spine Protoc plugin.")
            }
        }

        tasks.processResources {
            dependsOn(this@create)
        }
    }

    sourceSets.main {
        resources.srcDir(generatedResources)
    }

    apply<IncrementGuard>()
    apply<VersionWriter>()
    publishProtoArtifact(project)
    LicenseReporter.generateReportIn(project)

    protobuf { protoc { artifact = Protobuf.compiler } }

    protoData {
        renderers(
            "io.spine.validation.java.PrintValidationInsertionPoints",
            "io.spine.validation.java.JavaValidationRenderer",

            // Suppress warnings in the generated code.
            "io.spine.protodata.codegen.java.file.PrintBeforePrimaryDeclaration",
            "io.spine.protodata.codegen.java.suppress.SuppressRenderer"
        )
        plugins(
            "io.spine.validation.ValidationPlugin"
        )
        options(
            "spine/options.proto",
            "spine/time_options.proto"
        )
    }
}

JacocoConfig.applyTo(project)
PomGenerator.applyTo(project)
LicenseReporter.mergeAllReports(project)

/**
 * Collect `publishToMavenLocal` tasks for all subprojects that are specified for
 * publishing in the root project.
 */
val projectsToPublish: Set<String> = the<PublishExtension>().projectsToPublish.get()

val testAll by tasks.registering {
    val testTasks = projectsToPublish.map { p ->
        val subProject = project(p)
        subProject.tasks["test"]
    }
    dependsOn(testTasks)
}

val localPublish by tasks.registering {
    val pubTasks = projectsToPublish.map { p ->
        val subProject = project(p)
        subProject.tasks["publishToMavenLocal"]
    }
    dependsOn(pubTasks)
}

/**
 * The build task executed under `tests` subdirectory.
 *
 * These tests depend on locally published artifacts.
 * It is similar to the dependency on such artifacts that `:mc-java` module declares for
 * its tests. So, we depend on the `test` task of this module for simplicity.
 */
val integrationTests by tasks.registering(RunBuild::class) {
    directory = "$rootDir/tests"

    /** A timeout for the case of stalled child processes under Windows. */
    timeout.set(Duration.ofMinutes(20))
    dependsOn(testAll)
    dependsOn(localPublish)
}

tasks.register("buildAll") {
    dependsOn(tasks.build, integrationTests)
}

val check: Task by tasks.getting {
    dependsOn(integrationTests)
}
