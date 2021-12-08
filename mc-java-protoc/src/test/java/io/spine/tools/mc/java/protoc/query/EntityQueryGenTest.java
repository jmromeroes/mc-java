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

package io.spine.tools.mc.java.protoc.query;

import com.google.common.testing.NullPointerTester;
import io.spine.option.OptionsProto;
import io.spine.tools.mc.java.codegen.CodegenOptions;
import io.spine.tools.mc.java.codegen.Entities;
import io.spine.tools.mc.java.protoc.NoOpGenerator;
import io.spine.tools.proto.code.ProtoOption;
import io.spine.tools.protoc.plugin.nested.Task;
import io.spine.tools.protoc.plugin.nested.TaskView;
import io.spine.type.EnumType;
import io.spine.type.MessageType;
import io.spine.type.Type;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.tools.mc.java.protoc.Generators.generate;

@DisplayName("`EntityQueryGen` should")
class EntityQueryGenTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testAllPublicStaticMethods(EntityQueryGen.class);
    }

    @Test
    @DisplayName("generate code for entities if requested types where appropriate")
    void generateWhenRequired() {
        var config = newOptions();

        var generator = EntityQueryGen.instance(config);
        assertThat(generator)
                .isInstanceOf(EntityQueryGen.class);
        Type<?, ?> type = new MessageType(TaskView.getDescriptor());
        var output = generate(generator, type);
        assertThat(output)
                .isNotEmpty();
    }

    @Test
    @DisplayName("ignore non-`Message` types")
    void enums() {
        var config = newOptions();

        var generator = EntityQueryGen.instance(config);
        assertThat(generator)
                .isInstanceOf(EntityQueryGen.class);
        Type<?, ?> type = EnumType.create(Task.Priority.getDescriptor());
        var output = generate(generator, type);

        assertThat(output)
                .isEmpty();
    }

    @Test
    @DisplayName("do nothing if turned off")
    void off() {
        var config = newOptions(false);

        var generator = EntityQueryGen.instance(config);
        assertThat(generator)
                .isInstanceOf(NoOpGenerator.class);
    }

    private static CodegenOptions newOptions() {
        return newOptions(true);
    }

    private static CodegenOptions newOptions(boolean generate) {
        var entities = Entities.newBuilder();
        entities.addOption(ProtoOption.newBuilder()
                .setName(OptionsProto.entity.getDescriptor().getName())
        );
        entities.setGenerateQueries(generate);
        return CodegenOptions.newBuilder()
                .setEntities(entities)
                .build();
    }
}
