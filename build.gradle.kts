plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.7.2"
}

group = "com.local"
version = "0.2.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "io.github.zhoutl.ai-commit-message"
        name = "AI Commit Message Generator"
        version = project.version.toString()
        ideaVersion {
            sinceBuild = "261"
            untilBuild = "263.*"
        }
    }
}

dependencies {
    intellijPlatform {
        local("C:/Program Files/Android/Android Studio")
        bundledPlugin("Git4Idea")
    }
}
