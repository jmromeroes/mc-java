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

package io.spine.tools.mc.java.field;

import com.google.common.collect.ImmutableSet;
import io.spine.annotation.Internal;
import io.spine.tools.java.code.field.FieldName;

import java.util.Collection;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

/**
 * Property accessor methods generated by the Protobuf compiler for a field.
 *
 * <p>Each Protobuf field results in a number of accessor methods. The count and naming of
 * the methods depends on the field type.
 */
@Internal
public final class Accessors {

    private final FieldName propertyName;
    private final FieldType type;

    private Accessors(FieldName propertyName, FieldType type) {
        this.propertyName = propertyName;
        this.type = type;
    }

    /**
     * Creates an instance of {@code GeneratedAccessors} for the given field.
     *
     * @param name
     *         the name of the field associated with the accessors
     * @param type
     *         the type of the field associated with the accessors
     * @return new instance
     */
    public static Accessors forField(io.spine.code.proto.FieldName name, FieldType type) {
        var javaFieldName = FieldName.from(name);
        return new Accessors(javaFieldName, type);
    }

    /**
     * Obtains all the names of the accessor methods.
     *
     * <p>The accessor methods may have different parameters. Some of the obtained names may
     * reference several method overloads.
     */
    public ImmutableSet<String> names() {
        var names = names(type.accessors());
        return names;
    }

    private ImmutableSet<String> names(Collection<Accessor> accessors) {
        return accessors.stream()
                        .map(accessor -> accessor.format(propertyName))
                        .collect(toImmutableSet());
    }
}
