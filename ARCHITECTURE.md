# Domain Class Generator - Architecture Overview

## High-Level Architecture

```
User Input (Ctrl+Shift+E)
         ↓
┌────────────────────────────────────────┐
│   GenerateDomainClassesAction         │
│   - Triggered by keyboard shortcut     │
│   - Opens configuration dialog         │
└────────────────────────────────────────┘
         ↓
┌────────────────────────────────────────┐
│   GeneratorConfigDialog (UI)           │
│   - Collects user input                │
│   - Validates package names            │
│   - Returns GeneratorConfig            │
└────────────────────────────────────────┘
         ↓
┌────────────────────────────────────────┐
│   DomainGeneratorService               │
│   - Orchestrates generation process    │
│   - Scans source package               │
│   - Sorts by inheritance hierarchy     │
│   - Creates target directory           │
│   - Manages progress indicator         │
└────────────────────────────────────────┘
         ↓
┌────────────────────────────────────────┐
│   DomainClassGenerator                 │
│   - Generates Java source code         │
│   - Handles imports and annotations    │
│   - Preserves inheritance structure    │
│   - Generates fields and accessors     │
└────────────────────────────────────────┘
         ↓
┌────────────────────────────────────────┐
│   File System                          │
│   - Creates Java files                 │
│   - Opens first generated file         │
│   - Shows success notification         │
└────────────────────────────────────────┘
```

## Component Details

### 1. Action Layer
**Component**: `GenerateDomainClassesAction`

**Responsibilities**:
- Entry point for user interaction
- Handles keyboard shortcut (Ctrl+Shift+E)
- Opens configuration dialog
- Delegates to service layer

**Threading**: Background Thread (BGT) for updates, EDT for action execution

**Key Methods**:
- `update(AnActionEvent)`: Enable/disable action
- `actionPerformed(AnActionEvent)`: Execute action

---

### 2. UI Layer
**Component**: `GeneratorConfigDialog`

**Responsibilities**:
- Displays configuration UI
- Collects user input (packages, annotations, options)
- Validates input
- Returns configuration object

**Threading**: Event Dispatch Thread (EDT)

**Key Features**:
- Package name text fields with validation
- Lombok annotation checkboxes
- Additional options (inheritance, manual accessors)
- Validation on submit

---

### 3. Service Layer
**Component**: `DomainGeneratorService`

**Responsibilities**:
- Business logic orchestration
- Package scanning (source classes)
- Class filtering (exclude interfaces, enums)
- Inheritance hierarchy sorting
- Directory creation
- Progress management
- Notification handling

**Threading**: Background thread with progress indicator

**Key Methods**:
- `generateDomainClasses(config)`: Main entry point
- `performGeneration(config, indicator)`: Actual generation
- `sortByInheritanceHierarchy(classes)`: Sort classes by dependency
- `findOrCreatePackageDirectory(package)`: Create target package

**Algorithm - Inheritance Sorting**:
```
1. Find classes without local superclasses → Add to sorted list
2. While unsorted classes remain:
   a. Find classes whose superclasses are in sorted list
   b. Add to sorted list
   c. If no progress, add remaining classes and break
3. Return sorted list
```

---

### 4. Generator Layer
**Component**: `DomainClassGenerator`

**Responsibilities**:
- Java source code generation
- Import collection
- Annotation generation
- Field declaration generation
- Getter/setter generation (if enabled)
- Type resolution

**Threading**: Background thread (read-only PSI operations)

**Key Methods**:
- `generateDomainClass(sourceClass)`: Generate complete class
- `collectImports(sourceClass, fields)`: Collect necessary imports
- `generateExtendsClause(sourceClass)`: Generate inheritance
- `generateFieldDeclaration(field)`: Generate field
- `getSimpleTypeName(type)`: Resolve type names

**Code Generation Strategy**:
1. Package declaration
2. Imports (Lombok + types)
3. Lombok annotations
4. Class declaration with extends
5. Field declarations
6. Optional manual accessors

---

### 5. Utility Layer

#### PsiHelper
**Responsibilities**:
- PSI tree navigation
- Class and field analysis
- Package scanning
- Type inspection

**Key Methods**:
- `findClassesInPackage()`: Find classes
- `findClassesInPackageRecursive()`: Recursive search
- `getNonStaticFields()`: Get instance fields
- `getNonObjectSuperclass()`: Get parent class
- `belongsToPackage()`: Check package membership

#### NotificationHelper
**Responsibilities**:
- User notifications
- Success/warning/error messages

**Key Methods**:
- `showInfo()`: Success notifications
- `showWarning()`: Warning messages
- `showError()`: Error messages

---

### 6. Model Layer

#### GeneratorConfig
**Data Class** containing:
- `sourcePackage`: Source library package
- `targetPackage`: Target domain package
- `lombokAnnotations`: Selected annotations
- `generateGettersSetters`: Manual accessor flag
- `followInheritance`: Inheritance preservation flag

#### LombokAnnotations
**Data Class** containing:
- Boolean flags for each Lombok annotation
- `getAnnotationFQNs()`: Get list of annotation FQNs
- `hasAnyAnnotation()`: Check if any selected

---

## Threading Model

### Read Operations
- PSI reading: Background thread with read lock
- Package scanning: Background thread
- Class analysis: Background thread

