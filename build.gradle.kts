import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kover)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.kotest)
}

group = "com.jsoizo"
version = "2.0.0-dev1"
val projectName = "kotlin-csv"

buildscript {
    repositories {
        mavenCentral()
    }
}

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
    js {
        browser {
        }
        nodejs {
        }
    }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.io)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.kotest.framework.engine)
                implementation(libs.kotlinx.datetime)
            }
        }

        wasmJsTest {
            dependencies {
                implementation(kotlin("test-wasm-js"))
            }
        }

        jvm().compilations["main"].defaultSourceSet {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        jvm().compilations["test"].defaultSourceSet {
            dependencies {
                implementation(libs.bundles.kotest)
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

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    if (project.hasProperty("signing.keyId")) {
        signAllPublications()
    }

    coordinates(group.toString(), projectName, version.toString())

    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Dokka("dokkaHtml"),
            sourcesJar = true,
            androidVariantsToPublish = listOf("debug", "release"),
        )
    )

    val repo = "github.com/jsoizo/${projectName}"
    val repoHttpUrl = "https://${repo}"
    val repoGitUrl = "git://${repo}.git"

    pom {
        name = projectName
        description = "Pure Kotlin CSV reader and writer"
        inceptionYear = "2019"
        url = repoHttpUrl
        organization {
            name.set("com.jsoizo")
            url.set("https://github.com/jsoizo")
        }
        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("${repoHttpUrl}/blob/master/LICENSE")
            }
        }
        scm {
            url.set(repoHttpUrl)
            connection.set("scm:git:${repoGitUrl}")
            developerConnection.set(repoHttpUrl)
        }
        developers {
            developer {
                name.set("jsoizo")
            }
        }
    }
}
