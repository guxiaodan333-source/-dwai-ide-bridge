plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "com.dwai.idebridge"
version = "1.0.0"

repositories {
    mavenCentral()
}

intellij {
    version.set("2023.3.6")
    type.set("IC") // IntelliJ IDEA Community Edition
    pluginName.set("DWAI")
    plugins.set(listOf("Git4Idea"))
    updateSinceUntilBuild.set(false)
}

dependencies {
    implementation(kotlin("stdlib"))
}

tasks {
    initializeIntelliJPlugin {
        selfUpdateCheck.set(false)
    }

    patchPluginXml {
        sinceBuild.set("233")
        untilBuild.set("253.*")
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }

    compileJava {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    runIde {
        // 调试时自动打开的 IDE，默认用当前系统安装的
        // ideDir.set(File("C:\\Program Files\\JetBrains\\PyCharm 2023.3"))
    }
}