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

package io.spine.test.tools.validate;

import com.google.common.truth.Correspondence;
import com.google.common.truth.IterableSubject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.test.tools.validate.command.CreateProject;
import io.spine.test.tools.validate.entity.Project;
import io.spine.test.tools.validate.entity.Task;
import io.spine.test.tools.validate.event.ProjectCreated;
import io.spine.test.tools.validate.rejection.TestRejections.CannotCreateProject;
import io.spine.validate.ConstraintViolation;
import io.spine.validate.ValidationError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Correspondence.transforming;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static io.spine.base.Identifier.newUuid;
import static io.spine.test.tools.validate.IsValid.assertInvalid;
import static io.spine.test.tools.validate.IsValid.assertValid;
import static io.spine.test.tools.validate.UltimateChoice.CHICKEN;
import static io.spine.test.tools.validate.UltimateChoice.FISH;
import static io.spine.test.tools.validate.UltimateChoice.VEGETABLE;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("`(required)` constraint should be compiled so that")
class RequiredConstraintTest {

    private static final Correspondence<ConstraintViolation, String> fieldName = transforming(
            violation -> violation.getFieldPath().getFieldName(0),
            "field name"
    );

    @Test
    @DisplayName("a number field can have any value")
    void ignoreNumbers() {
        Singulars.Builder singulars = Singulars.newBuilder()
                .setOneOrMoreBytes(ByteString.copyFromUtf8("qwerty"))
                .setNotVegetable(CHICKEN)
                .setNotEmptyString("   ")
                .setNotDefault(Enclosed.newBuilder().setValue("  "));
        assertValid(singulars);
    }

    @Nested
    @DisplayName("a `string` field")
    class StringField {

        private static final String FIELD = "not_empty_string";

        @Test
        @DisplayName("cannot be empty")
        void empty() {
            Singulars.Builder singulars = Singulars.newBuilder();
            checkViolation(singulars, FIELD);
        }

        @Test
        @DisplayName("must have a non-empty value")
        void acceptNonEmptyString() {
            Singulars.Builder singulars = Singulars
                    .newBuilder()
                    .setNotEmptyString(" ")
                    .setNotVegetable(FISH)
                    .setNotDefault(Enclosed.newBuilder().setValue("  "))
                    .setOneOrMoreBytes(ByteString.copyFromUtf8("foobar"));
            assertValid(singulars);
        }
    }

    @Nested
    @DisplayName("a `bytes` field")
    class BytesField {

        private static final String FIELD = "one_or_more_bytes";

        @Test
        @DisplayName("cannot be empty")
        void empty() {
            Singulars.Builder singulars = Singulars.newBuilder();
            checkViolation(singulars, FIELD);
        }

        @Test
        @DisplayName("must have bytes, allowing all zeros")
        void nonEmpty() {
            Singulars.Builder nonZeros = Singulars.newBuilder()
                    .setNotDefault(Enclosed.newBuilder().setValue("non-default enclosed"))
                    .setNotVegetable(CHICKEN)
                    .setOneOrMoreBytes(ByteString.copyFromUtf8("non-empty"))
                    .setNotEmptyString("str");
            assertValid(nonZeros);

            byte[] zeros = {0};
            Singulars.Builder withZeroes = Singulars.newBuilder()
                    .setOneOrMoreBytes(ByteString.copyFrom(zeros))
                    .setNotVegetable(CHICKEN)
                    .setNotDefault(Enclosed.newBuilder().setValue("   "))
                    .setNotEmptyString("  ");
            assertValid(withZeroes);
        }
    }

    @Nested
    @DisplayName("an enum field")
    class EnumField {

        private static final String FIELD = "not_vegetable";

        @Test
        @DisplayName("cannot have a zero-index enum item value")
        void zeroValue() {
            Singulars.Builder singulars = Singulars
                    .newBuilder()
                    .setNotVegetable(VEGETABLE);
            checkViolation(singulars, FIELD);
        }

        @Test
        @DisplayName("must have a non-zero index item value")
        void acceptNonDefaultEnum() {
            Singulars.Builder singulars = Singulars
                    .newBuilder()
                    .setOneOrMoreBytes(ByteString.copyFrom(new byte[]{0}))
                    .setNotDefault(Enclosed.newBuilder().setValue("baz"))
                    .setNotVegetable(CHICKEN)
                    .setNotEmptyString("not empty");
            assertValid(singulars);
        }
    }

