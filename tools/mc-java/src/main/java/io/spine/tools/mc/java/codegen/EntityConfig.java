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

package io.spine.tools.mc.java.codegen;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import io.spine.tools.protoc.FilePattern;
import io.spine.tools.protoc.ForEntities;
import io.spine.tools.protoc.ProtoOption;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

import java.util.List;

import static java.util.stream.Collectors.toList;

public final class EntityConfig extends MessageGroupConfig<ForEntities> {

    private final SetProperty<String> options;
    private final Property<Boolean> generateQueries;

    EntityConfig(Project p) {
        super(p);
        options = p.getObjects().setProperty(String.class);
        generateQueries = p.getObjects().property(Boolean.class);
    }

    void convention(GeneratedMessage.GeneratedExtension<?, ?> option,
                    Class<? extends Message> markerInterface,
                    Class<?> fieldSuperclass) {
        convention(FilePattern.getDefaultInstance());
        convention(fieldSuperclass);
        options.convention(ImmutableSet.of(option.getDescriptor().getName()));
        interfaceNames().convention(ImmutableSet.of(markerInterface.getCanonicalName()));
        generateQueries.convention(true);
    }

    public SetProperty<String> getOptions() {
        return options;
    }

    public void generateQueries() {
        generateQueries.set(true);
    }

    public void skipQueries() {
        generateQueries.set(false);
    }

    @Override
    ForEntities toProto() {
        return ForEntities.newBuilder()
                .addAllAddInterface(interfaces())
                .addAllOption(options())
                .addAllPattern(patterns())
                .setGenerateQueries(generateQueries.get())
                .setGenerateFields(generateFields())
                .build();
    }

    private List<ProtoOption> options() {
        return options.get()
                      .stream()
                      .map(name -> ProtoOption.newBuilder()
                              .setName(name)
                              .build())
                      .collect(toList());
    }
}
