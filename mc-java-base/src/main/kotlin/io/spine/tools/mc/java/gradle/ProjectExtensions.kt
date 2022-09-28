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

@file:JvmName("Projects")

package io.spine.tools.mc.java.gradle

import com.google.protobuf.gradle.ProtobufConvention
import io.spine.tools.code.SourceSetName
import io.spine.tools.fs.DirectoryName
import io.spine.tools.fs.DirectoryName.grpc
import io.spine.tools.fs.DirectoryName.java
import io.spine.tools.fs.DirectoryName.spine
import io.spine.tools.gradle.ProtobufDependencies
import io.spine.tools.gradle.project.sourceSet
import io.spine.tools.java.fs.DefaultJavaPaths
import io.spine.tools.mc.gradle.modelCompiler
import java.nio.file.Path
import kotlin.io.path.Path
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.the

/**
 * Obtains options of Model Compiler for Java.
 */
public val Project.mcJava: McJavaOptions
    get() = (modelCompiler as ExtensionAware).extensions.getByType()

private val Project.defaultPaths: DefaultJavaPaths
    get() = DefaultJavaPaths.at(projectDir.toPath())

/**
 * Obtains the directory containing proto source code of the specified source set.
 */
public fun Project.protoDir(ss: SourceSetName): Path {
    val sourceSetDir = defaultPaths.src().path().resolve(ss.value)
    return sourceSetDir.resolve(ProtobufDependencies.sourceSetExtensionName)
}

/**
 * Obtains collection of proto files (if any) in the source set with the given name.
 */
public fun Project.protoFiles(ssn: SourceSetName): FileCollection? {
    val sourceSet = sourceSet(ssn)
    val extension = sourceSet.extensions.findByName(ProtobufDependencies.sourceSetExtensionName)
    return extension as FileCollection?
}

/**
 * Obtains the path to the generated code directory of this project.
 */
public val Project.generatedDir: Path
    get() = Path(the<ProtobufConvention>().protobuf.generatedFilesBaseDir)

/**
 * The short name of the directory containing generated Java source code.
 */
public val generatedJavaDirName: DirectoryName = java

/**
 * The short name of the directory containing generated gRPC source code.
 */
public val generatedGrpcDirName: DirectoryName = grpc

/**
 * The short name of the directory containing generated rejections source code.
 */
public val generatedRejectionsDirName: DirectoryName = spine

/**
 * Obtains the directory containing generated Java source code for the specified source set.
 */
public fun Project.generatedJavaDir(ss: SourceSetName): Path =
    generated(ss).resolve(generatedJavaDirName)

/**
 * Obtains the directory with the generated gRPC code for the specified source set.
 */
public fun Project.generatedGrpcDir(ss: SourceSetName): Path =
    generated(ss).resolve(generatedGrpcDirName)

/**
 * Obtains the directory with the rejections source code generated for the specified source set.
 */
public fun Project.generatedRejectionsDir(ss: SourceSetName): Path =
    generated(ss).resolve(generatedRejectionsDirName)

private fun Project.generated(ss: SourceSetName): Path {
    return generatedDir.resolve(ss.value)
}

private fun Path.resolve(dir: DirectoryName) = this.resolve(dir.value())