### Write Operations
- File creation: EDT with write action
- PSI modification: EDT with WriteCommandAction

### UI Operations
- Dialog display: EDT
- Progress indicator: Background thread
- Notifications: EDT

---

## Data Flow

```
User Input → GeneratorConfig
           ↓
    PsiClass[] (source classes)
           ↓
    Sorted by Inheritance
           ↓
    For each class:
      - Read fields, methods, superclass (PSI read)
      - Generate source code (string building)
      - Create Java file (PSI write on EDT)
           ↓
    PsiFile[] (generated classes)
           ↓
    Open first file, show notification
```

---

## Error Handling

### Validation Errors
- **Location**: Dialog (before processing)
- **Examples**: Empty package, invalid format, same source/target
- **Handling**: Show validation error, prevent dialog close

### Runtime Errors
- **Location**: Service layer
- **Examples**: Package not found, file creation failed
- **Handling**: Catch exception, show notification, continue with next class

### PSI Errors
- **Location**: Generator and utilities
- **Examples**: Null PSI elements, unresolved types
- **Handling**: Null-safe operators, default values, skip problematic elements

---

## Performance Considerations

### Optimization Strategies
1. **Lazy Loading**: Only load classes when needed
2. **Batch Processing**: Generate all classes in single operation
3. **Progress Indicator**: Keep UI responsive during long operations
4. **Background Threading**: Heavy work on background threads
5. **Read Lock Optimization**: Minimize time holding read locks

### Bottlenecks
- **Package Scanning**: O(n) where n = number of classes
- **Inheritance Sorting**: O(n²) worst case for deep hierarchies
- **File Creation**: O(n) sequential operations on EDT
- **Type Resolution**: O(n×m) where m = average fields per class

### Scalability
- **Small packages** (< 10 classes): Instant
- **Medium packages** (10-50 classes): < 1 second
- **Large packages** (50-200 classes): 1-5 seconds
- **Very large packages** (> 200 classes): May need optimization

---

## Extension Points

### Adding New Features

#### 1. Custom Annotations
Extend `LombokAnnotations` model:
```kotlin
data class LombokAnnotations(
    // Existing annotations...
    val useCustomAnnotation: Boolean = false
)
```

#### 2. Custom Field Transformations
Extend `DomainClassGenerator`:
```kotlin
private fun transformField(field: PsiField): String {
    // Custom transformation logic
}
```

#### 3. Custom UI Components
Extend `GeneratorConfigDialog`:
```kotlin
override fun createCenterPanel(): JComponent {
    return panel {
        // Existing UI...
        group("Custom Options") {
            // New UI components
        }
    }
}
```

---

## Testing Strategy

### Unit Tests
- **Target**: Business logic in utilities
- **Framework**: JUnit 5
- **Examples**: Type resolution, package name parsing

### Integration Tests
- **Target**: PSI operations, file generation
- **Framework**: IntelliJ Platform Test Framework
- **Examples**: Generate class from PSI, validate output

### Manual Testing
- **Target**: End-to-end workflow
- **Method**: Run plugin in sandbox IDE
- **Scenarios**: See TESTING.md

---

## Security Considerations

### Input Validation
- Package names validated with regex
- Path traversal prevention (uses PSI API)
- No user-provided code execution

### File System Access
- Limited to project directory
- Uses IntelliJ's VFS (Virtual File System)
- Write operations wrapped in write actions

---

## Future Enhancements

### Short Term
1. Support for Kotlin data classes
2. Custom annotation configuration
3. Field mapping customization
4. Preview before generation

### Medium Term
1. Support for Java records (Java 14+)
2. Template-based generation
3. Incremental generation (selected classes only)
4. Undo support

### Long Term
1. Visual package browser
2. Batch operations across multiple projects
3. Integration with mapping libraries (MapStruct, ModelMapper)
4. AI-powered field mapping suggestions

---

## Dependencies

### Runtime Dependencies
- IntelliJ Platform SDK (2025.1+)
- com.intellij.java (bundled plugin)
- Kotlin Standard Library (2.1.0)

### Build Dependencies
- Gradle (8.0+)
- IntelliJ Platform Gradle Plugin (2.5.0)
- Kotlin Gradle Plugin (2.1.0)

### Optional Dependencies
- Lombok (in target project for annotations)

---

## Deployment

### Build Process
1. Compile Kotlin sources
2. Process resources (plugin.xml)
3. Create JAR with manifest
4. Package into ZIP

### Installation
1. Manual: Install from disk
2. Marketplace: Publish to JetBrains Marketplace
3. Enterprise: Deploy via plugin repository

---

## Monitoring and Logging

### Logging Strategy
- Use IntelliJ's Logger API
- Log levels:
  - INFO: Generation start/complete
  - WARN: Non-fatal issues (class skipped)
  - ERROR: Fatal errors with stack traces

### Metrics
- Number of classes generated
- Generation time
- Success/failure rate
- User-cancelled operations

---

This architecture ensures:
- **Separation of Concerns**: Clear layer boundaries
- **Testability**: Each component can be tested independently
- **Maintainability**: Well-organized, documented code
- **Performance**: Optimized threading and resource usage
- **Extensibility**: Easy to add new features
- **Robustness**: Comprehensive error handling
