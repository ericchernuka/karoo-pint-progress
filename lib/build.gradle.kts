import org.jetbrains.dokka.gradle.engine.plugins.DokkaHtmlPluginParameters
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.LocalDateTime

/*
 * Modified by Eric Chernuka for Pint Progress. See NOTICE for details.
 * Upstream GitHub Packages publishing is deliberately excluded from this application repository.
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.dokka)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

val libVersion = "1.1.9"

android {
    namespace = "io.hammerhead.karooext"
    compileSdk = 34

    defaultConfig {
        minSdk = 23

        buildConfigField("String", "LIB_VERSION", "\"$libVersion\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    buildFeatures {
        buildConfig = true
        aidl = true
    }

}

dokka {
    dokkaPublications.html {
        moduleName.set("karoo-ext")
        moduleVersion.set(libVersion)
        suppressInheritedMembers.set(true)
    }
    pluginsConfiguration.named("html", DokkaHtmlPluginParameters::class.java) {
        val assetsDir = rootDir.resolve("assets")
        homepageLink.set("https://github.com/hammerheadnav/karoo-ext")
        footerMessage.set("© ${LocalDateTime.now().year} SRAM LLC.")
        customAssets.from(assetsDir.resolve("logo-icon.svg"))
        customStyleSheets.from(assetsDir.resolve("hammerhead-style.css"))
    }
    dokkaSourceSets.configureEach {
        // A bug exists in dokka for Android libraries that prevents this from being generated
        // https://github.com/Kotlin/dokka/issues/2876
        sourceLink {
            localDirectory.set(projectDir.resolve("src/main/kotlin"))
            remoteUrl("https://github.com/ericchernuka/karoo-pint-progress/blob/main/lib/src/main/kotlin")
            remoteLineSuffix.set("#L")
        }
        skipEmptyPackages.set(true)
        documentedVisibilities.set(setOf(VisibilityModifier.Public))
        includes.from("Module.md")
    }
    dokkaSourceSets.named("main") {
        samples.from("src/test/kotlin/Samples.kt")
    }
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)

    dokkaPlugin(libs.jetbrains.dokka.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
}
