import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.20.0"
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    // 依存は plugin → engine の一方向。engine とその依存(Kuromoji)は配布 zip の lib/ に入る。
    implementation(project(":engine"))

    intellijPlatform {
        intellijIdeaCommunity("2025.2.6.2")
        bundledPlugin("com.intellij.java")
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252"
            untilBuild = "252.*"
        }
    }
}

tasks.test {
    useJUnit()
    jvmArgs("-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
    testLogging {
        showStandardStreams = true
        events("passed", "failed", "skipped")
    }
}
