import groovy.util.Node

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

plugins {
    application
    `maven-publish`
    id("com.github.johnrengelman.shadow").version("7.1.2")
}


/** The publishing settings from the root project. */
val spinePublishing = rootProject.the<io.spine.internal.gradle.publish.SpinePublishing>()

/**
 * The ID of the far JAR artifact.
 *
 * This value is also used in `io.spine.tools.mc.java.gradle.Artifacts.kt`.
 */
val pArtifact = spinePublishing.artifactPrefix + "mc-java-plugins"

val protoDataVersion: String by extra

dependencies {
    implementation(project(":mc-java"))
    implementation(project(":mc-java-protoc"))
}

application {
    mainClass.set("io.spine.tools.mc.java.protoc.Plugin")
}

publishing {
    val pGroup = project.group.toString()
    val pVersion = project.version.toString()

    publications {
        create("fat-jar", MavenPublication::class) {
            groupId = pGroup
            artifactId = pArtifact
            version = pVersion
            artifact(tasks.shadowJar)

            /**
             * Manually add the dependency onto `io.spine:protodata`,
             * as there is no good way to remove all the dependencies
             * from the fat JAR artifact, but leave just this one.
             *
             * This dependency is required in order to place the ProtoData plugin
             * onto the build classpath, so that `mc-java` routines
             * could apply it programmatically.
             *
             * The appended content should look like this:
             * ```
             *     <dependency>
             *         <groupId>io.spine</groupId>
             *         <artifactId>protodata</artifactId>
             *         <version>$protoDataVersion</version>
             *         <scope>runtime</scope>
             *
             *
             *         <exclusions>
             *              <exclusion>
             *                  <groupId>io.spine.protodata</groupId>
             *                  <artifactId>*</artifactId>
             *              </exclusion>
             *              <exclusion>
             *                  <groupId>org.jetbrains.kotlin</groupId>
             *                  <artifactId>*</artifactId>
             *              </exclusion>
             *              <exclusion>
             *                  <groupId>com.google.protobuf</groupId>
             *                  <artifactId>*</artifactId>
             *              </exclusion>
             *              <exclusion>
             *                  <groupId>io.spine.tools</groupId>
             *                  <artifactId>*</artifactId>
             *              </exclusion>
             *         </exclusions>
             *    </dependency>
             * ```
             */
            pom.withXml {
                val projectNode: Node = asNode() as Node

                val dependencies = Node(projectNode, "dependencies")
                val dependency = Node(dependencies, "dependency")
                Node(dependency, "groupId", "io.spine")
                Node(dependency, "artifactId", "protodata")
                Node(dependency, "version", protoDataVersion)
                Node(dependency, "scope", "runtime")

                val exclusions = Node(dependency, "exclusions")

                excludeGroupId(exclusions, "io.spine.protodata")
                excludeGroupId(exclusions, "org.jetbrains.kotlin")
                excludeGroupId(exclusions, "com.google.protobuf")
                excludeGroupId(exclusions, "io.spine.tools")
            }
        }
    }
}

tasks.publish {
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    exclude(
        /**
         * Excluding this type to avoid it being located in the fat JAR.
         *
         * Locating this type in its own `io:spine:protodata` artifact is crucial
         * for obtaining proper version values from the manifest file.
         * This file is only present in `io:spine:protodata` artifact.
         */
        "io/spine/protodata/gradle/plugin/Plugin.class",
        "META-INF/gradle-plugins/io.spine.protodata.properties",

        /**
         * Exclude Gradle types to reduce the size of the resulting JAR.
         *
         * Those required for the plugins are available at runtime anyway.
         */
        "org/gradle/**",

        /**
         * Remove all third-party plugin declarations as well.
         *
         * They should be loaded from their respective dependencies.
         */
        "META-INF/gradle-plugins/com**",
        "META-INF/gradle-plugins/net**",
        "META-INF/gradle-plugins/org**")

    setZip64(true)  /* The archive has way too many items. So using the Zip64 mode. */
    archiveClassifier.set("all")    /** To prevent Gradle setting something like `osx-x86_64`. */
    mergeServiceFiles("desc.ref")
}

// See https://github.com/johnrengelman/shadow/issues/153.
tasks.shadowDistTar.get().enabled = false
tasks.shadowDistZip.get().enabled = false
tasks.distTar.get().enabled = false
tasks.distZip.get().enabled = false

fun excludeGroupId(exclusions: Node, groupId: String) {
    val exclusion = Node(exclusions, "exclusion")
    Node(exclusion, "groupId", groupId)
    Node(exclusion, "artifactId", "*")
}
