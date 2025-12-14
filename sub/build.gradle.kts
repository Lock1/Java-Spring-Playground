plugins {
    id("me.champeau.jmh") version "0.7.3"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.openjdk.jmh:jmh-core:1.37")
}