    @Nested
    @DisplayName("a message field")
    class MessageField {

        protected static final String FIELD = "not_default";

        @Test
        @DisplayName("cannot have a default message value")
        void defaultValue() {
            Singulars.Builder singulars = Singulars.newBuilder();
            checkViolation(singulars, FIELD);
        }

        @Test
        @DisplayName("must have a not-default message value")
        void nonDefaultMessage() {
            Singulars.Builder singulars = Singulars.newBuilder()
                    .setNotVegetable(CHICKEN)
                    .setOneOrMoreBytes(ByteString.copyFromUtf8("lalala"))
                    .setNotDefault(Enclosed.newBuilder().setValue(newUuid()))
                    .setNotEmptyString(" ");
            assertValid(singulars);
        }

        @Test
        @DisplayName("cannot be of type `google.protobuf.Empty`")
        void notAllowEmptyRequired() {
            final String fieldName = "impossible";

            AlwaysInvalid.Builder unset = AlwaysInvalid.newBuilder();
            checkViolation(unset, fieldName);

            AlwaysInvalid.Builder set = AlwaysInvalid
                    .newBuilder()
                    .setImpossible(Empty.getDefaultInstance());
            checkViolation(set, fieldName);
        }
    }

    @Test
    @DisplayName("all violations on a single message are collected")
    void collectManyViolations() {
        Singulars instance = Singulars.getDefaultInstance();
        Optional<ValidationError> error = instance.validate();
        assertThat(error)
                .isPresent();
        assertThat(error.get().getConstraintViolationList())
                .hasSize(4);
    }

    @Nested
    @DisplayName("a repeated number field")
    class RepeatedNumberField {

        protected static final String FIELD = "not_empty_list_of_longs";

        @Test
        @DisplayName("cannot be empty")
        void emptyRepeatedInt() {
            Collections.Builder instance = Collections.newBuilder();
            checkViolation(instance, FIELD);
        }

        @Test
        @DisplayName("can have any items, including zero")
        void repeatedInt() {
            Collections.Builder instance = Collections
                    .newBuilder()
                    .addNotEmptyListOfLongs(0L)
                    .putContainsANonEmptyStringValue("", "")
                    .addAtLeastOnePieceOfMeat(CHICKEN)
                    .putNotEmptyMapOfInts(42, 0);
            assertValid(instance);
        }
    }

    @Nested
    @DisplayName("a map field with number values")
    class MapNumberField {

        private static final String FIELD = "not_empty_map_of_ints";

        @Test
        @DisplayName("cannot be empty")
        void empty() {
            Collections.Builder instance = Collections.newBuilder();
            checkViolation(instance, FIELD);
        }

        @Test
        @DisplayName("can have entries with any values, including zero")
        void mapOfInts() {
            Collections.Builder instance = Collections
                    .newBuilder()
                    .putNotEmptyMapOfInts(0, 0)
                    .putContainsANonEmptyStringValue("  ", "")
                    .addAtLeastOnePieceOfMeat(FISH)
                    .addNotEmptyListOfLongs(981L);
            assertValid(instance);
        }
    }

    @Nested
    @DisplayName("a map field with string values")
    class MapStringField {

        private static final String FIELD = "contains_a_non_empty_string_value";

        @Test
        @DisplayName("cannot be empty")
        void empty() {
            Collections.Builder instance = Collections.newBuilder();
            checkViolation(instance, FIELD);
        }

        @Test
        @DisplayName("cannot have a single empty value entry")
        void nonEmptyValue() {
            Collections.Builder empty = Collections.newBuilder()
                    .putContainsANonEmptyStringValue("", "");
            assertInvalid(empty);

            Collections.Builder nonEmpty = Collections.newBuilder()
                    .putContainsANonEmptyStringValue("", "")
                    .putContainsANonEmptyStringValue("foo", "bar")
                    .putNotEmptyMapOfInts(0, 0)
                    .addAtLeastOnePieceOfMeat(FISH)
                    .addNotEmptyListOfLongs(42L);
            assertValid(nonEmpty);
        }

