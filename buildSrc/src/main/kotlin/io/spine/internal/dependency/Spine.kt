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

package io.spine.internal.dependency

import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.extra

/**
 * Dependencies on Spine `base` modules.
 *
 * @constructor
 * Creates a new instance of `Spine` taking the `baseVersion` from the given project's
 * extra properties.
 */
class Spine(p: ExtensionAware) {

    val base = "io.spine:spine-base:${p.baseVersion}"
    val testlib = "io.spine.tools:spine-testlib:${p.baseVersion}"

    val toolBase = "io.spine.tools:spine-tool-base:${p.toolBaseVersion}"
    val pluginBase = "io.spine.tools:spine-plugin-base:${p.toolBaseVersion}"
    val pluginTestlib = "io.spine.tools:spine-plugin-testlib:${p.toolBaseVersion}"

    val modelCompiler = "io.spine.tools:spine-model-compiler:${p.mcVersion}"

    val validation = Validation(p)

    private val ExtensionAware.baseVersion: String
        get() = extra["baseVersion"] as String
    private val ExtensionAware.mcVersion: String
        get() = extra["mcVersion"] as String
    private val ExtensionAware.toolBaseVersion: String
        get() = extra["toolBaseVersion"] as String

    /**
     * Dependencies on Spine validation modules.
     *
     * See [`SpineEventEngine/validation`](https://github.com/SpineEventEngine/validation/).
     */
    class Validation(p: ExtensionAware) {

        val runtime = "io.spine.validation:runtime:${p.validationVersion}"
        val java = "io.spine.validation:java:${p.validationVersion}"
        val model = "io.spine.validation:model:${p.validationVersion}"
        val config = "io.spine.validation:configuration:${p.validationVersion}"

        private val ExtensionAware.validationVersion: String
            get() = extra["validationVersion"] as String
    }

    /**
     * Dependencies on ProtoData modules.
     *
     * See [`SpineEventEngine/ProtoData`](https://github.com/SpineEventEngine/ProtoData/).
     */
    object ProtoData {

        const val pluginId = "io.spine.proto-data"

        /**
         * The version of ProtoData.
         *
         * We declare ProtoData version here instead of `versions.gradle.kts` because we later use
         * it in a `plugins` section in a build script.
         */
        const val version = "0.1.2"
        const val pluginLib = "io.spine:proto-data:$version"
    }
}
