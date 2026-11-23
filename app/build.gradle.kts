plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
}
android {
 namespace = "com.bentleyplayz.screenrecorder"
 compileSdk = 36
 defaultConfig {
  applicationId = "com.bentleyplayz.screenrecorder"
  minSdk = 29
  targetSdk = 35
  versionCode = 2
  versionName = "1.1.0"
 }
 buildTypes { release { isMinifyEnabled = false } }
 compileOptions {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies { implementation("androidx.core:core-ktx:1.15.0") }
