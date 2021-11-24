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
package io.spine.tools.mc.java.rejection.gradle;

import io.spine.code.proto.FileSet;
import io.spine.tools.gradle.ProtoPlugin;
import io.spine.tools.gradle.SourceSetName;
import io.spine.tools.gradle.task.GradleTask;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.io.File;
import java.util.function.Supplier;

import static io.spine.tools.gradle.project.Projects.descriptorSetFile;
import static io.spine.tools.gradle.task.JavaTaskName.compileJava;
import static io.spine.tools.gradle.task.JavaTaskName.compileTestJava;
import static io.spine.tools.mc.java.gradle.McJavaTaskName.generateRejections;
import static io.spine.tools.mc.java.gradle.McJavaTaskName.generateTestRejections;
import static io.spine.tools.mc.java.gradle.McJavaTaskName.mergeDescriptorSet;
import static io.spine.tools.mc.java.gradle.McJavaTaskName.mergeTestDescriptorSet;
import static io.spine.tools.mc.java.gradle.Projects.generatedRejectionsDir;
import static io.spine.tools.mc.java.gradle.Projects.protoDir;

/**
 * Plugin which generates Rejections declared in {@code rejections.proto} files.
 *
 * <p>Uses generated proto descriptors.
 *
 * <p>Logs a warning if there are no protobuf descriptors generated.
 */
public class RejectionGenPlugin extends ProtoPlugin {

    /**
     * Applies the plug-in to a project.
     *
     * <p>Adds {@code :generateRejections} and {@code :generateTestRejections} tasks.
     *
     * <p>Tasks depend on corresponding {@code :generateProto} tasks and are executed
     * before corresponding {@code :compileJava} tasks.
     */
    @Override
    public void apply(Project project) {
        Action<Task> mainScopeAction =
                createAction(project,
                             mainProtoFiles(project),
                             () -> generatedRejectionsDir(project, SourceSetName.main).toString(),
                             () -> protoDir(project,  SourceSetName.main).toString());
        GradleTask mainTask =
                GradleTask.newBuilder(generateRejections, mainScopeAction)
                        .insertAfterTask(mergeDescriptorSet)
                        .insertBeforeTask(compileJava)
                        .applyNowTo(project);
        Action<Task> testScopeAction =
                createAction(project,
                             testProtoFiles(project),
                             () -> generatedRejectionsDir(project, SourceSetName.test).toString(),
                             () -> protoDir(project, SourceSetName.test).toString());

        GradleTask testTask =
                GradleTask.newBuilder(generateTestRejections, testScopeAction)
                        .insertAfterTask(mergeTestDescriptorSet)
                        .insertBeforeTask(compileTestJava)
                        .applyNowTo(project);

        project.getLogger().debug(
                "Rejection generation phase initialized with tasks: `{}`, `{}`.",
                mainTask, testTask
        );
    }

    private static Action<Task> createAction(Project project,
                                             Supplier<FileSet> files,
                                             Supplier<String> targetDirPath,
                                             Supplier<String> protoSrcDir) {
        return new RejectionGenAction(project, files, targetDirPath, protoSrcDir);
    }

    @Override
    protected Supplier<File> mainDescriptorFile(Project project) {
        return () -> descriptorSetFile(project, SourceSetName.main);
    }

    @Override
    protected Supplier<File> testDescriptorFile(Project project) {
        return () -> descriptorSetFile(project, SourceSetName.test);
    }
}
