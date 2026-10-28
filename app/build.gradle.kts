import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}
val local = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
val separateApp = providers.gradleProperty("separateApp").map(String::toBoolean).getOrElse(false)
android {
    namespace = "de.tidalbooks.prototype"
    compileSdk = 35
    defaultConfig {
        applicationId = if (separateApp) "de.tidalbooks.prototype.testapp" else "de.tidalbooks.prototype"
        manifestPlaceholders["appLabel"] = if (separateApp) "Hörbuch Test" else "@string/app_name"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = if (separateApp) "0.3.2-private-test" else "0.3.2-private"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "TIDAL_CLIENT_ID", quoted(local.getProperty("tidal.clientId", "WE3EnCGIVO57t7U0")))
        buildConfigField("String", "TIDAL_SCOPES", quoted(local.getProperty("tidal.scopes", "")))
    }
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
    buildFeatures { compose = true; buildConfig = true }
    local.getProperty("debug.storeFile")?.let { debugStore ->
        signingConfigs.getByName("debug") {
            storeFile = file(debugStore)
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
dependencies {


    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3:1.5.0-alpha01")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.tidal.sdk:auth:0.10.1")
    implementation("com.tidal.sdk:eventproducer:0.3.2")
    implementation("com.tidal.sdk:player:0.0.73")
    implementation("com.github.instacart.truetime-android:library-extension-rx:3.5")
    implementation("org.slf4j:slf4j-nop:2.0.12")
    implementation("androidx.media3:media3-session:1.5.0")
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.room:room-testing:2.8.5")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
