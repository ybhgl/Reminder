plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.ybhgl.hiddenapi"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 28
    }
}
