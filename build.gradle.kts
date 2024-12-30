plugins {
    kotlin("multiplatform") version "2.1.0"
    id("org.jetbrains.dokka") version "2.0.0"
    id("org.jetbrains.kotlinx.kover") version "0.9.0"
    `maven-publish`
    signing
}

group = "com.jsoizo"
version = "1.10.0"

buildscript {
    repositories {
        mavenCentral()
    }
}

repositories {
    mavenCentral()
}

val dokkaJar by tasks.registering(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "A Javadoc JAR containing Dokka HTML"
    from(tasks.dokkaHtml.flatMap { it.outputDirectory })
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
    js {
        browser {
        }
        nodejs {
        }
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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
            }
        }
        jvm().compilations["test"].defaultSourceSet {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
                implementation("io.kotest:kotest-runner-junit5:5.9.1")
                implementation("io.kotest:kotest-assertions-core:5.9.1")
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