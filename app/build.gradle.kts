import org.gradle.api.artifacts.VersionCatalogsExtension

val libsCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hiltAndroid)
    alias(libs.plugins.ksp)
}

android {
    namespace = "co.farmpulse.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "co.farmpulse.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libsCatalog.findLibrary("retrofit").get())
    implementation(libsCatalog.findLibrary("retrofitConverterGson").get())
    implementation(libsCatalog.findLibrary("okhttpLoggingInterceptor").get())
    implementation(libsCatalog.findLibrary("kotlinxCoroutinesAndroid").get())
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.1")
    implementation(libsCatalog.findLibrary("androidxLifecycleViewmodelCompose").get())
    implementation(libsCatalog.findLibrary("androidxNavigationCompose").get())
    implementation(libsCatalog.findLibrary("androidxRoomRuntime").get())
    implementation(libsCatalog.findLibrary("androidxRoomKtx").get())
    ksp(libsCatalog.findLibrary("androidxRoomCompiler").get())
    implementation(libsCatalog.findLibrary("androidxDatastorePreferences").get())
    implementation(libsCatalog.findLibrary("coilCompose").get())
    implementation("io.coil-kt:coil-svg:2.7.0")
    implementation(libsCatalog.findLibrary("playServicesLocation").get())
    implementation(libsCatalog.findLibrary("accompanistPermissions").get())
    implementation(libsCatalog.findLibrary("hiltAndroid").get())
    ksp(libsCatalog.findLibrary("hiltCompiler").get())
    implementation(libsCatalog.findLibrary("androidxHiltNavigationCompose").get())
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("androidx.compose.material:material-icons-extended")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}