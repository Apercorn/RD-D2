//
// build.gradle.kts in TeamCode
//
import java.util.regex.Pattern

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.firstinspires.ftc.teamcode"
    compileSdk = 36

    signingConfigs {
        val apkStoreFile = System.getenv("APK_SIGNING_STORE_FILE")
        create("release") {
            if (apkStoreFile != null) {
                keyAlias = System.getenv("APK_SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("APK_SIGNING_KEY_PASSWORD")
                storeFile = file(apkStoreFile)
                storePassword = System.getenv("APK_SIGNING_STORE_PASSWORD")
            } else {
                keyAlias = "androiddebugkey"
                keyPassword = "android"
                storeFile = rootProject.file("libs/ftc.debug.keystore")
                storePassword = "android"
            }
        }
        getByName("debug") {
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storeFile = rootProject.file("libs/ftc.debug.keystore")
            storePassword = "android"
        }
    }

    defaultConfig {
        signingConfig = signingConfigs.getByName("debug")
        applicationId = "com.qualcomm.ftcrobotcontroller"
        minSdk = 24
        @Suppress("ExpiredTargetSdkVersion")
        targetSdk = 28

        // Keep versionCode/versionName in sync with FtcRobotController's AndroidManifest.xml
        val manifestFile = project(":FtcRobotController").file("src/main/AndroidManifest.xml")
        val manifestText = manifestFile.readText()
        versionCode = Pattern.compile("""versionCode="(\d+(\.\d+)*)""").matcher(manifestText)
            .also { it.find() }.group(1).toInt()
        versionName = Pattern.compile("""versionName="(.*)""").matcher(manifestText)
            .also { it.find() }.group(1)
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a") }
        }
        getByName("debug") {
            isDebuggable = true
            isJniDebuggable = true
            ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a") }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
    }

    packaging {
        jniLibs.useLegacyPackaging = true
        jniLibs.pickFirsts.add("**/*.so")
    }

    ndkVersion = "21.3.6528147"
}

repositories {
    mavenCentral()
    google()
    maven { url = uri("https://mymaven.bylazar.com/releases") }
    maven { url = uri("https://maven.brott.dev/") }
}

dependencies {
    // FTC SDK
    implementation("org.firstinspires.ftc:Inspection:11.1.0")
    implementation("org.firstinspires.ftc:Blocks:11.1.0")
    implementation("org.firstinspires.ftc:RobotCore:11.1.0")
    implementation("org.firstinspires.ftc:RobotServer:11.1.0")
    implementation("org.firstinspires.ftc:OnBotJava:11.1.0")
    implementation("org.firstinspires.ftc:Hardware:11.1.0")
    implementation("org.firstinspires.ftc:FtcCommon:11.1.0")
    implementation("org.firstinspires.ftc:Vision:11.1.0")
    implementation("androidx.appcompat:appcompat:1.2.0")

    implementation(project(":FtcRobotController"))

    // TeamCode dependencies
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.7")
    implementation("com.acmerobotics.roadrunner:core:0.5.6")
    implementation("com.acmerobotics.dashboard:dashboard:0.4.15")
    implementation("com.bylazar:fullpanels:1.0.12")
}
