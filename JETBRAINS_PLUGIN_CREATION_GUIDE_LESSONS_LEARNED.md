# Lessons Learned from Domain Class Generator Plugin Development

This document captures real-world mistakes, solutions, and best practices discovered during the development of the Domain Class Generator plugin. Use these lessons to avoid common pitfalls in your own plugin development.

---

## Table of Contents
- [Threading and EDT Violations](#threading-and-edt-violations)
- [UI/UX Issues](#uiux-issues)
- [Code Generation Challenges](#code-generation-challenges)
- [PSI and Type Resolution](#psi-and-type-resolution)
- [Dependency Management](#dependency-management)
- [Plugin Metadata and Documentation](#plugin-metadata-and-documentation)

---

## Threading and EDT Violations

### ❌ CRITICAL MISTAKE: Slow Package Scanning on EDT

**Problem:** During dialog initialization, we called `PackageHelper.getAllPackages(project)` which recursively scanned all source directories.

```kotlin
// BAD - Causes "Slow operations are prohibited on EDT" error
init {
    val suggestedPackages = if (sourcePackage.isNotEmpty()) {
        PackageHelper.getSuggestedTargetPackages(project, sourcePackage)  // SLOW!
    } else {
        emptyList()
    }
    targetPackage = suggestedPackages.first()
}
```

**Error Message:**
```
java.lang.Throwable: Slow operations are prohibited on EDT.
    at com.intellij.util.SlowOperations.assertSlowOperationsAreAllowed
    at com.intellij.workspaceModel.core.fileIndex.impl.WorkspaceFileIndexDataImpl.ensureIsUpToDate
    ...
```

**Why It Failed:**
- Dialog `init` blocks run on EDT (UI thread)
- Package scanning involves file I/O and PSI operations
- IntelliJ detects slow operations (>100ms) and throws exceptions

✅ **SOLUTION: Use Fast String Heuristics**

```kotlin
// GOOD - Fast, no file I/O
init {
    if (sourcePackage.isNotEmpty()) {
        targetPackage = getSuggestedTargetPackage(sourcePackage)
    }
}

private fun getSuggestedTargetPackage(sourcePackage: String): String {
    return when {
        sourcePackage.contains("library") -> sourcePackage.replace("library", "domain")
        sourcePackage.contains("external") -> sourcePackage.replace("external", "domain")
        else -> {
            val basePkg = sourcePackage.substringBeforeLast(".", sourcePackage)
            if (basePkg.isNotEmpty()) "$basePkg.domain" else "domain"
        }
    }
}
```

**Lesson:** Always profile operations during UI initialization. If it involves file I/O, PSI scanning, or indexing, move it to a background task or use simple heuristics.

---

## UI/UX Issues

### ❌ MISTAKE: Validation Doesn't Re-Enable OK Button

**Problem:** After fixing validation errors, the OK button remained disabled.

```kotlin
// BAD - Only validates on OK button click
override fun doValidate(): ValidationInfo? {
    targetPackage = targetPackageField.text.trim()
    if (targetPackage.isBlank()) {
        return ValidationInfo("Target package cannot be empty")
    }
    return null
}
```

**User Experience:**
1. User leaves field empty → Error appears, OK disabled ✓
2. User types valid package → OK button stays disabled ✗
3. User confused, tries clicking OK anyway → Nothing happens ✗

✅ **SOLUTION: Use `validationOnInput`**

```kotlin
// GOOD - Real-time validation
row("Target Package:") {
    cell(targetPackageField)
        .validationOnInput {
            val text = targetPackageField.text.trim()
            when {
                text.isBlank() -> error("Target package cannot be empty")
                !PackageHelper.isValidPackageName(text) -> error("Invalid package format")
                text == sourcePackage -> error("Source and target must be different")
                else -> null
            }
        }
}

// Also sync the backing variable
targetPackageField.textField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
    override fun insertUpdate(e: javax.swing.event.DocumentEvent?) {
        targetPackage = textField.text.trim()
    }
    override fun removeUpdate(e: javax.swing.event.DocumentEvent?) {
        targetPackage = textField.text.trim()
    }
    override fun changedUpdate(e: javax.swing.event.DocumentEvent?) {
        targetPackage = textField.text.trim()
    }
})
```

**Lesson:** Always use `.validationOnInput()` for real-time feedback. The OK button will automatically enable/disable based on validation results.

### ❌ MISTAKE: Verbose UI with Too Many Comments

**Problem:** Every field had lengthy explanatory comments, making the dialog cluttered.

```kotlin
// BAD - Takes up too much vertical space
row("Source Package:") {
    textField()
        .comment("Package to scan for library classes (auto-detected from current file)")
        .comment("Use lowercase letters and dots (e.g., com.example.models)")
}
row {
    comment("""
        <b>Tip:</b> Click the folder icon to browse packages, or type a package name.
        The generator will scan all classes in the source package.
    """.trimIndent())
}
```

✅ **SOLUTION: Concise, Tooltip-Like Comments Only When Needed**

```kotlin
// GOOD - Clean and compact
row("Source Package:") {
    textField()
        .bindText(::sourcePackage.toMutableProperty())
        .columns(40)
}
```

**Lesson:** UI should be self-explanatory. Only add comments for complex/non-obvious fields. Users prefer compact UIs.

### ✅ GOOD PRACTICE: Compact Layouts with Multiple Controls Per Row

```kotlin
// GOOD - Lombok annotations in 3 rows instead of 8
group("Lombok Annotations") {
    row {
        checkBox("@Data").bindSelected(::useData.toMutableProperty())
        checkBox("@Builder").bindSelected(::useBuilder.toMutableProperty())
        checkBox("@Getter").bindSelected(::useGetter.toMutableProperty())
        checkBox("@Setter").bindSelected(::useSetter.toMutableProperty())
    }
    row {
        checkBox("@NoArgsConstructor").bindSelected(::useNoArgsConstructor.toMutableProperty())
        checkBox("@AllArgsConstructor").bindSelected(::useAllArgsConstructor.toMutableProperty())
    }
    row {
        checkBox("@ToString").bindSelected(::useToString.toMutableProperty())
        checkBox("@EqualsAndHashCode").bindSelected(::useEqualsAndHashCode.toMutableProperty())
    }
}
```

**Lesson:** Group related controls on the same row to save vertical space and improve scannability.

---

## Code Generation Challenges

### ❌ MISTAKE: Generating `java.lang.Enum` as a Class

**Problem:** Enums that extended `java.lang.Enum` were generated as regular classes.

```kotlin
// BAD - Only filters Object
fun getNonObjectSuperclass(psiClass: PsiClass): PsiClass? {
    val superClass = psiClass.superClass ?: return null
    val superFqn = superClass.qualifiedName ?: return null
    return if (superFqn != "java.lang.Object") superClass else null
}
```

**Result:**
```java
// Generated (WRONG)
public class SomeEnum extends Enum {
    // ...
}

public class Enum {  // This shouldn't exist!
    private String name;
    private int ordinal;
}
```

✅ **SOLUTION: Filter Both Object and Enum**

```kotlin
// GOOD - Filter out both
fun getNonObjectSuperclass(psiClass: PsiClass): PsiClass? {
    val superClass = psiClass.superClass ?: return null
    val superFqn = superClass.qualifiedName ?: return null
    return if (superFqn != "java.lang.Object" && superFqn != "java.lang.Enum") {
        superClass
    } else {
        null
    }
}
```

**Lesson:** Always think about Java's built-in classes (`Object`, `Enum`, `Record`) when dealing with inheritance. They should typically be filtered out.

### ❌ MISTAKE: Missing Enum Fields from Constructor Parameters

**Problem:** Enums with constructor parameters were missing field declarations.

```kotlin
// BAD - Only generates existing fields
val fields = PsiHelper.getNonStaticFields(sourceClass)
fields.forEach { field ->
    appendLine("    private ${getSimpleTypeName(field.type)} ${field.name};")
}
```

**Source Enum:**
```java
public enum Status {
    ACTIVE("Active", 1),
    INACTIVE("Inactive", 0);

    private String displayName;
    // Missing: private int code;

    Status(String displayName, int code) {
        this.displayName = displayName;
        this.code = code;
    }
}
```

**Generated (WRONG):**
```java
public enum Status {
    ACTIVE("Active", 1),
    INACTIVE("Inactive", 0);

    private String displayName;
    // Missing: private int code;

    Status(String displayName, int code) {  // Constructor references missing field!
        this.displayName = displayName;
        this.code = code;
    }
}
```

✅ **SOLUTION: Generate Fields from Constructor Parameters**

```kotlin
// GOOD - Use constructor as source of truth
private data class FieldInfo(val name: String, val type: PsiType)

val fieldsToGenerate = if (constructors.isNotEmpty()) {
    // Use constructor parameters as source of truth
    val primaryConstructor = constructors.first()
    primaryConstructor.parameterList.parameters.map { param ->
        FieldInfo(name = param.name!!, type = param.type)
    }
} else {
    // Fallback to existing fields
    PsiHelper.getNonStaticFields(sourceClass).map { field ->
        FieldInfo(name = field.name, type = field.type)
    }
}
```

**Lesson:** For enums, constructor parameters are the source of truth for fields, not the explicitly declared fields. Some enum fields might only exist implicitly.

### ❌ MISTAKE: Missing Imports for Enum Constructor Parameters

**Problem:** Enum constructors used custom types, but imports weren't collected.

```kotlin
// BAD - Only collects field type imports
fields.forEach { field ->
    collectTypeImports(field.type, imports)
}
```

**Result:**
```java
// Missing: import java.math.BigDecimal;
public enum Status {
    ACTIVE(BigDecimal.ZERO);  // Compilation error!

    private BigDecimal value;

    Status(BigDecimal value) {
        this.value = value;
    }
}
```

✅ **SOLUTION: Collect Imports from Constructor Parameters**

```kotlin
// GOOD - Collect from constructors too
private fun collectEnumImports(
    sourceClass: PsiClass,
    constructors: List<PsiMethod>
): Set<String> {
    val imports = mutableSetOf<String>()

    // Lombok imports
    config.lombokAnnotations.getAnnotationFQNs().forEach { fqn ->
        imports.add("import $fqn;")
    }

    // Constructor parameter type imports
    constructors.forEach { constructor ->
        constructor.parameterList.parameters.forEach { param ->
            collectTypeImports(param.type, imports)
        }
    }

    return imports
}
```

**Lesson:** When generating code with constructors, always collect imports from constructor parameter types, not just from fields.

---

## PSI and Type Resolution

### ❌ MISTAKE: Not Handling Enum Constants Properly

**Problem:** Used wrong filter to detect enum constants.

```kotlin
// BAD - Filters by type presentation text
val enumConstants = sourceClass.fields.filter {
    it.hasModifierProperty(PsiModifier.STATIC) &&
    it.type.presentableText == className
}
```

✅ **SOLUTION: Use `PsiEnumConstant` Type**

```kotlin
// GOOD - Type-safe check
val enumConstants = sourceClass.fields.filter {
    it is PsiEnumConstant
}
```

**Lesson:** IntelliJ provides specialized PSI types (`PsiEnumConstant`, `PsiAnnotation`, etc.). Use them instead of manual checks.

### ✅ GOOD PRACTICE: Recursive Type Import Collection

```kotlin
private fun collectTypeImports(type: PsiType, imports: MutableSet<String>) {
    when (type) {
        is PsiClassType -> {
            val resolvedClass = type.resolve()
            if (resolvedClass != null) {
                val fqn = PsiHelper.getFqn(resolvedClass)
                if (fqn != null && !fqn.startsWith("java.lang.")) {
                    // Check if being generated in our batch
                    if (classesBeingGenerated.contains(resolvedClass)) {
                        imports.add("import ${config.targetPackage}.${resolvedClass.name};")
                    } else {
                        imports.add("import $fqn;")
                    }
                }
            }

            // Recursively handle generics
            type.parameters.forEach { paramType ->
                collectTypeImports(paramType, imports)
            }
        }
        is PsiArrayType -> {
            collectTypeImports(type.componentType, imports)
        }
    }
}
```

**Lesson:** Always handle nested types (generics, arrays) recursively. A type like `Map<String, List<CustomClass>>` requires deep traversal.

---

## Dependency Management

### ❌ MISTAKE: Single Class Mode Doesn't Generate Field Dependencies

**Problem:** Single class mode only generated the selected class, leaving references to library classes.

```kotlin
// BAD - No dependency collection for single class
val classesToGenerate = if (config.singleClassMode) {
    listOf(config.singleClass)
} else {
    scanPackage(config.sourcePackage)
}
// Stop here - returns only the single class
```

**Result:**
```java
package com.example.domain;

import com.library.Address;  // Still references library!

public class User {
    private Address address;  // Should reference domain.Address
    private String name;
}
```

✅ **SOLUTION: Always Collect Dependencies**

```kotlin
// GOOD - Collect dependencies for both modes
val initialClasses = if (config.singleClassMode) {
    listOf(config.singleClass)
} else {
    scanPackage(config.sourcePackage)
}

// Always collect dependencies (superclasses + field types)
val classesToGenerate = if (initialClasses.isNotEmpty()) {
    PsiHelper.collectAllDependencies(initialClasses)
} else {
    initialClasses
}
```

**Lesson:** Domain isolation requires generating ALL dependencies. If you're copying a class to avoid library coupling, you must also copy its dependencies.

### ✅ GOOD PRACTICE: Dependency Collection with Depth Limiting

```kotlin
fun collectAllDependencies(classes: List<PsiClass>, maxDepth: Int = 10): List<PsiClass> {
    val allClasses = mutableSetOf<PsiClass>()
    val processed = mutableSetOf<PsiClass>()
    val toProcess = mutableListOf<PsiClass>()

    toProcess.addAll(classes)
    var depth = 0

    while (toProcess.isNotEmpty() && depth < maxDepth) {
        val current = toProcess.removeAt(0)
        if (processed.contains(current)) continue

        processed.add(current)
        allClasses.add(current)

        // Collect superclasses
        val superclasses = getAllSuperclasses(current)
        superclasses.forEach { superClass ->
            if (!processed.contains(superClass) && !toProcess.contains(superClass)) {
                toProcess.add(superClass)
            }
        }

        // Collect field type dependencies
        val fieldDependencies = getFieldTypeDependencies(current)
        fieldDependencies.forEach { dependency ->
            if (!processed.contains(dependency) && !toProcess.contains(dependency)) {
                toProcess.add(dependency)
            }
        }

        depth++
    }

    return sortByDependencyOrder(allClasses.toList(), classes)
}
```

**Lesson:** Use breadth-first search with depth limiting to prevent infinite loops in circular dependencies.

---

## Plugin Metadata and Documentation

### ❌ MISTAKE: Hardcoded Plugin Version in Generated Code

**Problem:** JavaDoc contained hardcoded version string.

```kotlin
// BAD
appendLine(" * Generated by: Domain Class Generator v1.0-SNAPSHOT")
```

✅ **SOLUTION: Read from Plugin Descriptor**

```kotlin
// GOOD - Always accurate
val pluginId = "com.example.domaingenerator"
val plugin = com.intellij.ide.plugins.PluginManagerCore.getPlugin(
    com.intellij.openapi.extensions.PluginId.getId(pluginId)
)
val pluginName = plugin?.name ?: "Domain Class Generator"
val pluginVersion = plugin?.version ?: "1.0-SNAPSHOT"
val pluginVendor = plugin?.vendor ?: "YourCompany"

appendLine(" * Generated by: $pluginName v$pluginVersion")
appendLine(" * @author $pluginVendor")
```

**Lesson:** Never hardcode metadata that exists in `plugin.xml`. Always read it dynamically to stay in sync.

### ✅ GOOD PRACTICE: Include Shortcuts in Plugin Name

```xml
<!-- Makes shortcut discoverable in plugin list -->
<name>Domain Class Generator (Ctrl+Shift+E)</name>
```

**Lesson:** Users often don't read documentation. Putting the shortcut in the plugin name makes it immediately visible.

### ✅ GOOD PRACTICE: Problem Statement in Plugin Description

```xml
<description><![CDATA[
<h3>Why Use This Plugin?</h3>
<p>
  Using external library data objects directly in your domain layer is <b>unreliable and unsecured</b>:
</p>
<ul>
    <li><b>Version Changes:</b> External libraries may change class structures</li>
    <li><b>Security Risks:</b> Library classes may expose unnecessary fields</li>
    <li><b>Tight Coupling:</b> Direct dependency on external models is fragile</li>
</ul>
<p>
  <b>Solution:</b> Create your own copy of the class structure in your domain layer.
</p>
]]></description>
```

**Lesson:** Always explain WHY the plugin exists, not just WHAT it does. Users need to understand the problem before they'll use your solution.

---

## Architecture Patterns

### ✅ GOOD PRACTICE: Separation of Concerns

**Our Plugin Structure:**
```
src/main/kotlin/com/example/domaingenerator/
├── actions/              # User triggers (BGT)
│   └── GenerateDomainClassesAction.kt
├── services/             # Orchestration (Background tasks)
│   └── DomainGeneratorService.kt
├── generator/            # Core logic (Pure functions)
│   └── DomainClassGenerator.kt
├── ui/                   # User interaction (EDT)
│   └── GeneratorConfigDialog.kt
├── models/               # DTOs (Data only)
│   └── GeneratorConfig.kt
└── utils/                # Utilities (Stateless helpers)
    ├── PsiHelper.kt
    ├── PackageHelper.kt
    └── NotificationHelper.kt
```

**Why This Works:**
- **Actions**: Lightweight, just handle user input and delegate to services
- **Services**: Manage background tasks, progress, and orchestration
- **Generator**: Pure logic, easily testable, no IDE dependencies
- **UI**: Only concerned with user interaction, no business logic
- **Utils**: Reusable, stateless helpers

**Lesson:** Each layer has one responsibility. Actions don't generate code. Generators don't show dialogs. UI doesn't scan files.

### ✅ GOOD PRACTICE: DTO Pattern for Configuration

```kotlin
// Clean data transfer object
data class GeneratorConfig(
    val sourcePackage: String,
    val targetPackage: String,
    val lombokAnnotations: LombokAnnotations,
    val generateGettersSetters: Boolean = false,
    val followInheritance: Boolean = true,
    val generateJavaDocs: Boolean = true,
    val singleClassMode: Boolean = false,
    val singleClass: PsiClass? = null
)
```

**Why This Works:**
- Immutable (val)
- All configuration in one place
- Easy to pass between layers
- Easy to serialize/deserialize if needed
- Defaults make optional parameters clear

**Lesson:** Use data classes for configuration. They're concise, immutable, and come with free `copy()`, `equals()`, `hashCode()`.

---

## Testing and Debugging

### ✅ GOOD PRACTICE: Build Commands

Always test with:
```bash
# Clean build
./gradlew clean build

# Run in sandbox IDE
./gradlew runIde

# Verify plugin structure
./gradlew verifyPlugin

# Build distributable ZIP
./gradlew buildPlugin
```

**Lesson:** `runIde` is your best friend. Test every change in a real IDE instance before committing.

### ❌ MISTAKE: Not Testing Edge Cases

**Edge Cases We Found:**
1. Enums with no constructors
2. Enums with constructors but no explicit fields
3. Classes with circular dependencies
4. Generic types with multiple levels (`Map<String, List<Pair<K, V>>>`)
5. Empty packages
6. Same source and target package
7. Invalid package names

**Lesson:** Always test edge cases. Your users will find them even if you don't.

---

## Quick Reference: Common Issues and Solutions

| Issue | Solution |
|-------|----------|
| "Slow operations are prohibited on EDT" | Move to background thread or use simple heuristics |
| "Read access is allowed from read-action only" | Wrap in `runReadAction { }` |
| "Write access is allowed from write-action only" | Wrap in `WriteCommandAction.runWriteCommandAction { }` |
| Validation doesn't update | Use `.validationOnInput { }` instead of `doValidate()` |
| Missing imports | Collect imports from all types (fields, parameters, generics) |
| Generated class references library | Ensure `collectAllDependencies()` is called |
| Enum fields missing | Generate fields from constructor parameters |
| java.lang.Enum generated as class | Filter out `java.lang.Enum` from superclasses |

---

## Final Recommendations

### DO:
✅ Use simple string heuristics in UI initialization
✅ Implement real-time validation with `.validationOnInput`
✅ Collect dependencies recursively for domain isolation
✅ Generate enum fields from constructor parameters
✅ Read plugin metadata dynamically from descriptor
✅ Include problem statement in plugin description
✅ Test in sandbox IDE with `./gradlew runIde`
✅ Handle edge cases (empty inputs, circular deps, etc.)

### DON'T:
❌ Do slow operations (file I/O, PSI scanning) on EDT
❌ Rely only on `doValidate()` for form validation
❌ Hardcode plugin version or metadata
❌ Generate only the selected class without dependencies
❌ Forget to filter `java.lang.Enum` and `java.lang.Object`
❌ Assume enum fields exist as explicit declarations
❌ Skip constructor parameter imports for enums
❌ Use verbose comments that clutter the UI

---

## Conclusion

These lessons came from real development challenges. Every mistake listed here caused actual bugs or user confusion. Every solution has been tested and proven to work.

**Remember:** IntelliJ plugin development is challenging because:
1. Threading rules are strict and non-negotiable
2. PSI is complex and requires deep understanding
3. UI performance affects user experience dramatically
4. Edge cases are everywhere in code analysis

But with careful attention to these lessons, you can avoid the painful debugging sessions we went through.

**Happy plugin development!** 🚀

---

**Document Version:** 1.0
**Based On:** Domain Class Generator Plugin (v1.0-SNAPSHOT)
**Last Updated:** 2025
