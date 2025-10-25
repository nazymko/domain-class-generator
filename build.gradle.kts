plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "com.example.domaingenerator"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        create("IC", "2025.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // REQUIRED for Java PSI access (PsiClass, PsiMethod, etc.)
        bundledPlugin("com.intellij.java")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
            untilBuild = "999.*"  // Compatible with all future versions
        }

        changeNotes = """
            <h3>Version 1.0.0 - Initial Release</h3>
            <ul>
                <li><b>Code Generation:</b> Generate domain classes and enums from library packages</li>
                <li><b>Enum Support:</b> Preserves enum constructors, constants, and field declarations</li>
                <li><b>Dependency Tracking:</b> Automatically generates all field type dependencies for complete domain isolation</li>
                <li><b>Lombok Integration:</b> Configure @Data, @Builder, @Getter, @Setter, and more</li>
                <li><b>JavaDoc Generation:</b> Optional JavaDoc with source information and plugin metadata</li>
                <li><b>Inheritance Support:</b> Follows superclass hierarchies across generated classes</li>
                <li><b>Smart Package Selection:</b> Intelligent package suggestions with native chooser dialog</li>
                <li><b>Real-time Validation:</b> Immediate feedback on configuration inputs</li>
                <li><b>Keyboard Shortcut:</b> Ctrl+Shift+E (Cmd+Shift+E on Mac) for quick access</li>
                <li><b>Progress Tracking:</b> Visual progress indicators for large generation tasks</li>
            </ul>
            <h4>Why Use This Plugin?</h4>
            <p>
                Avoid tight coupling with external libraries by creating your own domain model copies.
                Protects against version changes, security risks, and provides full control over your domain layer.
            </p>
        """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
    }
}
