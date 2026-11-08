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
    // 設定画面は 1 つだけで検索の恩恵がほぼ無い。生成物(searchableOptions jar)には生成時の IDE の
    // 保存先パス(開発機の絶対パス)が入るので、配布物に入れないよう作らない。
    buildSearchableOptions = false

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
