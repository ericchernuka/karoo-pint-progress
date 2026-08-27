import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import java.util.Base64
plugins { alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    jacoco }
val pintVersionCode = providers.gradleProperty("pintVersionCode")
    .map { it.toInt() }
    .orElse(2)
val pintVersionName = providers.gradleProperty("pintVersionName")
    .orElse("1.0.0")
val releaseSigningValues = listOf("PINT_KEY_ALIAS" to providers.environmentVariable("PINT_KEY_ALIAS").orNull, "PINT_KEY_PASSWORD" to providers.environmentVariable("PINT_KEY_PASSWORD").orNull, "PINT_KEYSTORE_PASSWORD" to providers.environmentVariable("PINT_KEYSTORE_PASSWORD").orNull, "PINT_KEYSTORE_BASE64" to providers.environmentVariable("PINT_KEYSTORE_BASE64").orNull,
)
val releaseSigningConfigured = releaseSigningValues.any { !it.second.isNullOrBlank() }
val releaseSigningReady = releaseSigningValues.all { !it.second.isNullOrBlank() }
if (releaseSigningConfigured && !releaseSigningReady) { val missing = releaseSigningValues.filter { it.second.isNullOrBlank() }.map { it.first }
    throw GradleException("Release signing is partially configured. Missing: ${missing.joinToString()}") }
jacoco { toolVersion = "0.8.15" }
android { namespace = "io.ericchernuka.pintprogress"
    compileSdk = 34
    defaultConfig { applicationId = "io.ericchernuka.pintprogress"
        minSdk = 23
        targetSdk = 34
        versionCode = pintVersionCode.get()
        versionName = pintVersionName.get() }
    if (releaseSigningReady) { signingConfigs { create("release") { val keystoreFile = File.createTempFile("pint-progress-release-", ".jks")
                keystoreFile.deleteOnExit()
                keystoreFile.writeBytes(Base64.getDecoder().decode(releaseSigningValues.first { it.first == "PINT_KEYSTORE_BASE64" }.second!!, ), )
                keyAlias = releaseSigningValues.first { it.first == "PINT_KEY_ALIAS" }.second!!
                keyPassword = releaseSigningValues.first { it.first == "PINT_KEY_PASSWORD" }.second!!
                storeFile = keystoreFile
                storePassword = releaseSigningValues.first { it.first == "PINT_KEYSTORE_PASSWORD" }.second!! } } }
    buildTypes { release { if (releaseSigningReady) { signingConfig = signingConfigs.getByName("release") }
            isMinifyEnabled = false } }
    buildFeatures { buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8 }
    kotlinOptions { jvmTarget = "1.8" }
    testOptions { unitTests.all { it.extensions.configure<JacocoTaskExtension> { isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*") } } } }
dependencies { implementation(project(":lib"))
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test) }
val behaviorClasses = layout.buildDirectory.dir("tmp/kotlin-classes/debug/io/ericchernuka/pintprogress/core")
tasks.register<JacocoCoverageVerification>("jacocoBehaviorTestCoverageVerification") { dependsOn("testDebugUnitTest")
    classDirectories.setFrom(fileTree(behaviorClasses) { include("**/*.class") })
    sourceDirectories.setFrom(files("src/main/kotlin"))
    executionData.setFrom(fileTree(layout.buildDirectory) { include("jacoco/testDebugUnitTest.exec") })
    violationRules { rule { listOf("INSTRUCTION", "BRANCH").forEach { counterType -> limit { counter = counterType
                    value = "COVEREDRATIO"
                    minimum = "1.0".toBigDecimal() } } } } }
