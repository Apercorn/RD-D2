import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

//
// build.gradle.kts in FtcRobotController
//
plugins {
    id("com.android.library")
}

android {
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        @Suppress("ExpiredTargetSdkVersion")
        targetSdk = 28
        buildConfigField(
            "String",
            "APP_BUILD_TIME",
            "\"${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ROOT).format(Date())}\""
        )
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    namespace = "com.qualcomm.ftcrobotcontroller"
}

apply(from = "../build.dependencies.gradle")
