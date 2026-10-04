plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "de.eschoenawa.aoasample.common"
    compileSdk = 36
    defaultConfig { minSdk = 31 }
}

dependencies {
    implementation(libs.core.ktx)
    api(libs.coroutines.android)
}
