plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
}

val releaseStoreFile = System.getenv("RELEASE_KEYSTORE_PATH")
val releaseStorePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("RELEASE_KEY_ALIAS")
val releaseKeyPassword = System.getenv("RELEASE_KEY_PASSWORD")

android {
 namespace = "com.bentleyplayz.screenrecorder"
 compileSdk = 36
 defaultConfig {
  applicationId = "com.bentleyplayz.screenrecorder"
  minSdk = 29
  targetSdk = 35
  versionCode = 3
  versionName = "1.2.0"
 }
 signingConfigs {
  create("release") {
   if (!releaseStoreFile.isNullOrBlank()) storeFile = file(releaseStoreFile)
   storePassword = releaseStorePassword
   keyAlias = releaseKeyAlias
   keyPassword = releaseKeyPassword
  }
 }
 buildTypes {
  debug { }
  release {
   isMinifyEnabled = false
   signingConfig = signingConfigs.getByName("release")
  }
 }
 compileOptions {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies { implementation("androidx.core:core-ktx:1.15.0") }
