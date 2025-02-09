import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    java
    kotlin("multiplatform") version "2.1.10"
    id("org.jetbrains.dokka").version("1.7.20")
    `maven-publish`
    signing
    jacoco
}

group = "com.jsoizo"
version = "1.10.0"

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.7.20")
    }
}

repositories {
    mavenCentral()
}

val dokkaJar = task<Jar>("dokkaJar") {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveClassifier.set("javadoc")
}

kotlin {
    jvm {
        compilations.forEach {
            it.kotlinOptions.jvmTarget = "1.8"
        }
        //https://docs.gradle.org/current/userguide/publishing_maven.html
        mavenPublication {
            artifact(dokkaJar)
        }
    }
    js(IR) {
        browser {
        }
        nodejs {
        }
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    sourceSets {
        commonMain {}
        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        jvm().compilations["main"].defaultSourceSet {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
            }
        }
        jvm().compilations["test"].defaultSourceSet {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
                implementation("io.kotest:kotest-runner-junit5:4.6.3")
                implementation("io.kotest:kotest-assertions-core:4.6.3")
            }
        }
        js().compilations["main"].defaultSourceSet {
            dependencies {
            }
        }
        js().compilations["test"].defaultSourceSet {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

tasks.withType<Test>() {
    useJUnitPlatform()
}


publishing {
    publications.all {
        (this as MavenPublication).pom {
            name.set("kotlin-csv")
            description.set("Kotlin CSV Reader/Writer")
            url.set("https://github.com/jsoizo/kotlin-csv")

            organization {
                name.set("com.jsoizo")
                url.set("https://github.com/jsoizo")
            }
            licenses {
                license {
                    name.set("Apache License 2.0")
                    url.set("https://github.com/jsoizo/kotlin-csv/blob/master/LICENSE")
                }
            }
            scm {
                url.set("https://github.com/jsoizo/kotlin-csv")
                connection.set("scm:git:git://github.com/jsoizo/kotlin-csv.git")
                developerConnection.set("https://github.com/jsoizo/kotlin-csv")
            }
            developers {
                developer {
                    name.set("jsoizo")
                }
            }
        }
    }
    repositories {
        maven {
            credentials {
                val nexusUsername: String? by project
                val nexusPassword: String? by project
                username = nexusUsername
                password = nexusPassword
            }

            val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
        }
    }
}

signing {
    sign(publishing.publications)
}

/////////////////////////////////////////
//         Jacoco setting              //
/////////////////////////////////////////
jacoco {
    toolVersion = "0.8.8"
}
tasks.jacocoTestReport {
    val coverageSourceDirs = arrayOf(
        "commonMain/src",
        "jvmMain/src"
    )
    val classFiles = File("${buildDir}/classes/kotlin/jvm/")
        .walkBottomUp()
        .toSet()
    classDirectories.setFrom(classFiles)
    sourceDirectories.setFrom(files(coverageSourceDirs))
    additionalSourceDirs.setFrom(files(coverageSourceDirs))

    executionData
        .setFrom(files("${buildDir}/jacoco/jvmTest.exec"))

    reports {
        xml.required.set(true)
        html.required.set(false)
    }
}
