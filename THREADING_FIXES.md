# Threading Fixes - Domain Class Generator

This document details all threading fixes applied to ensure proper PSI access in IntelliJ IDEA.

## Problem

IntelliJ Platform has strict threading requirements:
- **PSI reads** must be in a read action (`runReadAction`)
- **PSI writes** must be in a write action on EDT (`WriteCommandAction`)
- UI operations must be on EDT

Violating these rules causes: `RuntimeExceptionWithAttachments: Read access is allowed from inside read-action only`

## Solutions Applied

### 1. **GenerateDomainClassesAction.kt**

**Issue**: PSI access in `actionPerformed` without read actions

**Fix**:
```kotlin
override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val editor = e.getData(CommonDataKeys.EDITOR)
    val psiFile = e.getData(CommonDataKeys.PSI_FILE) as? PsiJavaFile ?: return

    // Read PSI data in a read action
    val contextData = runReadAction {
        // Detect class and package
        Pair(detectedClass, detectedPackage)
    }

    // Show dialog on EDT
    invokeLater {
        val dialog = GeneratorConfigDialog(...)
        // ...
    }
}
```

**Key Changes**:
- ✅ Wrapped PSI reads in `runReadAction`
- ✅ Ensured dialog is shown on EDT with `invokeLater`

---

### 2. **PackageHelper.kt**

**Issue**: PSI access when scanning packages without read actions

**Fix**:
```kotlin
fun getAllPackages(project: Project): List<String> {
    return runReadAction {
        try {
            val packages = mutableSetOf<String>()
            val psiManager = PsiManager.getInstance(project)
            val projectRootManager = ProjectRootManager.getInstance(project)
            val sourceRoots = projectRootManager.contentSourceRoots

            sourceRoots.forEach { sourceRoot ->
                val sourceDirectory = psiManager.findDirectory(sourceRoot)
                sourceDirectory?.let {
                    collectPackagesFromDirectory(it, packages)
                }
            }

            packages.sorted()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

private fun collectPackagesFromDirectory(
    directory: PsiDirectory,
    packages: MutableSet<String>
) {
    try {
        directory.files.forEach { file ->
            if (file.name.endsWith(".java") && file is PsiJavaFile) {
                val packageName = file.packageName
                if (packageName.isNotEmpty()) {
                    packages.add(packageName)
                }
            }
        }

        directory.subdirectories.forEach { subDir ->
            collectPackagesFromDirectory(subDir, packages)
        }
    } catch (e: Exception) {
        // Skip problematic directories
    }
}
```

**Key Changes**:
- ✅ Wrapped entire package scanning in `runReadAction`
- ✅ Added comprehensive exception handling
- ✅ Simplified logic to avoid nested read actions
- ✅ Only scans user's source directories (not libraries)

---

### 3. **DomainGeneratorService.kt**

**Issue**: Background task accessing PSI without read actions, and opening files from background thread

**Fix**:
```kotlin
private fun performGeneration(config: GeneratorConfig, indicator: ProgressIndicator) {
    indicator.isIndeterminate = false

    // Determine which classes to generate (PSI read operation)
    val classesToGenerate = runReadAction {
        if (config.singleClassMode && config.singleClass != null) {
            listOf(config.singleClass)
        } else {
            val sourceClasses = PsiHelper.findClassesInPackageRecursive(project, config.sourcePackage)
            sourceClasses.filter { psiClass ->
                !PsiHelper.isInterface(psiClass) && !PsiHelper.isEnum(psiClass)
            }
        }
    }

    // Sort classes by inheritance hierarchy (PSI read operation)
    val sortedClasses = runReadAction {
        if (config.followInheritance) {
            sortByInheritanceHierarchy(classesToGenerate)
        } else {
            classesToGenerate
        }
    }

    // Find or create target directory
    val targetDirectory = findOrCreatePackageDirectory(config.targetPackage)

    // Generate classes
    sortedClasses.forEachIndexed { index, sourceClass ->
        // Read class name in read action
        val className = runReadAction { sourceClass.name ?: "UnknownClass" }

        // Generate class code (PSI read operation)
        val generatedClass = runReadAction {
            generator.generateDomainClass(sourceClass)
        }

        // Write file (write operation)
        val psiFile = createJavaFile(...)
    }

    // Open first generated file on EDT
    if (generatedFiles.isNotEmpty()) {
        val firstFile = generatedFiles.first().virtualFile
        if (firstFile != null) {
            invokeLater {
                FileEditorManager.getInstance(project).openFile(firstFile, true)
            }
        }
    }
}
```

**Key Changes**:
- ✅ Wrapped class scanning in `runReadAction`
- ✅ Wrapped inheritance sorting in `runReadAction`
- ✅ Wrapped code generation in `runReadAction`
- ✅ Separated read and write operations
- ✅ Fixed `findOrCreatePackageDirectory` to use read action for initial lookup
- ✅ Wrapped file opening in `invokeLater` to run on EDT

---

## Threading Model Summary

### Read Operations (PSI reads)
All wrapped in `runReadAction`:
- Detecting class at cursor
- Reading package names
- Scanning source packages
- Sorting by inheritance
- Generating class code

### Write Operations (PSI writes)
All wrapped in `WriteCommandAction` on EDT:
- Creating directories
- Creating Java files
- Modifying PSI

### UI Operations
All on EDT:
- Showing dialogs (`invokeLater`)
- Showing notifications
- Opening files in editor (`invokeLater` from background threads)

### Background Operations
Run in background threads with progress indicators:
- Package scanning
- Class generation
- File writing

---

## Verification Checklist

✅ No PSI access without read action
✅ No PSI modification without write action on EDT
✅ All dialogs shown on EDT
✅ Background tasks properly use progress indicators
✅ Comprehensive exception handling
✅ Graceful degradation on errors

---

## Testing

To verify all threading issues are resolved:

1. Run the plugin:
   ```bash
   ./gradlew runIde
   ```

2. Test these scenarios:
   - Open a Java file and press Ctrl+Shift+E
   - Generate a single class
   - Generate an entire package
   - Try with large packages (50+ classes)
   - Try with classes that have inheritance

3. Check for errors in the IDE log:
   - Help → Show Log in Finder/Explorer
   - Look for threading exceptions

---

## IntelliJ Threading Rules Reference

| Operation | Thread | Required Wrapper |
|-----------|--------|------------------|
| PSI Read | Any | `runReadAction {}` |
| PSI Write | EDT | `WriteCommandAction.runWriteCommandAction {}` |
| UI Display | EDT | `invokeLater {}` or already on EDT |
| Heavy Work | Background | `ProgressManager` with `Task.Backgroundable` |

---

## Future Considerations

- All new PSI operations must be wrapped in appropriate read/write actions
- Use `runReadAction` for ALL PSI reads, even simple ones
- Use `WriteCommandAction` for ALL PSI writes
- Test with IntelliJ's threading assertions enabled
- Profile with IntelliJ Profiler to ensure no thread blocking

---

## References

- [IntelliJ Platform Threading](https://plugins.jetbrains.com/docs/intellij/general-threading-rules.html)
- [PSI Documentation](https://plugins.jetbrains.com/docs/intellij/psi.html)
- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
