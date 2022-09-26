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

import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.protobuf
import io.spine.internal.dependency.Grpc
import io.spine.internal.dependency.JavaPoet
import io.spine.internal.dependency.Protobuf
import io.spine.internal.dependency.Roaster
import io.spine.internal.dependency.Spine
import io.spine.internal.gradle.WriteVersions

val baseVersion: String by extra
val protoDataVersion: String by extra

val spine = Spine(project)
val validation = spine.validation

dependencies {
    implementation(JavaPoet.lib)
    implementation(Roaster.api) {
        exclude(group = "com.google.guava")
    }
    implementation(Roaster.jdt) {
        exclude(group = "com.google.guava")
    }
    implementation(Protobuf.GradlePlugin.lib)

    implementation(project(":mc-java-base"))
    implementation(project(":mc-java-annotation"))
    implementation(project(":mc-java-checks"))
    implementation(project(":mc-java-rejection"))
    implementation(project(":mc-java-protodata-params"))

    implementation("io.spine:protodata:$protoDataVersion")
    implementation("io.spine.protodata:protodata-compiler:$protoDataVersion")
    implementation(validation.config)

    testImplementation(gradleTestKit())
    testImplementation(spine.testlib)
    testImplementation(spine.pluginTestlib)
}

protobuf {
    generateProtoTasks {
        all().forEach { task ->
            val scope = task.sourceSet.name
            task.generateDescriptorSet = true
            with(task.descriptorSetOptions) {
                path = "$buildDir/descriptors/${scope}/io.spine.tools.spine-mc-java-${scope}.desc"
                includeImports = true
                includeSourceInfo = true
            }
        }
    }
}

tasks {
    /**
     * Tests use the artifacts published to `mavenLocal`, so we need to publish them all first.
     */
    test {
        dependsOn(rootProject.tasks.named("localPublish"))
    }

    withType<WriteVersions>().configureEach {
        version(Grpc.protobufPlugin)
        version(validation.java)
    }
}
