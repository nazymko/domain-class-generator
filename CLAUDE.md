# Domain Class Generator - IntelliJ IDEA Plugin

## Overview

This IntelliJ IDEA plugin generates domain classes from library classes with Lombok annotations support. It allows developers to quickly create domain models based on external library classes while maintaining inheritance structures and adding Lombok annotations.

## Features

- **Smart Context Detection**: Automatically detects the class at cursor position or from current file
- **Flexible Generation Modes**:
  - Single class generation (for detected class only)
  - Entire package generation (all classes in source package)
- **Intelligent Package Selection**:
  - Dropdown showing existing packages in project
  - Suggested target packages based on source package
  - Create new packages on-the-fly
- **Inheritance Support**: Automatically follows extension/superclass structure
- **Lombok Integration**: Configure annotations (@Builder, @Data, @Getter, @Setter, etc.)
- **Easy-to-use UI**: Context-aware dialog with auto-populated fields
- **Keyboard Shortcut**: **Ctrl+Shift+E** (Windows/Linux) or **Cmd+Shift+E** (Mac)

## Project Structure

```
src/main/kotlin/com/example/domaingenerator/
├── actions/
│   └── GenerateDomainClassesAction.kt    # Main action triggered by Ctrl+Shift+E
├── generator/
│   └── DomainClassGenerator.kt           # Core class generation logic
├── models/
│   └── GeneratorConfig.kt                # Configuration data models
├── services/
│   └── DomainGeneratorService.kt         # Orchestrates generation process
├── ui/
│   └── GeneratorConfigDialog.kt          # Configuration dialog UI
└── utils/
    ├── NotificationHelper.kt             # User notification utilities
    └── PsiHelper.kt                      # PSI manipulation utilities
```

## Plugin Components

### Actions

- **GenerateDomainClassesAction** (`actions/GenerateDomainClassesAction.kt`)
  - Entry point for the plugin
  - Triggered by Ctrl+Shift+E shortcut
  - Opens configuration dialog and initiates generation
  - Implements DumbAware for availability during indexing
  - Uses BGT (Background Thread) for action updates

### Models

- **GeneratorConfig** (`models/GeneratorConfig.kt`)
  - Configuration data class containing:
    - sourcePackage: Package to scan for library classes
    - targetPackage: Package where domain classes will be generated
    - lombokAnnotations: Selected Lombok annotations
    - followInheritance: Whether to maintain superclass relationships
    - generateGettersSetters: Generate manual accessors if no Lombok
    - singleClassMode: Whether to generate only one class or entire package
    - singleClass: The specific PsiClass to generate (if single class mode)

- **LombokAnnotations** (`models/GeneratorConfig.kt`)
  - Configurable Lombok annotations:
    - @Data
    - @Builder
    - @Getter / @Setter
    - @NoArgsConstructor / @AllArgsConstructor
    - @ToString
    - @EqualsAndHashCode

### UI Components

- **GeneratorConfigDialog** (`ui/GeneratorConfigDialog.kt`)
  - Configuration dialog using IntelliJ's Kotlin UI DSL
  - Auto-populated with detected context:
    - Shows detected class name and FQN
    - Auto-fills source package from detected class
    - Pre-fills suggested target package
  - Input components:
    - Detected Context group (shows current class info)
    - Source package text field (auto-filled, editable)
    - Target package field with browse button (opens native PackageChooserDialog)
    - Generation scope radio buttons (single class vs entire package)
    - Lombok annotation checkboxes
    - Additional options (inheritance, manual accessors)
  - Uses JetBrains' native `PackageChooserDialog` for package selection
  - Validates package names using PackageHelper
  - Returns GeneratorConfig with all selections

### Services

- **DomainGeneratorService** (`services/DomainGeneratorService.kt`)
  - Project-level service (one per project)
  - Orchestrates the generation process:
    1. Scans source package for classes
    2. Filters out interfaces, enums
    3. Sorts classes by inheritance hierarchy (superclasses first)
    4. Creates target package directory if needed
    5. Generates domain classes using DomainClassGenerator
    6. Shows progress indicator and notifications
  - Key methods:
    - `generateDomainClasses(config)`: Main entry point
    - `sortByInheritanceHierarchy()`: Ensures superclasses are generated first
    - `findOrCreatePackageDirectory()`: Creates target package structure

### Generator

- **DomainClassGenerator** (`generator/DomainClassGenerator.kt`)
  - Core code generation logic
  - Generates Java source code from PsiClass
  - Features:
    - Generates package declaration
    - Collects and generates imports (including Lombok)
    - Handles generic types and type parameters
    - Generates extends clause for inheritance
    - Generates field declarations
    - Optional manual getter/setter generation
    - Handles classes from source package (replaces package in types)
  - Returns GeneratedClass with source code

### PSI Utilities

- **PsiHelper** (`utils/PsiHelper.kt`)
  - PSI manipulation and analysis utilities
  - Key methods:
    - `findClassesInPackage()`: Find classes in package (non-recursive)
    - `findClassesInPackageRecursive()`: Find classes including subpackages
    - `getNonStaticFields()`: Get instance fields
    - `getNonObjectSuperclass()`: Get superclass (excluding Object)
    - `hasNonObjectSuperclass()`: Check if class extends another class
    - `belongsToPackage()`: Check if class is in package
    - `findClassByFqn()`: Find class by fully qualified name
    - Type checking: `isPrimitive()`, `isAbstract()`, `isInterface()`, `isEnum()`
    - Field analysis: `hasGetter()`, `hasSetter()`

