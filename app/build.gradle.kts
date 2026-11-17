plugins { id("com.android.application") }
android {
    namespace = "btm.m.os4.systemuihook"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.yg.sbonly"
        minSdk = 35
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        resources {
            merges += "META-INF/xposed/*"
        }
    }
}
dependencies {
    compileOnly("io.github.libxposed:api:102.0.0")
    implementation("io.github.libxposed:service:102.0.0")
}
