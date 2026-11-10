plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose"); id("org.jetbrains.kotlin.kapt") }
android {
    namespace = "org.steamlite"
    compileSdk = 35
    defaultConfig { applicationId = "org.steamlite"; minSdk = 26; targetSdk = 34; versionCode = 100; versionName = "1.0.0"; testInstrumentationRunner = "org.steamlite.ImageNetworkCheck" }
    val releaseKey = System.getenv("STEAMLITE_KEYSTORE")
    if (!releaseKey.isNullOrBlank()) signingConfigs.create("distribution") {
        storeFile = file(releaseKey)
        storePassword = System.getenv("STEAMLITE_KEYSTORE_PASSWORD")
        keyAlias = System.getenv("STEAMLITE_KEY_ALIAS") ?: "androiddebugkey"
        keyPassword = System.getenv("STEAMLITE_KEY_PASSWORD")
    }
    buildTypes.getByName("release") {
        isDebuggable = false
        isMinifyEnabled = false
        if (!releaseKey.isNullOrBlank()) signingConfig = signingConfigs.getByName("distribution")
    }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += setOf("META-INF/INDEX.LIST", "META-INF/DEPENDENCIES", "META-INF/*.kotlin_module", "META-INF/versions/**/OSGI-INF/MANIFEST.MF") }
}
dependencies {
    testImplementation("junit:junit:4.13.2")
    implementation("org.jsoup:jsoup:1.24.1")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    kapt("androidx.room:room-compiler:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")
    implementation("com.github.penfeizhou.android.animation:apng:3.0.5")
    implementation("androidx.vectordrawable:vectordrawable-animated:1.2.0")
    implementation("in.dragonbra:javasteam:1.8.0")
    implementation("com.google.protobuf:protobuf-java:4.31.1")
    implementation("org.bouncycastle:bcprov-jdk18on:1.86")
}

