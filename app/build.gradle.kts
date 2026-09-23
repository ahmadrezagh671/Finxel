plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.ahmadrezagh671.finxel"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.ahmadrezagh671.finxel"
        minSdk = 28
        targetSdk = 36
        versionCode = 24
        versionName = "0.4.14.C01"

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
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.swiperefreshlayout)
    implementation(libs.work.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

    // room db
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // code editor
    implementation(platform(libs.editor.bom))
    implementation(libs.editor)
    implementation(libs.language.textmate)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)

    // google location
    implementation(libs.play.services.location)
}