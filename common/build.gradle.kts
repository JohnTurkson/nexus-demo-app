plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}


java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.3.70")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0")
    implementation("io.ktor:ktor-client-okhttp:1.3.2")
    implementation("io.ktor:ktor-client-cio:1.3.2")
    implementation("io.ktor:ktor-client-websockets:1.3.2")
    implementation("io.ktor:ktor-client-json-jvm:1.3.2")
    implementation("io.ktor:ktor-client-serialization-jvm:1.3.2")
}
