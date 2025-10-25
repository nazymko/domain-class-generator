# JetBrains IntelliJ IDEA Plugin Creation Guide

This comprehensive guide provides step-by-step instructions for creating a JetBrains IntelliJ IDEA plugin from scratch, based on battle-tested patterns and architecture used in production plugins.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Project Setup](#project-setup)
3. [Plugin Configuration](#plugin-configuration)
4. [Core Components](#core-components)
5. [PSI (Program Structure Interface)](#psi-program-structure-interface)
6. [UI Components](#ui-components)
7. [Threading and Performance](#threading-and-performance)
8. [File Operations](#file-operations)
9. [Testing Strategy](#testing-strategy)
10. [Build and Deployment](#build-and-deployment)
11. [Documentation Maintenance](#documentation-maintenance)
12. [Best Practices](#best-practices)

---

## Prerequisites

- **Java**: JDK 11 or higher
- **Kotlin**: 2.1.0 or higher (recommended)
- **Gradle**: 8.0+ (wrapper included)
- **IntelliJ IDEA**: 2023.1+ (for development)
- **Basic knowledge**: Kotlin/Java, Gradle, IntelliJ Platform SDK

---

## Project Setup

### 1. Create Project Structure

```
project-root/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/yourcompany/yourplugin/
│   │   └── resources/
│   │       └── META-INF/
│   │           └── plugin.xml
│   └── test/
│       └── kotlin/
└── .claude/
    └── CLAUDE.md
```

### 2. Configure build.gradle.kts

**CRITICAL**: This file controls your entire plugin build process.

```kotlin
plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.5.0"  // Use 2.5.0 for stability
}

group = "io.github.yourname.yourplugin"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Choose your target IDE
        // IC = IntelliJ Community, IU = IntelliJ Ultimate
        // Using 2023.2 for development while supporting 2023.0+
        create("IC", "2023.2")

        // REQUIRED for Java PSI access (PsiClass, PsiMethod, etc.)
        bundledPlugin("com.intellij.java")

        // Add if using Kotlin
        bundledPlugin("org.jetbrains.kotlin")

        // Test framework
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "230"  // IntelliJ IDEA 2023.0+
            // No untilBuild means compatible with all future versions
        }

        changeNotes = """
            <h3>Version 1.0.0 - Initial Release</h3>
            <ul>
                <li><b>Feature 1:</b> Description with rich HTML</li>
                <li><b>Feature 2:</b> Another feature</li>
                <li><b>Keyboard Shortcut:</b> Ctrl+Shift+E for quick access</li>
            </ul>
            <h4>Why Use This Plugin?</h4>
            <p>Brief explanation of the problem this plugin solves.</p>
        """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    // Java 17 required for IntelliJ 2023.0+ compatibility
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}
```

**Key Points:**
- **Plugin Version**: Use version 2.5.0 for stability (2.7.0 may have issues)
- **Java Version**: IntelliJ 2023.0+ requires Java 17
- **IDE Version**: Develop with 2023.2 while supporting 2023.0+ (build 230)
- **Change Notes**: Support rich HTML formatting for better presentation

### 3. Configure settings.gradle.kts

```kotlin
rootProject.name = "Your Plugin Name"
```

### 4. Add gradle.properties (Optional but Recommended)

```properties
org.gradle.jvmargs=-Xmx2048m
org.gradle.parallel=true
kotlin.code.style=official
```

---

## Plugin Configuration

### plugin.xml - The Heart of Your Plugin

**Location**: `src/main/resources/META-INF/plugin.xml`

This file defines your plugin's identity, dependencies, and components.

```xml
<idea-plugin>
    <!-- REQUIRED: Unique plugin ID (reverse domain notation) -->
    <id>com.yourcompany.yourplugin</id>

    <!-- REQUIRED: Plugin name (shown in marketplace) -->
    <name>Your Plugin Name</name>

    <!-- REQUIRED: Vendor information -->
    <vendor email="support@yourcompany.com" url="https://yourcompany.com">
        Your Company Name
    </vendor>

    <!-- REQUIRED: Minimum IDE version -->
    <idea-version since-build="230"/>

    <!-- REQUIRED: Plugin description (supports HTML) -->
    <description>
        <![CDATA[
        <h2>Your Plugin Description</h2>
        <p>A comprehensive description of what your plugin does.</p>

        <h3>Features</h3>
        <ul>
            <li>Feature 1</li>
            <li>Feature 2</li>
            <li>Feature 3</li>
        </ul>

        <h3>Usage</h3>
        <p>Instructions on how to use your plugin.</p>
        ]]>
    </description>

    <!-- CRITICAL: Platform dependencies -->
    <depends>com.intellij.modules.platform</depends>

    <!-- Add if working with Java code -->
    <depends>com.intellij.java</depends>

    <!-- Add if working with Kotlin code -->
    <depends optional="true" config-file="kotlin-support.xml">
        org.jetbrains.kotlin
    </depends>

    <!-- Actions: User-triggerable operations -->
    <actions>
        <action id="YourActionId"
                class="com.yourcompany.yourplugin.actions.YourAction"
                text="Action Display Text"
                description="Detailed description of what this action does">

            <!-- Where the action appears in menus -->
            <add-to-group group-id="EditorPopupMenu" anchor="last"/>
            <add-to-group group-id="ProjectViewPopupMenu" anchor="last"/>

            <!-- Keyboard shortcuts -->
            <!-- Windows/Linux -->
            <keyboard-shortcut first-keystroke="ctrl shift S" keymap="$default"/>
            <!-- macOS -->
            <keyboard-shortcut first-keystroke="meta shift S" keymap="Mac OS X"/>
        </action>
    </actions>

    <!-- Application-level services (singleton across IDE) -->
    <applicationListeners>
        <listener class="com.yourcompany.yourplugin.listeners.YourListener"
                  topic="com.intellij.openapi.project.ProjectManagerListener"/>
    </applicationListeners>

    <!-- Project-level services (one per project) -->
    <projectListeners>
        <listener class="com.yourcompany.yourplugin.listeners.YourProjectListener"
                  topic="com.intellij.openapi.vfs.VirtualFileListener"/>
    </projectListeners>

    <!-- Extensions: Extend IDE functionality -->
    <extensions defaultExtensionNs="com.intellij">
        <!-- Example: Add a tool window -->
        <toolWindow id="YourToolWindow"
                    anchor="right"
                    factoryClass="com.yourcompany.yourplugin.toolwindow.YourToolWindowFactory"/>

        <!-- Example: Add a settings page -->
        <projectConfigurable instance="com.yourcompany.yourplugin.settings.YourSettings"
                            displayName="Your Plugin Settings"/>
    </extensions>
</idea-plugin>
```

---

## Core Components

### 1. Actions - User-Triggered Operations

Actions are the primary way users interact with your plugin.

**Key Concepts**:
- **AnAction**: Base class for all actions
- **DumbAware**: Interface for actions that work during indexing
- **ActionUpdateThread**: Controls threading for action updates

**Example Action Implementation**:

```kotlin
package com.yourcompany.yourplugin.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages

/**
 * Main action entry point.
 *
 * IMPORTANT: Implement DumbAware if your action should work during indexing.
 * Otherwise, it will be disabled during background tasks.
 */
class YourAction : AnAction("Your Action Name"), DumbAware {

    /**
     * CRITICAL: Specify threading model for action updates.
     * - BGT (Background Thread): Use for lightweight operations
     * - EDT (Event Dispatch Thread): Use only if you need EDT
     */
    override fun getActionUpdateThread(): ActionUpdateThread =
        ActionUpdateThread.BGT

    /**
     * Called to update action state (enable/disable, visible/hidden).
     * Should be FAST - no heavy computations here!
     */
    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        val file = e.getData(CommonDataKeys.PSI_FILE)

        // Enable only if we have a project, editor, and file
        e.presentation.isEnabledAndVisible =
            project != null && editor != null && file != null
    }

    /**
     * Called when user triggers the action.
     * Can perform heavy operations here.
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        // Your action logic here
        Messages.showInfoMessage(
            project,
            "Action performed!",
            "Success"
        )
    }
}
```

**Common DataKeys**:
```kotlin
// Get project
val project = e.project

// Get editor
val editor = e.getData(CommonDataKeys.EDITOR)

// Get virtual file
val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)

// Get PSI file
val psiFile = e.getData(CommonDataKeys.PSI_FILE)

// Get PSI element at caret
val psiElement = e.getData(CommonDataKeys.PSI_ELEMENT)
```

### 2. Services - Singleton Components

Services provide stateful functionality across your plugin.

**Types**:
- **Application Service**: One instance for entire IDE
- **Project Service**: One instance per project
- **Module Service**: One instance per module

**Example Application Service**:

```kotlin
package com.yourcompany.yourplugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

/**
 * Application-level service (singleton across IDE).
 */
@Service
class YourApplicationService {

    private val cache = mutableMapOf<String, Any>()

    fun cacheValue(key: String, value: Any) {
        cache[key] = value
    }

    fun getCachedValue(key: String): Any? = cache[key]

    companion object {
        fun getInstance(): YourApplicationService = service()
    }
}
```

**Example Project Service**:

```kotlin
package com.yourcompany.yourplugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Project-level service (one per project).
 */
@Service(Service.Level.PROJECT)
class YourProjectService(private val project: Project) {

    fun doSomethingWithProject() {
        // Access project-specific functionality
        val basePath = project.basePath
        // ...
    }

    companion object {
        fun getInstance(project: Project): YourProjectService =
            project.service()
    }
}
```

**Register Services in plugin.xml**:

```xml
<extensions defaultExtensionNs="com.intellij">
    <!-- Application service -->
    <applicationService
        serviceImplementation="com.yourcompany.yourplugin.services.YourApplicationService"/>

    <!-- Project service -->
    <projectService
        serviceImplementation="com.yourcompany.yourplugin.services.YourProjectService"/>
</extensions>
```

### 3. Listeners - React to IDE Events

Listeners allow you to react to various IDE events.

**Example File Change Listener**:

```kotlin
package com.yourcompany.yourplugin.listeners

import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

class YourFileListener : BulkFileListener {
    override fun after(events: List<VFileEvent>) {
        events.forEach { event ->
            // React to file changes
            println("File event: ${event.path}")
        }
    }
}
```

---

## PSI (Program Structure Interface)

PSI is the IntelliJ Platform's representation of source code. Understanding PSI is CRITICAL for any plugin that analyzes or modifies code.

### Key Concepts

**PSI Hierarchy**:
```
PsiFile (root)
  └─ PsiClass (class declaration)
      ├─ PsiField (field)
      ├─ PsiMethod (method)
      │   ├─ PsiParameterList
      │   ├─ PsiCodeBlock (method body)
      │   └─ ...
      └─ PsiClass (nested class)
```

### Working with Java PSI

```kotlin
package com.yourcompany.yourplugin.psi

import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

object PsiHelper {

    /**
     * Get the PsiClass at the caret position.
     */
    fun getClassAtCaret(editor: Editor, psiFile: PsiFile): PsiClass? {
        val offset = editor.caretModel.offset
        val element = psiFile.findElementAt(offset) ?: return null
        return PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
    }

    /**
     * Get all fields in a class.
     */
    fun getFields(psiClass: PsiClass): List<PsiField> {
        return psiClass.fields.toList()
    }

    /**
     * Get all methods in a class.
     */
    fun getMethods(psiClass: PsiClass): List<PsiMethod> {
        return psiClass.methods.toList()
    }

    /**
     * Check if a class has a specific annotation.
     */
    fun hasAnnotation(psiClass: PsiClass, annotationFqn: String): Boolean {
        return psiClass.annotations.any {
            it.qualifiedName == annotationFqn
        }
    }

    /**
     * Get the fully qualified name of a class.
     */
    fun getFqn(psiClass: PsiClass): String? {
        return psiClass.qualifiedName
    }

    /**
     * Get the package name of a class.
     */
    fun getPackageName(psiClass: PsiClass): String? {
        return (psiClass.containingFile as? PsiJavaFile)?.packageName
    }

    /**
     * Check if a field has a getter method.
     */
    fun hasGetter(field: PsiField, psiClass: PsiClass): Boolean {
        val getterName = "get${field.name.capitalize()}"
        return psiClass.methods.any { it.name == getterName }
    }
}
```

### Type Resolution

**CRITICAL**: Proper type resolution is essential for code generation.

```kotlin
package com.yourcompany.yourplugin.types

import com.intellij.psi.*

object TypeResolver {

    /**
     * Resolve the canonical type text (fully qualified name).
     */
    fun getCanonicalText(type: PsiType): String {
        return type.canonicalText
    }

    /**
     * Check if a type is a primitive.
     */
    fun isPrimitive(type: PsiType): Boolean {
        return type is PsiPrimitiveType
    }

    /**
     * Check if a type is a collection.
     */
    fun isCollection(type: PsiType): Boolean {
        val canonicalText = type.canonicalText
        return canonicalText.startsWith("java.util.List") ||
               canonicalText.startsWith("java.util.Set") ||
               canonicalText.startsWith("java.util.Collection")
    }

    /**
     * Extract generic type parameters.
     */
    fun getGenericParameters(type: PsiType): List<PsiType> {
        return when (type) {
            is PsiClassType -> type.parameters.toList()
            else -> emptyList()
        }
    }

    /**
     * Resolve a PsiClass from a PsiType.
     */
    fun resolveClass(type: PsiType): PsiClass? {
        return when (type) {
            is PsiClassType -> type.resolve()
            else -> null
        }
    }
}
```

### Modifying PSI

**CRITICAL**: ALL PSI modifications MUST be done in a write action on EDT.

```kotlin
package com.yourcompany.yourplugin.modifications

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*

object PsiModifier {

    /**
     * Add a method to a class.
     * MUST be called from EDT or within a write action.
     */
    fun addMethod(
        project: Project,
        psiClass: PsiClass,
        methodText: String
    ) {
        WriteCommandAction.runWriteCommandAction(project) {
            val factory = JavaPsiFacade.getElementFactory(project)
            val method = factory.createMethodFromText(methodText, psiClass)
            psiClass.add(method)
        }
    }

    /**
     * Add a field to a class.
     */
    fun addField(
        project: Project,
        psiClass: PsiClass,
        fieldText: String
    ) {
        WriteCommandAction.runWriteCommandAction(project) {
            val factory = JavaPsiFacade.getElementFactory(project)
            val field = factory.createFieldFromText(fieldText, psiClass)
            psiClass.add(field)
        }
    }

    /**
     * Create a new Java file.
     */
    fun createJavaFile(
        project: Project,
        directory: PsiDirectory,
        className: String,
        fileContent: String
    ): PsiFile? {
        return WriteCommandAction.runWriteCommandAction<PsiFile>(project) {
            val factory = JavaPsiFacade.getElementFactory(project)
            val psiFile = PsiFileFactory.getInstance(project)
                .createFileFromText(
                    "$className.java",
                    JavaFileType.INSTANCE,
                    fileContent
                )
            directory.add(psiFile) as PsiFile
        }
    }
}
```

### Working with Enums

**CRITICAL**: Enums require special handling compared to regular classes.

```kotlin
package com.yourcompany.yourplugin.psi

import com.intellij.psi.*

object EnumHandler {

    /**
     * Check if a class is an enum.
     */
    fun isEnum(psiClass: PsiClass): Boolean {
        return psiClass.isEnum
    }

    /**
     * Get enum constants (not regular fields).
     */
    fun getEnumConstants(psiClass: PsiClass): List<PsiEnumConstant> {
        return psiClass.fields.filterIsInstance<PsiEnumConstant>()
    }

    /**
     * Get enum constructors.
     */
    fun getEnumConstructors(psiClass: PsiClass): List<PsiMethod> {
        return psiClass.constructors.toList()
    }

    /**
     * Generate enum fields from constructor parameters.
     * IMPORTANT: Some enum fields only exist implicitly in constructors.
     */
    fun getEnumFields(psiClass: PsiClass): List<FieldInfo> {
        val constructors = psiClass.constructors

        return if (constructors.isNotEmpty()) {
            // Use constructor parameters as source of truth
            val primaryConstructor = constructors.first()
            primaryConstructor.parameterList.parameters.map { param ->
                FieldInfo(name = param.name!!, type = param.type)
            }
        } else {
            // Fallback to explicit fields (excluding constants)
            psiClass.fields
                .filterNot { it is PsiEnumConstant }
                .filterNot { it.hasModifierProperty(PsiModifier.STATIC) }
                .map { field -> FieldInfo(name = field.name, type = field.type) }
        }
    }

    /**
     * Generate enum constant initialization.
     */
    fun generateEnumConstant(constant: PsiEnumConstant): String {
        val name = constant.name
        val args = constant.argumentList?.expressions?.joinToString(", ") {
            it.text
        } ?: ""

        return if (args.isNotEmpty()) {
            "$name($args)"
        } else {
            name
        }
    }

    data class FieldInfo(val name: String, val type: PsiType)
}
```

**Key Enum Challenges:**
1. **Enum Constants**: Use `PsiEnumConstant` type, not manual checks
2. **Fields**: May only exist in constructor parameters, not explicit declarations
3. **Superclass**: Filter out `java.lang.Enum` (like `java.lang.Object`)
4. **Imports**: Collect from constructor parameters, not just fields
5. **Initialization**: Preserve enum constant arguments

---

## UI Components

### 1. Dialogs

Dialogs are the primary way to get user input.

**Example Configuration Dialog**:

```kotlin
package com.yourcompany.yourplugin.ui

import com.intellij.ide.util.PackageChooserDialog
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent

/**
 * Configuration dialog using IntelliJ's Kotlin UI DSL.
 */
class YourConfigDialog(
    private val project: Project,
    private val detectedPackage: String? = null
) : DialogWrapper(project) {

    // Configuration fields
    private var sourcePackage: String = detectedPackage ?: ""
    private var targetPackage: String = ""
    private lateinit var targetPackageField: TextFieldWithBrowseButton

    private var useFeature: Boolean = false
    private var generateDocs: Boolean = true
    private var generationMode: String = "all"

    init {
        title = "Configure Your Plugin"

        // IMPORTANT: Only do fast operations here (no file I/O, no PSI scanning)
        // Use simple heuristics for suggestions
        if (sourcePackage.isNotEmpty()) {
            targetPackage = getSuggestedPackage(sourcePackage)
        }

        init()
    }

    /**
     * Fast string-based heuristic (no slow operations on EDT).
     */
    private fun getSuggestedPackage(sourcePackage: String): String {
        return when {
            sourcePackage.contains("library") -> sourcePackage.replace("library", "domain")
            sourcePackage.contains("external") -> sourcePackage.replace("external", "domain")
            else -> {
                val basePkg = sourcePackage.substringBeforeLast(".", sourcePackage)
                if (basePkg.isNotEmpty()) "$basePkg.domain" else "domain"
            }
        }
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            group("Package Configuration") {
                row("Source Package:") {
                    textField()
                        .bindText(::sourcePackage.toMutableProperty())
                        .columns(40)
                        .validationOnInput {
                            when {
                                it.text.isBlank() -> error("Package cannot be empty")
                                !isValidPackageName(it.text) -> error("Invalid package format")
                                else -> null
                            }
                        }
                }

                row("Target Package:") {
                    // Create browse field with package chooser
                    targetPackageField = TextFieldWithBrowseButton().apply {
                        text = targetPackage
                        addActionListener {
                            val dialog = PackageChooserDialog(
                                "Select Target Package",
                                project
                            )
                            dialog.show()
                            dialog.selectedPackage?.qualifiedName?.let { selected ->
                                textField.text = selected
                                targetPackage = selected
                            }
                        }
                    }
                    cell(targetPackageField)
                        .validationOnInput {
                            val text = targetPackageField.text.trim()
                            when {
                                text.isBlank() -> error("Target package cannot be empty")
                                !isValidPackageName(text) -> error("Invalid package format")
                                text == sourcePackage -> error("Source and target must differ")
                                else -> null
                            }
                        }
                        .align(AlignX.FILL)
                }
            }

            buttonsGroup("Generation Scope") {
                row {
                    radioButton("Generate all classes", "all")
                    radioButton("Single class only", "single")
                }.bind(::generationMode.toMutableProperty())
            }

            group("Options") {
                row {
                    checkBox("Use feature X")
                        .bindSelected(::useFeature.toMutableProperty())
                    checkBox("Generate documentation")
                        .bindSelected(::generateDocs.toMutableProperty())
                }
            }
        }
    }

    /**
     * Validate package name format.
     */
    private fun isValidPackageName(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        val segments = packageName.split(".")
        return segments.all { segment ->
            segment.isNotEmpty() &&
            segment[0].isJavaIdentifierStart() &&
            segment.all { it.isJavaIdentifierPart() }
        }
    }

    fun getSourcePackage(): String = sourcePackage
    fun getTargetPackage(): String = targetPackage.trim()
    fun isFeatureEnabled(): Boolean = useFeature
    fun shouldGenerateDocs(): Boolean = generateDocs
}

// Usage:
fun showDialog(project: Project, detectedPackage: String?) {
    val dialog = YourConfigDialog(project, detectedPackage)
    if (dialog.showAndGet()) {
        val source = dialog.getSourcePackage()
        val target = dialog.getTargetPackage()
        // Use the values
    }
}
```

**Key Dialog Features:**
1. **Real-time Validation**: Use `.validationOnInput` for immediate feedback
2. **Package Chooser**: Use `PackageChooserDialog` for native package selection
3. **Fast Initialization**: Only use simple heuristics in `init` block (no file I/O)
4. **Property Binding**: Use `.bindText()` and `.bindSelected()` for automatic sync
5. **Validation**: Validate package names, prevent duplicates, ensure format

### 2. Progress Indicators

Show progress for long-running operations.

```kotlin
package com.yourcompany.yourplugin.progress

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

object ProgressHelper {

    /**
     * Run a task with a progress indicator.
     */
    fun runWithProgress(
        project: Project,
        title: String,
        canBeCancelled: Boolean = true,
        task: (ProgressIndicator) -> Unit
    ) {
        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, title, canBeCancelled) {
                override fun run(indicator: ProgressIndicator) {
                    task(indicator)
                }
            }
        )
    }

    /**
     * Run a modal task that blocks the UI.
     */
    fun runModalTask(
        project: Project,
        title: String,
        task: (ProgressIndicator) -> Unit
    ) {
        ProgressManager.getInstance().run(
            object : Task.Modal(project, title, true) {
                override fun run(indicator: ProgressIndicator) {
                    task(indicator)
                }
            }
        )
    }
}

// Usage:
ProgressHelper.runWithProgress(project, "Processing...") { indicator ->
    indicator.isIndeterminate = false

    for (i in 0..100) {
        if (indicator.isCanceled) break

        indicator.fraction = i / 100.0
        indicator.text = "Processing item $i"

        // Do work
        Thread.sleep(50)
    }
}
```

### 3. Notifications

Show notifications to users.

```kotlin
package com.yourcompany.yourplugin.notifications

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object NotificationHelper {

    private const val NOTIFICATION_GROUP = "YourPlugin.Notifications"

    fun showInfo(project: Project, message: String) {
        show(project, message, NotificationType.INFORMATION)
    }

    fun showWarning(project: Project, message: String) {
        show(project, message, NotificationType.WARNING)
    }

    fun showError(project: Project, message: String) {
        show(project, message, NotificationType.ERROR)
    }

    private fun show(
        project: Project,
        message: String,
        type: NotificationType
    ) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP)
            .createNotification(message, type)
            .notify(project)
    }
}
```

**Register notification group in plugin.xml**:

```xml
<extensions defaultExtensionNs="com.intellij">
    <notificationGroup id="YourPlugin.Notifications"
                       displayType="BALLOON"/>
</extensions>
```

---

## Threading and Performance

**CRITICAL**: IntelliJ has strict threading requirements. Violations will cause exceptions.

### Threading Rules

1. **EDT (Event Dispatch Thread)**:
   - UI operations
   - PSI modifications (must also be in write action)
   - Should be FAST (< 100ms)

2. **BGT (Background Thread)**:
   - Heavy computations
   - File I/O
   - Network operations
   - PSI reading (read-only)

3. **Write Actions**:
   - REQUIRED for all PSI/VFS modifications
   - Must be on EDT
   - Blocks all other operations

### Threading Examples

```kotlin
package com.yourcompany.yourplugin.threading

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project

object ThreadingHelper {

    /**
     * Run on EDT (Event Dispatch Thread).
     */
    fun runOnEdt(task: () -> Unit) {
        ApplicationManager.getApplication().invokeLater {
            task()
        }
    }

    /**
     * Run on background thread.
     */
    fun runOnBackground(task: () -> Unit) {
        ApplicationManager.getApplication().executeOnPooledThread {
            task()
        }
    }

    /**
     * Read PSI safely (read lock).
     */
    fun <T> readPsi(task: () -> T): T {
        return runReadAction(task)
    }

    /**
     * Modify PSI safely (write lock + EDT).
     */
    fun modifyPsi(project: Project, task: () -> Unit) {
        WriteCommandAction.runWriteCommandAction(project) {
            task()
        }
    }

    /**
     * Run a task that needs both read and write access.
     */
    fun <T> runWithReadWriteAccess(
        project: Project,
        reader: () -> T,
        writer: (T) -> Unit
    ) {
        // Read on background thread
        val data = runReadAction(reader)

        // Write on EDT
        invokeLater {
            WriteCommandAction.runWriteCommandAction(project) {
                writer(data)
            }
        }
    }
}
```

**Performance Tips**:
- Cache expensive computations
- Use `@Cached` annotation for caching
- Minimize PSI traversal
- Use indexes when possible
- Profile with IntelliJ Profiler

---

## Dependency Collection and Code Generation

### Collecting Class Dependencies

**CRITICAL**: When generating domain classes, you must collect ALL dependencies to ensure complete isolation from library packages.

```kotlin
package com.yourcompany.yourplugin.dependencies

import com.intellij.psi.*

object DependencyCollector {

    /**
     * Collect all dependencies for a set of classes.
     * Includes superclasses and field type dependencies.
     *
     * @param initialClasses Classes to start from
     * @param sourcePackage Package to scan for dependencies
     * @param maxDepth Maximum recursion depth to prevent infinite loops
     * @return All classes that need to be generated
     */
    fun collectAllDependencies(
        initialClasses: List<PsiClass>,
        sourcePackage: String,
        maxDepth: Int = 10
    ): List<PsiClass> {
        val allClasses = mutableSetOf<PsiClass>()
        val processed = mutableSetOf<PsiClass>()
        val toProcess = mutableListOf<PsiClass>()

        toProcess.addAll(initialClasses)
        var depth = 0

        while (toProcess.isNotEmpty() && depth < maxDepth) {
            val current = toProcess.removeAt(0)

            // Skip if already processed
            if (processed.contains(current)) continue

            processed.add(current)
            allClasses.add(current)

            // Collect superclass dependencies
            val superClass = getNonObjectSuperclass(current)
            if (superClass != null && belongsToPackage(superClass, sourcePackage)) {
                if (!processed.contains(superClass) && !toProcess.contains(superClass)) {
                    toProcess.add(superClass)
                }
            }

            // Collect field type dependencies
            val fieldDependencies = getFieldTypeDependencies(current, sourcePackage)
            fieldDependencies.forEach { dependency ->
                if (!processed.contains(dependency) && !toProcess.contains(dependency)) {
                    toProcess.add(dependency)
                }
            }

            depth++
        }

        // Sort by dependency order (superclasses first)
        return sortByDependencyOrder(allClasses.toList(), initialClasses)
    }

    /**
     * Get superclass (excluding Object and Enum).
     */
    private fun getNonObjectSuperclass(psiClass: PsiClass): PsiClass? {
        val superClass = psiClass.superClass ?: return null
        val superFqn = superClass.qualifiedName ?: return null

        return if (superFqn != "java.lang.Object" && superFqn != "java.lang.Enum") {
            superClass
        } else {
            null
        }
    }

    /**
     * Get all classes referenced in field types.
     */
    private fun getFieldTypeDependencies(
        psiClass: PsiClass,
        sourcePackage: String
    ): List<PsiClass> {
        val dependencies = mutableListOf<PsiClass>()
        val fields = psiClass.fields.filterNot {
            it.hasModifierProperty(PsiModifier.STATIC)
        }

        fields.forEach { field ->
            collectTypeClasses(field.type, sourcePackage, dependencies)
        }

        // Also check constructor parameters for enums
        if (psiClass.isEnum) {
            psiClass.constructors.forEach { constructor ->
                constructor.parameterList.parameters.forEach { param ->
                    collectTypeClasses(param.type, sourcePackage, dependencies)
                }
            }
        }

        return dependencies
    }

    /**
     * Recursively collect classes from a type (handles generics and arrays).
     */
    private fun collectTypeClasses(
        type: PsiType,
        sourcePackage: String,
        dependencies: MutableList<PsiClass>
    ) {
        when (type) {
            is PsiClassType -> {
                val resolvedClass = type.resolve()
                if (resolvedClass != null && belongsToPackage(resolvedClass, sourcePackage)) {
                    if (!dependencies.contains(resolvedClass)) {
                        dependencies.add(resolvedClass)
                    }
                }
                // Recursively handle generic type parameters
                type.parameters.forEach { paramType ->
                    collectTypeClasses(paramType, sourcePackage, dependencies)
                }
            }
            is PsiArrayType -> {
                collectTypeClasses(type.componentType, sourcePackage, dependencies)
            }
        }
    }

    /**
     * Check if a class belongs to a package.
     */
    private fun belongsToPackage(psiClass: PsiClass, packageName: String): Boolean {
        val classPackage = (psiClass.containingFile as? PsiJavaFile)?.packageName
        return classPackage?.startsWith(packageName) == true
    }

    /**
     * Sort classes by dependency order (superclasses before subclasses).
     */
    private fun sortByDependencyOrder(
        classes: List<PsiClass>,
        originalOrder: List<PsiClass>
    ): List<PsiClass> {
        val sorted = mutableListOf<PsiClass>()
        val remaining = classes.toMutableSet()

        // Helper to check if all dependencies are already sorted
        fun canAdd(psiClass: PsiClass): Boolean {
            val superClass = getNonObjectSuperclass(psiClass)
            return superClass == null ||
                   !remaining.contains(superClass) ||
                   sorted.contains(superClass)
        }

        // Keep adding classes whose dependencies are satisfied
        while (remaining.isNotEmpty()) {
            val toAdd = remaining.filter { canAdd(it) }

            if (toAdd.isEmpty()) {
                // Circular dependency - just add remaining in original order
                sorted.addAll(remaining.sortedBy { originalOrder.indexOf(it) })
                break
            }

            // Add in original order preference
            toAdd.sortedBy { originalOrder.indexOf(it) }.forEach { psiClass ->
                sorted.add(psiClass)
                remaining.remove(psiClass)
            }
        }

        return sorted
    }
}
```

**Key Dependency Collection Points:**
1. **Breadth-First Search**: Process dependencies level by level
2. **Depth Limiting**: Prevent infinite loops with `maxDepth` parameter
3. **Superclass Dependencies**: Always include superclasses from source package
4. **Field Type Dependencies**: Recursively collect classes from field types
5. **Generic Types**: Handle `List<CustomClass>`, `Map<K, V>`, etc.
6. **Dependency Ordering**: Generate superclasses before subclasses
7. **Package Filtering**: Only collect dependencies from source package

### Generating Import Statements

```kotlin
package com.yourcompany.yourplugin.codegen

import com.intellij.psi.*

object ImportCollector {

    /**
     * Collect all necessary imports for a class.
     */
    fun collectImports(
        sourceClass: PsiClass,
        targetPackage: String,
        classesBeingGenerated: Set<PsiClass>,
        additionalImports: List<String> = emptyList()
    ): Set<String> {
        val imports = mutableSetOf<String>()

        // Add additional imports (e.g., Lombok annotations)
        imports.addAll(additionalImports)

        // Collect from fields
        sourceClass.fields
            .filterNot { it.hasModifierProperty(PsiModifier.STATIC) }
            .forEach { field ->
                collectTypeImports(field.type, targetPackage, classesBeingGenerated, imports)
            }

        // Collect from constructor parameters (for enums)
        if (sourceClass.isEnum) {
            sourceClass.constructors.forEach { constructor ->
                constructor.parameterList.parameters.forEach { param ->
                    collectTypeImports(param.type, targetPackage, classesBeingGenerated, imports)
                }
            }
        }

        // Collect from superclass
        val superClass = sourceClass.superClass
        if (superClass != null) {
            val superFqn = superClass.qualifiedName
            if (superFqn != null &&
                superFqn != "java.lang.Object" &&
                superFqn != "java.lang.Enum") {

                if (classesBeingGenerated.contains(superClass)) {
                    // Superclass is being generated - use target package
                    imports.add("import $targetPackage.${superClass.name};")
                } else {
                    imports.add("import $superFqn;")
                }
            }
        }

        return imports.sorted().toSet()
    }

    /**
     * Recursively collect imports from a type.
     */
    private fun collectTypeImports(
        type: PsiType,
        targetPackage: String,
        classesBeingGenerated: Set<PsiClass>,
        imports: MutableSet<String>
    ) {
        when (type) {
            is PsiClassType -> {
                val resolvedClass = type.resolve()
                if (resolvedClass != null) {
                    val fqn = resolvedClass.qualifiedName
                    if (fqn != null && !fqn.startsWith("java.lang.")) {
                        // Check if this class is being generated in our batch
                        if (classesBeingGenerated.contains(resolvedClass)) {
                            imports.add("import $targetPackage.${resolvedClass.name};")
                        } else {
                            imports.add("import $fqn;")
                        }
                    }
                }

                // Recursively handle generic parameters
                type.parameters.forEach { paramType ->
                    collectTypeImports(paramType, targetPackage, classesBeingGenerated, imports)
                }
            }
            is PsiArrayType -> {
                collectTypeImports(type.componentType, targetPackage, classesBeingGenerated, imports)
            }
        }
    }
}
```

**Import Collection Best Practices:**
1. **Filter `java.lang`**: Skip imports for classes in `java.lang` package
2. **Generated Classes**: Use target package for classes being generated together
3. **Recursive Collection**: Handle nested generic types properly
4. **Sort Imports**: Keep imports sorted alphabetically
5. **Lombok Annotations**: Don't forget annotation imports

---

## File Operations

### Creating Files and Directories

```kotlin
package com.yourcompany.yourplugin.files

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*

object FileOperations {

    /**
     * Create a directory.
     */
    fun createDirectory(
        parent: PsiDirectory,
        name: String
    ): PsiDirectory? {
        return parent.findSubdirectory(name)
            ?: parent.createSubdirectory(name)
    }

    /**
     * Find or create a directory by package name.
     */
    fun findOrCreatePackageDirectory(
        project: Project,
        sourceRoot: VirtualFile,
        packageName: String
    ): PsiDirectory? {
        val psiManager = PsiManager.getInstance(project)
        var current = psiManager.findDirectory(sourceRoot) ?: return null

        packageName.split(".").forEach { segment ->
            current = current.findSubdirectory(segment)
                ?: current.createSubdirectory(segment)
        }

        return current
    }

    /**
     * Create a Java file from text.
     */
    fun createJavaFile(
        project: Project,
        directory: PsiDirectory,
        fileName: String,
        content: String
    ): PsiFile? {
        return runWriteAction {
            PsiFileFactory.getInstance(project)
                .createFileFromText(
                    fileName,
                    JavaFileType.INSTANCE,
                    content
                ).let { file ->
                    directory.add(file) as PsiFile
                }
        }
    }

    /**
     * Navigate to a file and open it in editor.
     */
    fun navigateToFile(
        project: Project,
        file: PsiFile
    ) {
        file.virtualFile?.let { virtualFile ->
            com.intellij.openapi.fileEditor.FileEditorManager
                .getInstance(project)
                .openFile(virtualFile, true)
        }
    }

    /**
     * Format a file according to code style.
     */
    fun formatFile(project: Project, file: PsiFile) {
        runWriteAction {
            com.intellij.psi.codeStyle.CodeStyleManager
                .getInstance(project)
                .reformat(file)
        }
    }
}
```

---

## Testing Strategy

### 1. Unit Tests

Test your business logic without the IntelliJ platform.

```kotlin
package com.yourcompany.yourplugin

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class YourBusinessLogicTest {

    @Test
    fun `test your logic`() {
        val result = YourClass.doSomething("input")
        assertEquals("expected", result)
    }
}
```

### 2. Integration Tests

Test with the IntelliJ platform.

```kotlin
package com.yourcompany.yourplugin

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.psi.PsiClass

class YourPluginTest : BasePlatformTestCase() {

    fun `test action on Java class`() {
        // Create a test Java file
        val file = myFixture.addFileToProject(
            "src/test/java/TestClass.java",
            """
            package test;

            public class TestClass {
                private String field;

                public String getField() {
                    return field;
                }
            }
            """.trimIndent()
        )

        // Open the file
        myFixture.configureFromExistingVirtualFile(file.virtualFile)

        // Get the PsiClass
        val psiClass = myFixture.findClass("test.TestClass")
        assertNotNull(psiClass)

        // Test your logic
        val fields = psiClass.fields
        assertEquals(1, fields.size)
        assertEquals("field", fields[0].name)
    }
}
```

---

## Build and Deployment

### Essential Gradle Commands

```bash
# Build the plugin
./gradlew build

# Run tests
./gradlew test

# Launch IDE with plugin installed
./gradlew runIde

# Build plugin ZIP for distribution
./gradlew buildPlugin

# Validate plugin structure
./gradlew verifyPlugin

# Publish to JetBrains Marketplace (requires token)
./gradlew publishPlugin
```

### Publishing to JetBrains Marketplace

1. **Create account** at https://plugins.jetbrains.com/
2. **Generate token** in your account settings
3. **Add token to gradle.properties**:
   ```properties
   intellijPublishToken=your-token-here
   ```
4. **Configure publishing in build.gradle.kts**:
   ```kotlin
   intellijPlatform {
       publishing {
           token = providers.gradleProperty("intellijPublishToken")
           channels = listOf("default")  // or "beta", "alpha"
       }
   }
   ```
5. **Publish**: `./gradlew publishPlugin`

---

## Documentation Maintenance

### CRITICAL: CLAUDE.md Management

**The CLAUDE.md file is your plugin's living documentation for AI assistants.**

#### When Creating This Plugin

**IMMEDIATELY** add the following to `CLAUDE.md`:

```markdown
## Plugin Components

### Actions
- **ActionName** (`path/to/Action.kt`): Description of what it does
- Keyboard shortcut: Ctrl+Shift+X
- Available in: Editor context menu, Project view

### Services
- **ServiceName** (`path/to/Service.kt`): Description and purpose
  - Application-level / Project-level
  - Key methods: method1(), method2()

### PSI Utilities
- **PsiHelper** (`path/to/PsiHelper.kt`): PSI manipulation utilities
  - getClassAtCaret()
  - getFields()
  - etc.

### Type System
- **TypeHandler** (`path/to/TypeHandler.kt`): Handles type resolution
- **TypeRegistry** (`path/to/TypeRegistry.kt`): Central type registry

### UI Components
- **YourDialog** (`path/to/YourDialog.kt`): Configuration dialog
  - Fields: field1, field2
  - Returns: ConfigObject

## Architecture Decisions

### Why We Chose X Over Y
- Reason 1
- Reason 2

### Threading Strategy
- Actions run on: BGT
- PSI modifications: EDT + Write Action
- Heavy computations: Background threads

## Known Issues
- Issue 1: Description and workaround
- Issue 2: Description and status

## Performance Considerations
- Cache X because Y
- Avoid Z in hot paths
```

#### Maintenance Schedule

**MUST REVIEW CLAUDE.md**:
1. ✅ After adding/removing a component
2. ✅ After major refactoring
3. ✅ When changing architecture
4. ✅ After fixing a tricky bug (document the solution)
5. ✅ Before releases
6. ✅ Monthly (even if no changes)

#### What to Remove from CLAUDE.md

- ❌ Deleted files/classes
- ❌ Deprecated approaches
- ❌ Fixed issues
- ❌ Outdated package structures
- ❌ Old architecture diagrams

#### What to Keep Updated

- ✅ Current package structure
- ✅ Key components and their purposes
- ✅ Build commands
- ✅ Architecture decisions
- ✅ Active known issues
- ✅ Development workflow

**AUTOMATION TIP**: Add a Git pre-commit hook to remind about CLAUDE.md:

```bash
#!/bin/bash
# .git/hooks/pre-commit

changed_files=$(git diff --cached --name-only)

if echo "$changed_files" | grep -qE "\.kt$|\.java$"; then
    if ! echo "$changed_files" | grep -q "CLAUDE.md"; then
        echo "⚠️  Code changed but CLAUDE.md not updated."
        echo "   Consider updating CLAUDE.md if architecture changed."
        echo ""
        read -p "Continue anyway? (y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
fi
```

---

## Best Practices

### 1. Code Organization

```
src/main/kotlin/com/yourcompany/yourplugin/
├── actions/           # All AnAction implementations
├── services/          # Application/Project services
├── ui/                # Dialogs, panels, UI components
├── psi/               # PSI utilities and helpers
├── types/             # Type system and handlers
├── models/            # Data classes and models
├── utils/             # Generic utilities
├── listeners/         # Event listeners
└── settings/          # Plugin settings
```

### 2. Naming Conventions

- **Actions**: `*Action.kt` (e.g., `GenerateCodeAction.kt`)
- **Services**: `*Service.kt` (e.g., `CacheService.kt`)
- **Dialogs**: `*Dialog.kt` (e.g., `ConfigDialog.kt`)
- **Handlers**: `*Handler.kt` (e.g., `TypeHandler.kt`)
- **Utilities**: `*Helper.kt` or `*Utils.kt`

### 3. Error Handling

```kotlin
package com.yourcompany.yourplugin.utils

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.notification.NotificationType

object ErrorHandler {

    private val LOG = Logger.getInstance(ErrorHandler::class.java)

    fun handle(
        project: Project,
        message: String,
        exception: Exception? = null
    ) {
        // Log the error
        if (exception != null) {
            LOG.error(message, exception)
        } else {
            LOG.warn(message)
        }

        // Show user notification
        NotificationHelper.showError(project, message)
    }

    fun <T> tryOrNull(block: () -> T): T? {
        return try {
            block()
        } catch (e: Exception) {
            LOG.warn("Operation failed", e)
            null
        }
    }
}
```

### 4. Logging

```kotlin
import com.intellij.openapi.diagnostic.Logger

class YourClass {
    companion object {
        private val LOG = Logger.getInstance(YourClass::class.java)
    }

    fun doSomething() {
        LOG.info("Starting operation")
        LOG.debug("Debug information")
        LOG.warn("Warning message")
        LOG.error("Error message", exception)
    }
}
```

### 5. Resource Management

```kotlin
// Always close resources
val stream = FileInputStream(file)
try {
    // Use stream
} finally {
    stream.close()
}

// Or use 'use' for auto-closing
FileInputStream(file).use { stream ->
    // Use stream
}
```

### 6. Nullability

```kotlin
// Prefer safe calls
val length = text?.length

// Use Elvis operator for defaults
val name = user?.name ?: "Unknown"

// Use !! only when absolutely sure
val definitelyNotNull = maybeNull!!  // Avoid if possible
```

### 7. Immutability

```kotlin
// Prefer val over var
val immutable = "cannot change"
var mutable = "can change"

// Prefer immutable collections
val list = listOf(1, 2, 3)  // Immutable
val mutableList = mutableListOf(1, 2, 3)  // Only when needed
```

---

## Common Pitfalls and Solutions

### ❌ Problem: Action disabled during indexing

**Solution**: Implement `DumbAware`:
```kotlin
class YourAction : AnAction(), DumbAware {
    // ...
}
```

### ❌ Problem: "Read access is allowed from inside read-action"

**Solution**: Wrap PSI reading in read action:
```kotlin
runReadAction {
    val fields = psiClass.fields
}
```

### ❌ Problem: "Write access is allowed from inside write-action only"

**Solution**: Wrap PSI modifications in write action:
```kotlin
WriteCommandAction.runWriteCommandAction(project) {
    psiClass.add(newMethod)
}
```

### ❌ Problem: UI freezing during long operation

**Solution**: Use background task:
```kotlin
ProgressHelper.runWithProgress(project, "Processing...") { indicator ->
    // Long operation here
}
```

### ❌ Problem: Plugin not loading

**Solution**: Check:
1. `plugin.xml` is in `src/main/resources/META-INF/`
2. Plugin ID is unique
3. Dependencies are declared
4. Build was successful

### ❌ Problem: Can't find PsiClass

**Solution**: Ensure `com.intellij.java` dependency:
```xml
<depends>com.intellij.java</depends>
```

---

## Checklist for New Plugin

- [ ] Created project structure
- [ ] Configured `build.gradle.kts`
- [ ] Created `plugin.xml` with all required fields
- [ ] Implemented at least one Action
- [ ] Added keyboard shortcut
- [ ] Implemented proper threading (BGT/EDT)
- [ ] Added error handling
- [ ] Created tests
- [ ] Added logging
- [ ] Documented in CLAUDE.md
- [ ] Verified plugin with `./gradlew verifyPlugin`
- [ ] Tested in sandbox IDE with `./gradlew runIde`
- [ ] Built plugin ZIP with `./gradlew buildPlugin`

---

## Resources

### Official Documentation
- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [PSI Reference](https://plugins.jetbrains.com/docs/intellij/psi.html)
- [Threading Reference](https://plugins.jetbrains.com/docs/intellij/general-threading-rules.html)
- [UI Guidelines](https://plugins.jetbrains.com/docs/intellij/user-interface-components.html)

### Example Plugins
- [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- [IntelliJ Community Source](https://github.com/JetBrains/intellij-community)

### Tools
- [IntelliJ Platform Explorer](https://plugins.jetbrains.com/intellij-platform-explorer)
- [PSI Viewer](https://plugins.jetbrains.com/plugin/227-psiviewer)

---

## Final Notes

**Remember**:
1. Threading is CRITICAL - violations will crash
2. PSI modifications MUST be in write actions on EDT
3. Keep CLAUDE.md updated - it's your future self's best friend
4. Test in sandbox IDE before publishing
5. Profile for performance - users expect fast plugins
6. Handle errors gracefully - show helpful messages
7. Follow IntelliJ's UI guidelines for consistency

**When in doubt**:
- Check IntelliJ Platform SDK docs
- Look at existing plugins' source code
- Use PSI Viewer to understand PSI structure
- Test thoroughly in sandbox IDE

---

**Good luck building your plugin!** 🚀
