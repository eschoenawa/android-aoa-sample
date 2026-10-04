plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "de.eschoenawa.aoasample.host"
    compileSdk = 36
    defaultConfig { minSdk = 31 }
}

dependencies {
    implementation(project(":aoa:common"))
    implementation(libs.core.ktx)
    api(libs.coroutines.android)
}
