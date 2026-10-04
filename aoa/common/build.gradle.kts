plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "de.eschoenawa.aoasample.common"
    compileSdk = 36
    defaultConfig { minSdk = 31 }
}
