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

package io.spine.tools.mc.java.gradle.plugins;

import io.spine.logging.Logging;
import io.spine.protodata.gradle.Extension;
import io.spine.protodata.gradle.LaunchProtoData;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import static io.spine.tools.mc.java.gradle.Artifacts.validationJava;
import static io.spine.tools.mc.java.gradle.Artifacts.validationRuntime;
import static java.io.File.separatorChar;
import static java.lang.String.format;

/**
 * The plugin that configures ProtoData for the associated project.
 *
 * <p>We use ProtoData and the Validation library to generate validation code right inside
 * the Protobuf message classes. This plugin applies the {@code io.spine.proto-data} plugin,
 * configures its extension, writes the ProtoData configuration file, and adds the required
 * dependencies to the target project.
 */
final class ProtoDataConfigPlugin implements Plugin<Project>, Logging {

    /**
     * The name of the JVM system property, which, if set to any value, <b>disables</b>
     * the Spine Validation step, configured by this plugin.
     *
     * <p>The property can be set to <b>any</b> value.
     *
     * @see #isValidationDisabled()
     */
    private static final String VALIDATION_SWITCH_NAME = "spine.internal.validation.disabled";

    private static final String PROTO_DATA_ID = "io.spine.proto-data";
    private static final String CONFIG_SUBDIR = "protodata-config";

    private final boolean validationDisabled = isValidationDisabled();
    private final boolean validationEnabled = !validationDisabled;

    private @MonotonicNonNull Project project;
    private @MonotonicNonNull Extension ext;

    @Override
    public void apply(Project target) {
        if (validationDisabled) {
            _warn().log("The Spine Validation has been disabled via the system property.");
        }
        applyProtoDataPluginTo(target);
        addValidationRenderers();
        addOptions();
        addValidationDependencies();
        addTasks();
    }

    private void applyProtoDataPluginTo(Project target) {
        this.project = target;
        target.getPluginManager()
              .apply(PROTO_DATA_ID);
        this.ext = target.getExtensions()
                         .getByType(Extension.class);
    }

    private void addValidationRenderers() {
        if (validationEnabled) {
            ext.renderers(
                    "io.spine.validation.java.PrintValidationInsertionPoints",
                    "io.spine.validation.java.JavaValidationRenderer"
            );
            ext.plugins(
                    "io.spine.validation.ValidationPlugin"
            );
        }
    }

    private void addOptions() {
        ext.options(
                "spine/options.proto",
                "spine/time_options.proto"
        );
    }

    private void addValidationDependencies() {
        if (validationEnabled) {
            var dependencies = project.getDependencies();
            dependencies.add("protoData", validationJava().notation());
            dependencies.add("implementation", validationRuntime().notation());
        }
    }

    private void addTasks() {
        var tasks = project.getTasks();
        tasks.withType(LaunchProtoData.class, task -> {
            var name = task.getName();
            var taskName = format("writeConfigFor_%s", name);
            var configTask = tasks.create(
                    taskName,
                    GenerateProtoDataConfig.class,
                    t -> linkConfigFile(project, task, t)
            );
            task.dependsOn(configTask);
        });
    }

    @SuppressWarnings("AccessOfSystemProperties") // Experimental shortcut.
    private static boolean isValidationDisabled() {
        var value = System.getProperty(VALIDATION_SWITCH_NAME);
        var disabled = value != null;
        return disabled;
    }

    private static
    void linkConfigFile(Project target, LaunchProtoData task, GenerateProtoDataConfig t) {
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