        @Test
        @DisplayName("must have at least one non-empty entry")
        void mapOfStrings() {
            Collections.Builder instance = Collections.newBuilder()
                    .addNotEmptyListOfLongs(42L)
                    .putContainsANonEmptyStringValue("", " ")
                    .putNotEmptyMapOfInts(0, 0)
                    .addAtLeastOnePieceOfMeat(CHICKEN);
            assertValid(instance);
        }
    }

    @Nested
    @DisplayName("a repeated enum field")
    class RepeatedEnumField {

        private static final String FIELD = "at_least_one_piece_of_meat";

        @Test
        @DisplayName("cannot be empty")
        void emptyRepeatedEnum() {
            Collections.Builder instance = Collections.newBuilder();
            checkViolation(instance, FIELD);
        }

        @Test
        @DisplayName("cannot have all items with zero-index enum item value")
        void repeatedDefaultEnum() {
            Collections.Builder allZero = Collections.newBuilder()
                    .putNotEmptyMapOfInts(42, 314)
                    .addAtLeastOnePieceOfMeat(VEGETABLE)
                    .addAtLeastOnePieceOfMeat(VEGETABLE)
                    .putContainsANonEmptyStringValue("  ", "   ")
                    .addNotEmptyListOfLongs(42L);
            checkViolation(allZero, FIELD);
        }

        @Test
        @DisplayName("must have at least one value with non-zero emum item value")
        void repeatedEnum() {
            Collections.Builder instance = Collections.newBuilder()
                    .putContainsANonEmptyStringValue("", "")
                    .addNotEmptyListOfLongs(24L)
                    .putNotEmptyMapOfInts(0, 42)
                    .addAtLeastOnePieceOfMeat(FISH)
                    .addAtLeastOnePieceOfMeat(VEGETABLE);
            assertValid(instance);
        }
    }

    @Nested
    @DisplayName("the first field in a message which is")
    class FirstFieldCheck {

        @Nested
        @DisplayName("a command")
        class InCommand {

            @Test
            @DisplayName("cannot be empty")
            void notSet() {
                CreateProject.Builder msg = CreateProject.newBuilder();
                checkViolation(msg, "id");
            }

            @Test
            @DisplayName("must have a non-empty value")
            void set() {
                CreateProject.Builder msg = CreateProject.newBuilder()
                        .setId(newUuid());
                assertValid(msg);
            }
        }

        @Nested
        @DisplayName("an event")
        class InEvent {

            @Test
            @DisplayName("can be empty")
            void notSet() {
                ProjectCreated.Builder msg = ProjectCreated.newBuilder();
                assertValid(msg);
            }
        }

        @Nested
        @DisplayName("a rejection")
        class InRejection {

            @Test
            @DisplayName("can be empty")
            void notSet() {
                CannotCreateProject.Builder msg = CannotCreateProject.newBuilder();
                assertValid(msg);
            }
        }

        @Nested
        @DisplayName("an entity state")
        class InEntityState {

            @Test
            @DisplayName("cannot be empty")
            void notSet() {
                Project.Builder msg = Project.newBuilder();
                checkViolation(msg, "id");
            }

            @Test
            @DisplayName("must have a non-empty value")
            void set() {
                Project.Builder msg = Project.newBuilder()
                        .setId(newUuid());
                assertValid(msg);
            }

            @Test
            @DisplayName("allowing to omit, if set as not `required` explicitly")
            void notRequired() {
                Task.Builder msg = Task.newBuilder();
                assertValid(msg);
            }
        }
    }

    private static void checkViolation(Message.Builder message, String field) {
        checkViolation(message, field, "must be set");
    }

    private static void checkViolation(Message.Builder message,
                                       String field,
                                       String errorMessagePart) {
        List<ConstraintViolation> violations = assertInvalid(message);
        IterableSubject assertViolations = assertThat(violations);
        assertViolations
                .comparingElementsUsing(fieldName)
                .contains(field);
        ConstraintViolation violation = violationAtField(violations, field);
        assertThat(violation.getMsgFormat())
                .contains(errorMessagePart);
    }

    private static ConstraintViolation
    violationAtField(List<ConstraintViolation> violations, String fieldName) {
        return violations
                .stream()
                .filter(violation -> violation.getFieldPath()
                                              .getFieldName(0)
                                              .equals(fieldName))
                .findFirst()
                .orElseGet(() -> fail(format(
                        "No violation for field `%s`. Violations: %s", fieldName, violations
                )));
    }
}
