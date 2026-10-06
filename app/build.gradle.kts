plugins { id("com.android.application") }

android {
    namespace = "dev.cameronpak.muser1"
    compileSdk = 36
    buildFeatures { buildConfig = true }
    defaultConfig {
        applicationId = "dev.cameronpak.muser1"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "0.2.0"
        buildConfigField("boolean", "DEMO", "false")
        testInstrumentationRunner = "dev.cameronpak.muser1.DeviceChecks"
    }
    buildTypes {
        create("demo") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".demo"
            versionNameSuffix = "-offline-demo"
            buildConfigField("boolean", "DEMO", "true")
            matchingFallbacks += listOf("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.json:json:20240303")
}
