plugins {
    kotlin("jvm") version "1.3.41"
}

group = "jauntsy"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.0-RC")
}