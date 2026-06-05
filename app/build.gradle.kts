plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.geovision.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.geovision.mobile"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("core") {
            dimension = "distribution"
            buildConfigField("boolean", "IS_ESRI_VIEWER_INCLUDED", "false")
        }
        create("esri") {
            dimension = "distribution"
            minSdk = 28
            buildConfigField("boolean", "IS_ESRI_VIEWER_INCLUDED", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")

    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.documentfile:documentfile:1.0.1")

    // ────────────────────────────────────────────────────────────────────────────
    // Security - Encrypted Preferences
    // ────────────────────────────────────────────────────────────────────────────
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("org.osmdroid:osmdroid-android:6.1.18")

    implementation("org.maplibre.gl:android-sdk:11.5.2")

    // File GeoDatabase (.gdb) - parsed by GdbParser.kt (pure Kotlin binary .gdbtable reader)
    // Optional alternative: "org.jfgdb:jfgdb:0.1.4" from http://jfgdb.s3-website-eu-west-1.amazonaws.com/maven2

    // JTS Topology Suite for geometry operations
    implementation("org.locationtech.jts:jts-core:1.19.0")

    // CRS reprojection without native GDAL/PROJ dependencies.
    implementation("org.locationtech.proj4j:proj4j:1.4.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("net.sf.kxml:kxml2:2.3.0")
    testImplementation("org.json:json:20090211")

    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    "esriImplementation"(project(":geovision-esri-viewer"))
}
