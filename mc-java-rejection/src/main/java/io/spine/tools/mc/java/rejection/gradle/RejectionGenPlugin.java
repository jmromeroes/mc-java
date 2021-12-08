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

import com.google.common.collect.ImmutableList;
import io.spine.tools.gradle.SourceSetName;
import io.spine.tools.gradle.task.GradleTask;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.spine.tools.gradle.project.Projects.getSourceSetNames;
import static io.spine.tools.gradle.task.JavaTaskName.compileJava;
import static io.spine.tools.mc.java.gradle.McJavaTaskName.generateRejections;
import static io.spine.tools.mc.java.gradle.McJavaTaskName.mergeDescriptorSet;

/**
 * Plugin which generates Rejections declared in {@code rejections.proto} files.
 *
 * <p>Uses generated proto descriptors.
 *
 * <p>Logs a warning if there are no protobuf descriptors generated.
 */
public final class RejectionGenPlugin implements Plugin<Project> {

    /**
     * Applies the plug-in to a project.
     *
     * <p>Adds {@code :generateRejections} tasks for all source sets of the project.
     *
     * <p>Tasks depend on corresponding {@code :generateProto} tasks and are executed
     * before corresponding {@code :compileJava} tasks.
     */
    @Override
    public void apply(Project project) {
        var helper = new Helper(project);
        helper.configure();
        project.getLogger().info(
                "Rejection generation plugin initialized with tasks: `{}`.",
                helper.tasks
        );
    }

    /**
     * Creates tasks and applies them to the project.
     */
    private static final class Helper {

        private final Project project;
        private final ProtoModule module;

        /** Configured tasks are {@code null} until {@link #configure()} is called. */
        private @MonotonicNonNull ImmutableList<GradleTask> tasks;

        private Helper(Project project) {
            this.project = project;
            this.module = new ProtoModule(project);
        }

        private void configure() {
            this.tasks = getSourceSetNames(project).stream()
                    .map(this::createTask)
                    .collect(toImmutableList());
        }

        private GradleTask createTask(SourceSetName ssn) {
            var action = RejectionGenAction.create(project, ssn);
            return createTask(action, ssn);
        }

        private GradleTask createTask(Action<Task> action, SourceSetName ssn) {
            var rejections = generateRejections(ssn);
            var mergeTask = mergeDescriptorSet(ssn);
            var compileTask = compileJava(ssn);
            var inputFiles = module.protoSource(ssn);
            var outputFiles = module.generatedRejections(ssn);
            var task = GradleTask.newBuilder(rejections, action)
                    .insertBeforeTask(compileTask)
                    .insertAfterTask(mergeTask)
                    .withInputFiles(inputFiles)
                    .withOutputFiles(outputFiles)
                    .applyNowTo(project);
            return task;
        }
    }
}
