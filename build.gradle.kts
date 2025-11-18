import buildscript.CopyBuildResources
import buildscript.GradleBuildProfiles
import org.checkerframework.gradle.plugin.CheckerFrameworkExtension
import java.text.SimpleDateFormat
import java.util.Date

project.setGroup("com.brush")
version     = "0.0.1-SNAPSHOT"
description = "Playground MVC"

// TODO: Challenge - Transform plugin to Java-style
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

tasks.register("copyResources", CopyBuildResources::class, Action<CopyBuildResources> {
    this.getResourcesPathString()
        .set(providers.gradleProperty("resource")
            .orElse("")
            .map{ profileString -> GradleBuildProfiles.values()
                .find{ it.stringRepresentation.equals(profileString) }
                ?: throw GradleException("Cannot determine profile for build resources, please supply correct one (ex: -Presource=local)")
            }.map{ when (it) {
                GradleBuildProfiles.PRODUCTION, GradleBuildProfiles.LOCAL -> "resources/${it.stringRepresentation}"
        }})
})


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
    register<Copy>("generateProfileEnum") {
        // TODO: Some modification in package section
        this.from("buildSrc/src/main/java/buildscript/SpringProfiles.java")
        this.into("src/main/java/com/brush/generated")
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

