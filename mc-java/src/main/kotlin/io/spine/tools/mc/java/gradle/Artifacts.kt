/*
 * Copyright 2022, TeamDev. All rights reserved.
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

@file:JvmName("Artifacts")

package io.spine.tools.mc.java.gradle

import io.spine.tools.gradle.Artifact
import io.spine.tools.gradle.Artifact.SPINE_TOOLS_GROUP
import io.spine.tools.gradle.Dependency
import io.spine.tools.gradle.DependencyVersions
import io.spine.tools.gradle.ThirdPartyDependency

/**
 * This file defines utilities for generating instances of [Artifact].
 */

private const val JAR_EXTENSION = "jar"
private const val GRPC_GROUP = "io.grpc"
private const val GRPC_PLUGIN_NAME = "protoc-gen-grpc-java"
private const val MC_JAVA_NAME = "spine-mc-java"
private const val EXECUTABLE_CLASSIFIER = "exe"

/**
 * The name of the Maven artifact of the Spine Protobuf compiler plugin.
 */
internal const val SPINE_PROTOC_PLUGIN_NAME = "spine-mc-java-protoc"

private val versions = DependencyVersions.loadFor(MC_JAVA_NAME)

/**
 * The Maven artifact of the gRPC Protobuf compiler plugin.
 */
@get:JvmName("gRpcProtocPlugin")
internal val gRpcProtocPlugin: Artifact by lazy {
    val gRpcPlugin: Dependency = ThirdPartyDependency(GRPC_GROUP, GRPC_PLUGIN_NAME)
    gRpcPlugin.withVersionFrom(versions)
}

/**
 * The Maven artifact containing the `mc-java-protoc` module.
 */
@get:JvmName("spineProtocPlugin")
internal val spineProtocPlugin: Artifact by lazy {
    Artifact.newBuilder()
        .useSpineToolsGroup()
        .setName(SPINE_PROTOC_PLUGIN_NAME)
        .setVersion(mcJavaVersion)
        .setClassifier(EXECUTABLE_CLASSIFIER)
        .setExtension(JAR_EXTENSION)
        .build()
}

private const val VALIDATION_GROUP = "io.spine.validation"

private val validationJavaDependency =
    ThirdPartyDependency(VALIDATION_GROUP, "spine-validation-java")

private val validationVersion: String by lazy {
    versions.versionOf(validationJavaDependency).orElseThrow()
}

@get:JvmName("validationJava")
internal val validationJava: Artifact by lazy {
    Artifact.newBuilder()
        .setName(validationJavaDependency.name())
        .setGroup(validationJavaDependency.groupId())
        .setVersion(validationVersion)
        .build()
}

@get:JvmName("validationRuntime")
internal val validationRuntime: Artifact by lazy {
    Artifact.newBuilder()
        .setName("spine-validation-runtime")
        .setGroup(VALIDATION_GROUP)
        .setVersion(validationVersion)
        .build()
}

/**
 * The version of the Model Compiler Java modules.
 *
 * This is the version of all the modules declared in this project.
 */
@get:JvmName("mcJavaVersion")
internal val mcJavaVersion: String by lazy {
    val self: Dependency = ThirdPartyDependency(SPINE_TOOLS_GROUP, MC_JAVA_NAME)
    versions.versionOf(self)
        .orElseThrow { IllegalStateException("Unable to load versions of ${self}.") }
}
