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

package io.spine.tools.mc.java.gradle.plugins;

import io.spine.protodata.gradle.CodegenSettings;
import io.spine.protodata.gradle.plugin.LaunchProtoData;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import static io.spine.tools.mc.java.gradle.Artifacts.validationJavaBundle;
import static io.spine.tools.mc.java.gradle.Projects.getMcJava;
import static java.io.File.separatorChar;
import static java.lang.String.format;

/**
 * The plugin that configures ProtoData for the associated project.
 *
 * <p>We use ProtoData and the Validation library to generate validation code right inside
 * the Protobuf message classes. This plugin applies the {@code io.spine.protodata} plugin,
 * configures its extension, writes the ProtoData configuration file, and adds the required
 * dependencies to the target project.
 */
final class ProtoDataConfigPlugin implements Plugin<Project> {

    private static final String PROTO_DATA_ID = "io.spine.protodata";
    private static final String CONFIG_SUBDIR = "protodata-config";

    private static final String PROTODATA_CONFIGURATION = "protoData";

    /**
     * Applies the {@code io.spine.protodata} plugin to the project and, if the user needs
     * validation code generation, configures ProtoData to generate Java validation code.
     *
     * <p>ProtoData configuration is a tricky operation because of Gradle's lifecycle. On one hand,
     * to check if the user disables validation via
     * {@link io.spine.tools.mc.java.gradle.codegen.ValidationConfig#skipValidation()},
     * we need to run configuration after the project is evaluated. At the same time, we need to
     * squeeze our configuration before the {@code LaunchProtoData} task is configured. This means
     * adding the {@code afterEvaluate(..)} hook before the ProtoData Gradle plugin is applied to
     * the project.
     */
    @Override
    public void apply(Project target) {
        target.afterEvaluate(ProtoDataConfigPlugin::configureProtoData);
        target.getPluginManager()
              .apply(PROTO_DATA_ID);
    }

    private static void configureProtoData(Project target) {
        configureValidation(target);

        var tasks = target.getTasks();
        tasks.withType(LaunchProtoData.class, task -> {
            var name = task.getName();
            var taskName = format("writeConfigFor_%s", name);
            var configTask = tasks.create(
                    taskName,
                    GenerateProtoDataConfig.class,
                    t -> linkConfigFile(target, task, t)
            );
            task.dependsOn(configTask);
        });
    }

    /**
     * Configures ProtoData with the required Validation library extensions,
     * for the passed Gradle project.
     *
     * <p>In case the Validation
     * {@linkplain io.spine.tools.mc.java.gradle.codegen.ValidationConfig#shouldSkipValidation()
     * is disabled}, does nothing.
     */
    private static void configureValidation(Project target) {
        var options = getMcJava(target).codegen.validation();
        if (options.shouldSkipValidation()) {
            return;
        }
        var ext = target.getExtensions()
                        .getByType(CodegenSettings.class);
        ext.renderers(
                "io.spine.validation.java.PrintValidationInsertionPoints",
                "io.spine.validation.java.JavaValidationRenderer"
        );
        ext.plugins(
                "io.spine.validation.ValidationPlugin"
        );

        var dependencies = target.getDependencies();
        dependencies.add(PROTODATA_CONFIGURATION, validationJavaBundle().notation());
    }

    private static void linkConfigFile(Project target, LaunchProtoData task,
                                       GenerateProtoDataConfig t) {
        var targetFile = t.getTargetFile();
        var fileName = t.getName() + ".bin";
        var defaultFile = target.getLayout()
                                .getBuildDirectory()
                                .file(CONFIG_SUBDIR + separatorChar + fileName);
        targetFile.convention(defaultFile);
        task.getConfiguration()
            .set(targetFile);
    }
}