- **PackageHelper** (`utils/PackageHelper.kt`)
  - Package discovery and validation utilities
  - Key methods:
    - `getAllPackages()`: Get all Java packages in user's project source directories (excludes libraries)
    - `collectPackagesFromDirectory()`: Recursively scan source directories for packages
    - `getPackagesMatchingPrefix()`: Autocomplete support
    - `getSuggestedTargetPackages()`: Suggest packages based on source
    - `isValidPackageName()`: Validate package name format
  - Uses `ProjectRootManager.contentSourceRoots` to only scan user's code

- **NotificationHelper** (`utils/NotificationHelper.kt`)
  - Shows balloon notifications to users
  - Methods: `showInfo()`, `showWarning()`, `showError()`
  - Uses notification group: "DomainGenerator.Notifications"

## Architecture Decisions

### Why Kotlin UI DSL?
- Modern, declarative UI building
- Type-safe and concise
- Better IntelliJ integration than Swing
- Easier to maintain and extend

### Why Project-Level Service?
- Each project may have different configurations
- Scoped to project lifecycle
- Better resource management per project

### Why Sort by Inheritance Hierarchy?
- Ensures superclasses are generated before subclasses
- Allows proper import resolution for generated domain classes
- Prevents compilation errors in target package

### Threading Strategy
- Action updates: BGT (Background Thread) - lightweight checks
- UI operations: EDT (Event Dispatch Thread) - dialog display
- PSI modifications: EDT + Write Action - all file creation/modification
- Heavy operations: Background tasks with progress indicators

## Usage

### Basic Workflow

1. **Open any Java file** (library class you want to extend)
2. **Position cursor** on the class or anywhere in the file
3. Press **Ctrl+Shift+E** (or **Cmd+Shift+E** on Mac)
4. The dialog opens with auto-detected context:
   - **Detected Class**: Shows your current class (e.g., "User")
   - **Source Package**: Auto-filled (e.g., "com.library.models")
   - **Target Package**: Dropdown with suggestions
5. Choose generation scope:
   - **Single Class**: Generate only the detected class
   - **Entire Package**: Generate all classes in source package
6. Select Lombok annotations and options
7. Click **OK**
8. Plugin will:
   - Generate domain class(es) in target package
   - Show progress indicator
   - Display success notification
   - Open first generated file

### Package Selection

The target package field uses JetBrains' native package chooser:
1. **Click browse button** (📁) to open package chooser dialog
2. **Tree view** shows all packages in your project's source directories
3. **Navigate** the package hierarchy visually
4. **Select** the target package from the tree
5. **Or type manually** - you can also type a package name directly

**Important**: The package chooser only shows packages from your project's source roots (src/main/java, etc.). Library/dependency packages are excluded. The field is pre-filled with a suggested package (domain, dto, model, etc.) based on the source package.

### Generation Modes

#### Single Class Mode
- Available when a class is detected at cursor
- Generates only the detected class
- Faster for one-off domain class creation
- Still respects inheritance (generates superclass if needed and selected)

#### Package Mode
- Scans entire source package recursively
- Generates all non-interface, non-enum classes
- Maintains inheritance hierarchy
- Suitable for bulk migration

## Build Commands

```bash
# Build the plugin
./gradlew build

# Run tests
./gradlew test

# Launch IDE with plugin installed (for testing)
./gradlew runIde

# Build plugin ZIP for distribution
./gradlew buildPlugin

# Verify plugin structure
./gradlew verifyPlugin
```

## Testing

### Manual Testing
1. Run `./gradlew runIde` to launch IDE with plugin
2. Create or open a Java project
3. Ensure project has a library with classes in a package
4. Use Ctrl+Shift+E to test generation

### Test Scenarios
- Single class with no superclass
- Class hierarchy (parent -> child)
- Classes with generic types (List<String>, Map<K,V>)
- Classes with various field types (primitives, objects, collections)
- Classes from different packages
- Different Lombok annotation combinations

## Known Issues

None currently reported.

## Performance Considerations

- Uses recursive package scanning - may be slow for large packages
- Sorts classes by inheritance which has O(n²) complexity for deep hierarchies
- File creation is done sequentially on EDT - may block UI briefly for many classes
- Consider using `ProgressManager` for better UX with large generations

## Future Enhancements

Possible improvements:
- Support for generating domain classes from specific selected classes (not entire package)
- Support for Kotlin data classes generation
- Support for record classes (Java 14+)
- Custom field mapping configuration
- Support for custom annotations beyond Lombok
- Preview generated code before writing files
- Undo support for generation
- Support for package selection via file chooser

## Dependencies

- IntelliJ Platform SDK 2023.0+ (build 230+)
- com.intellij.java (bundled plugin for Java PSI support)
- Kotlin 2.1.0
- Gradle 8.0+
- Java 17+

## Plugin Metadata

- **ID**: io.github.nazymko.domaingenerator
- **Name**: Domain Class Generator (Ctrl+Shift+E)
- **Version**: 1.0.0
- **Vendor**: Domain Class Generator Team
- **Min IDE Version**: 2023.0 (build 230)
- **Supported IDE Versions**: IntelliJ IDEA 2023.0 - 2025.1+

## Development Notes

- Always use `WriteCommandAction.runWriteCommandAction()` for PSI modifications
- Always check for null when working with PSI elements
- Use `ProgressManager` for long-running operations
- Show notifications to keep users informed
- Validate user input in dialogs before processing
- Handle exceptions gracefully and show meaningful error messages

## Changelog

### Version 1.0.0 (Initial Release)
- Generate domain classes from library packages
- Lombok annotation support
- Inheritance structure preservation
- Configuration dialog with validation
- Progress indicators and notifications
- Keyboard shortcut: Ctrl+Shift+E
