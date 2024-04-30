plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.devtools.ksp")
    id("kotlin-kapt")
}

android {
    namespace = "com.tans.tasciiartplayer"
    compileSdk = properties["ANDROID_COMPILE_SDK"].toString().toInt()

    defaultConfig {
        applicationId = "com.tans.tasciiartplayer"
        minSdk = properties["ANDROID_MIN_SDK"].toString().toInt()
        targetSdk = properties["ANDROID_TARGET_SDK"].toString().toInt()
        versionCode = properties["VERSION_CODE"].toString().toInt()
        versionName = properties["VERSION_NAME"].toString()

        setProperty("archivesBaseName", "tasciiartplayer-${properties["VERSION_NAME"].toString()}")
    }

    signingConfigs {
        val debugConfig = this.getByName("debug")
        with(debugConfig) {
            storeFile = File(projectDir, "debugkey${File.separator}debug.jks")
            storePassword = "123456"
            keyAlias = "key0"
            keyPassword = "123456"
        }
    }

    buildTypes {

        debug {
            multiDexEnabled = true
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("debug")
        }
        release {
            multiDexEnabled = true
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        viewBinding {
            enable = true
        }
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.core.jvm)
    implementation(libs.coroutines.android)

    // Glide
    implementation(libs.glide)
    ksp(libs.glide.codegen.ksp)

    // Tans
    implementation(libs.tmediaplayer)
    implementation(libs.tuiutils)

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.codegen)
}