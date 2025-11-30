import buildscript.CopyBuildResources
import buildscript.GradleBuildProfiles
import org.checkerframework.gradle.plugin.CheckerFrameworkExtension
import java.text.SimpleDateFormat
import java.util.Date

object Helper {
    // "Function literal with receiver"
    fun <T> lambdaToFLReceiver(lambda: (T) -> Unit): T.() -> Unit {
        return fun T.() { lambda(this) }
    }

    fun <T> lambdaToAction(lambda: (T) -> Unit): Action<T> {
        return Action<T> { lambda(this) }
    }
}

project.setGroup("com.brush")
version     = "0.0.1-SNAPSHOT"
description = "Playground MVC"

project.plugins{
    val gradlePlugin: PluginDependenciesSpec = this;
    gradlePlugin.id("java")
    gradlePlugin.id("org.springframework.boot").version("3.5.7")
    gradlePlugin.id("io.spring.dependency-management").version("1.1.7")
    gradlePlugin.id("org.checkerframework").version("0.6.61")
}

project.java
    .getToolchain()
    .getLanguageVersion()
    .set(JavaLanguageVersion.of(24))

project.configure(Helper.lambdaToFLReceiver({ checkerPlugin: CheckerFrameworkExtension ->
    checkerPlugin.setCheckers(listOf(
        // "org.checkerframework.checker.nullness.NullnessChecker",
        // "org.checkerframework.checker.units.UnitsChecker",
        "org.checkerframework.checker.tainting.TaintingChecker",
    ))
}))


configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

tasks.register("copyResources", CopyBuildResources::class, Helper.lambdaToAction({ copyTask: CopyBuildResources ->
    copyTask.getResourcesPathString()
        .set(providers.gradleProperty("resource")
        .orElse("")
        .map{ profileString -> GradleBuildProfiles.values()
        .find{ it.stringRepresentation.equals(profileString) }
        ?: throw GradleException("Cannot determine profile for build resources, please supply correct one (ex: -Presource=local)")
    }.map{ when (it) {
        GradleBuildProfiles.PRODUCTION, GradleBuildProfiles.LOCAL -> "resources/${it.stringRepresentation}"
    }})
    copyTask.into("")
}))


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

    implementation("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mybatis.spring.boot:mybatis-spring-boot-starter-test:3.0.5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

project.tasks(Helper.lambdaToFLReceiver{ taskContainer: TaskContainer ->
    taskContainer.withType(Test::class)
        .all(Helper.lambdaToAction{ testTask: Test -> testTask.useJUnitPlatform() });

    taskContainer.withType(JavaCompile::class)
        .all(Helper.lambdaToAction{ javaCompileTask: JavaCompile ->
            javaCompileTask.getOptions()
                .getCompilerArgs()
                .addAll(listOf(
                    // "-AcheckPurityAnnotations",
                    // "-AsuppressWarnings=uninitialized",
                ));
        });
});

