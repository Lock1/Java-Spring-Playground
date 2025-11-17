import org.checkerframework.gradle.plugin.CheckerFrameworkExtension
import java.text.SimpleDateFormat
import java.util.Date

group       = "com.brush"
version     = "0.0.1-SNAPSHOT"
description = "Playground MVC"

plugins {
    java
    id("org.springframework.boot").version("3.5.7")
    id("io.spring.dependency-management").version("1.1.7")
    id("org.checkerframework").version("0.6.61")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

configure<CheckerFrameworkExtension> {
    checkers = listOf(
        "org.checkerframework.checker.nullness.NullnessChecker",
        "org.checkerframework.checker.units.UnitsChecker",
    )
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

enum class Profile(val stringConstant: String) {
    PRODUCTION("production"),
    STAGING("staging"),
    TEST("test"),
    LOCAL("local"),
}


repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.5")
    implementation("com.h2database:h2:2.3.232")

    compileOnly("org.checkerframework:checker-qual:3.49.0")
    testCompileOnly("org.checkerframework:checker-qual:3.49.0")
    checkerFramework("org.checkerframework:checker:3.49.0")

    implementation("com.github.ulisesbocchio:jasypt-spring-boot-starter:3.0.5")

    implementation("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mybatis.spring.boot:mybatis-spring-boot-starter-test:3.0.5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    val GENERATE_PROFILE_ENUM = "generateProfileEnum"

    register<Task>(GENERATE_PROFILE_ENUM) {
        description = "[Custom] Generate Profile Java enum in main source directory"

        val generationTime = SimpleDateFormat("yyyy-MM-dd").format(Date())
        val generatedSource = """
            |package com.brush.generated;

            |/** Gradle Task '${GENERATE_PROFILE_ENUM}': Source generated at ${generationTime} */
            |public enum Profile {
            |    ${Profile.values().joinToString{it.name} };

            |    ${Profile.values().joinToString(separator="\n    ") {
                "public static final String ${it.name}_STRING = \"${it.stringConstant}\";"
            }}
            |}
        """.trimMargin()

        val FILENAME = "src/main/java/com/brush/generated/Profile.java"
        val output = file(FILENAME)
        output.parentFile.mkdirs()
        output.writeText(generatedSource)
        logger.quiet("Successfully generated {} at {}", FILENAME, generationTime)
    }


    withType<Test> {
        useJUnitPlatform()
    }

    withType<JavaCompile> {
        options.compilerArgs.addAll(listOf(
            "-AcheckPurityAnnotations",
            "-AsuppressWarnings=uninitialized",
        ))
    }
}

