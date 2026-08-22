import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    jacoco
}

jacoco {
    toolVersion = "0.8.15"
}

android {
    namespace = "io.ericchernuka.pintprogress"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.ericchernuka.pintprogress"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    testOptions {
        unitTests.all {
            it.extensions.configure<JacocoTaskExtension> {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }
    }
}

dependencies {
    implementation(project(":lib"))
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
}

val behaviorClasses = layout.buildDirectory.dir("tmp/kotlin-classes/debug/io/ericchernuka/pintprogress/core")

tasks.register<JacocoReport>("jacocoBehaviorTestReport") {
    dependsOn("testDebugUnitTest")

    classDirectories.setFrom(fileTree(behaviorClasses) {
        include("**/*.class")
    })
    sourceDirectories.setFrom(files("src/main/kotlin"))
    executionData.setFrom(fileTree(layout.buildDirectory) {
        include("jacoco/testDebugUnitTest.exec")
    })

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.register<JacocoCoverageVerification>("jacocoBehaviorTestCoverageVerification") {
    dependsOn("jacocoBehaviorTestReport")

    classDirectories.setFrom(fileTree(behaviorClasses) {
        include("**/*.class")
    })
    sourceDirectories.setFrom(files("src/main/kotlin"))
    executionData.setFrom(fileTree(layout.buildDirectory) {
        include("jacoco/testDebugUnitTest.exec")
    })

    violationRules {
        rule {
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "1.0".toBigDecimal()
            }
        }
        rule {
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "1.0".toBigDecimal()
            }
        }
    }
}
