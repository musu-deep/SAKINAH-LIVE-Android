plugins {
    id("com.android.application")
}

android {
    namespace = "com.sakinah.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sakinah.mobile"
        minSdk = 29
        targetSdk = 36
        versionCode = 4
        versionName = "1.4.0"
    }

    buildFeatures {
        viewBinding = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.github.pedroSG94.RootEncoder:library:2.7.0")
}
