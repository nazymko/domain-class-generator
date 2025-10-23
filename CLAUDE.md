# Domain Class Generator - IntelliJ IDEA Plugin

## Overview

This IntelliJ IDEA plugin generates domain classes from library classes with Lombok annotations support. It allows developers to quickly create domain models based on external library classes while maintaining inheritance structures and adding Lombok annotations.

## Features

- Generate domain classes from any library package to a target package
- Automatically follows extension/superclass structure where possible
- Configure Lombok annotations (@Builder, @Data, @Getter, @Setter, etc.) per generation
- Easy-to-use configuration UI for package selection and annotation options
- Keyboard shortcut: **Ctrl+Shift+E** (Windows/Linux) or **Cmd+Shift+E** (Mac)

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
  - Input fields:
    - Source package text field
    - Target package text field
    - Lombok annotation checkboxes
    - Additional options (inheritance, manual accessors)
  - Validates package names and ensures source != target
  - Returns GeneratorConfig on OK

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

1. Open any Java file in your project
2. Press **Ctrl+Shift+E** (or Cmd+Shift+E on Mac)
3. In the dialog:
   - Enter source package (e.g., `com.library.models`)
   - Enter target package (e.g., `com.myapp.domain`)
   - Select desired Lombok annotations
   - Configure additional options
4. Click OK
5. Plugin will:
   - Scan source package for classes
   - Generate domain classes in target package
   - Show progress indicator
   - Display success notification
   - Open first generated file

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

- IntelliJ Platform SDK 2025.1+
- com.intellij.java (bundled plugin for Java PSI support)
- Kotlin 2.1.0
- Gradle 8.0+

## Plugin Metadata

- **ID**: com.example.domaingenerator
- **Name**: Domain Class Generator
- **Version**: 1.0-SNAPSHOT
- **Vendor**: YourCompany
- **Min IDE Version**: 2025.1 (build 251)

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
