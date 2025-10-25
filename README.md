# Domain Class Generator - IntelliJ IDEA Plugin

A powerful IntelliJ IDEA plugin that generates domain classes from library classes with Lombok annotations support.

## Features

- **Smart Context Detection**: Automatically detects the class at your cursor or current file
- **Flexible Generation**: Generate a single class or an entire package
- **Package Selector**: Choose from existing packages in dropdown or create new ones
- **Automatic Dependency Collection**: Automatically generates domain classes for:
  - All superclasses up to Object
  - All custom field types (recursively)
  - Complete dependency graph resolution
- **Correct Import Resolution**: Generated classes import from target package (not library packages)
- **Inheritance Support**: Maintains superclass/subclass relationships in generated classes
- **Lombok Integration**: Add Lombok annotations (@Data, @Builder, @Getter, @Setter, etc.) to generated classes
- **Configurable UI**: Easy-to-use dialog with intelligent suggestions
- **Fast Access**: Keyboard shortcut **Ctrl+Shift+E** (Cmd+Shift+E on Mac)

## Installation

### From Source
1. Clone this repository
2. Run `./gradlew build` to build the plugin
3. The plugin ZIP will be in `build/distributions/`
4. In IntelliJ IDEA: Settings → Plugins → ⚙️ → Install Plugin from Disk
5. Select the generated ZIP file
6. Restart IDE

### Development Mode
```bash
# Launch IDE with plugin installed for testing
./gradlew runIde
```

## Usage

### Quick Start

1. **Open any Java file** (preferably a library class you want to extend)
2. **Place your cursor** on the class or anywhere in the file
3. Press **Ctrl+Shift+E** (or **Cmd+Shift+E** on Mac)
4. The plugin automatically detects:
   - The class at your cursor
   - The package of that class
5. Configure generation:
   - **Detected Class**: Shows the class you're currently on
   - **Source Package**: Auto-filled from detected class (editable)
   - **Target Package**: Click **📁 Browse** to select from tree view, or type manually
   - **Generation Scope**: Choose to generate only the detected class or entire package
   - **Lombok Annotations**: Select desired annotations
   - **Options**: Configure inheritance and accessor generation
6. Click **OK**

The plugin will:
- Generate domain class(es) in the target package
- Maintain inheritance hierarchy
- Add selected Lombok annotations
- Show progress indicator
- Open the first generated file

### Example

**Source Library Class** (`com.library.models.User`):
```java
package com.library.models;

public class User {
    private Long id;
    private String username;
    private String email;
}
```

**Generated Domain Class** (`com.myapp.domain.User`) with @Data and @Builder:
```java
package com.myapp.domain;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class User {
    private Long id;
    private String username;
    private String email;
}
```

### With Automatic Superclass Generation

**Example**: When generating from a library class that extends another library class:

**Source Class**:
```java
// org.example.data.BatchDocumentInputConfig
package org.example.data;

import com.google.protobuf.GeneratedMessageV3;

public class BatchDocumentInputConfig extends GeneratedMessageV3 {
    private String inputPath;
    private int batchSize;
}
```

**Plugin automatically generates BOTH classes**:
```java
// com.myapp.domain.GeneratedMessageV3 (automatically included!)
@Data
public class GeneratedMessageV3 {
    // Fields from the library superclass
}

// com.myapp.domain.BatchDocumentInputConfig
@Data
public class BatchDocumentInputConfig extends GeneratedMessageV3 {
    private String inputPath;
    private int batchSize;
}
```

The plugin automatically:
1. Detects that `BatchDocumentInputConfig` extends `GeneratedMessageV3`
2. Generates a domain class for `GeneratedMessageV3` first (from `com.google.protobuf`)
3. Generates `BatchDocumentInputConfig` that extends your new domain `GeneratedMessageV3`
4. Uses correct imports: `import com.myapp.domain.GeneratedMessageV3;` (NOT the library class!)
5. Continues up the hierarchy until reaching `Object`
6. Also generates domain classes for any custom types used in fields

**Result**: All generated classes properly reference each other from the target package, not the library packages!

### With Field Type Dependencies

**Source Classes**:
```java
// com.library.models.Address
public class Address {
    private String street;
    private String city;
}

// com.library.models.User
public class User {
    private String name;
    private Address address;  // Custom type!
}
```

**When you generate `User`, the plugin automatically**:
1. Detects `Address` is used as a field type
2. Generates domain class for `Address` first
3. Generates domain class for `User` with correct import

**Generated Classes**:
```java
// com.myapp.domain.Address (auto-generated!)
package com.myapp.domain;

@Data
public class Address {
    private String street;
    private String city;
}

// com.myapp.domain.User
package com.myapp.domain;

import com.myapp.domain.Address;  // Correct import!

@Data
public class User {
    private String name;
    private Address address;
}
```

### With Inheritance in Same Package

**Source Classes**:
```java
// com.library.models.BaseEntity
public class BaseEntity {
    private Long id;
    private Date createdAt;
}

// com.library.models.User extends BaseEntity
public class User extends BaseEntity {
    private String username;
}
```

**Generated Classes** (with inheritance preserved):
```java
// com.myapp.domain.BaseEntity
@Data
public class BaseEntity {
    private Long id;
    private Date createdAt;
}

// com.myapp.domain.User
@Data
public class User extends BaseEntity {
    private String username;
}
```

## Configuration Options

### Generation Scope
- **Single Class**: Generate domain class only for the detected class at cursor
- **Entire Package**: Generate domain classes for all classes in the source package

### Package Selection
- **Source Package**: Auto-detected from current file, editable
- **Target Package**: Native JetBrains package browser:
  - Click **folder icon** to open tree view of all project packages
  - Navigate and select from visual package hierarchy
  - Auto-suggested package pre-filled (domain, dto, model, entity, etc.)
  - Type custom package name manually if preferred
  - Only shows your project's source directories (excludes libraries)

### Lombok Annotations
- **@Data**: Generates getters, setters, toString, equals, and hashCode
- **@Builder**: Builder pattern for object construction
- **@Getter**: Getter methods for all fields
- **@Setter**: Setter methods for all fields
- **@NoArgsConstructor**: No-argument constructor
- **@AllArgsConstructor**: Constructor with all arguments
- **@ToString**: toString method
- **@EqualsAndHashCode**: equals and hashCode methods

### Additional Options
- **Follow inheritance structure**: Maintains superclass relationships (recommended)
- **Generate manual getters/setters**: Creates manual accessors if no Lombok getter/setter is selected

## Requirements

- IntelliJ IDEA 2023.0 or higher (build 230+)
- Java 17 or higher
- Java plugin enabled in IDE

## Build Commands

```bash
# Build the plugin
./gradlew build

# Run tests
./gradlew test

# Launch IDE with plugin for testing
./gradlew runIde

# Build plugin ZIP for distribution
./gradlew buildPlugin

# Verify plugin structure
./gradlew verifyPlugin
```

## Project Structure

```
src/main/kotlin/com/example/domaingenerator/
├── actions/               # Action handlers
├── generator/             # Code generation logic
├── models/                # Data models
├── services/              # Business logic services
├── ui/                    # User interface components
└── utils/                 # Utility classes

src/main/resources/
└── META-INF/
    └── plugin.xml         # Plugin configuration
```

## Development

See [JETBRAINS_PLUGIN_CREATION_GUIDE.md](JETBRAINS_PLUGIN_CREATION_GUIDE.md) for comprehensive plugin development guidelines.

See [CLAUDE.md](CLAUDE.md) for detailed architectural documentation.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test with `./gradlew runIde`
5. Submit a pull request

## License

This project is provided as-is for educational and commercial use.

## Support

For issues, questions, or feature requests, please open an issue on GitHub.

## Changelog

### Version 1.0.0 (Initial Release)
- Generate domain classes from library packages
- Lombok annotation support (@Data, @Builder, etc.)
- Inheritance structure preservation
- Configuration dialog with validation
- Progress indicators and notifications
- Keyboard shortcut: Ctrl+Shift+E

## Tips

- The plugin works best with well-structured library packages
- Ensure the source package actually exists in your project classpath
- Generated files will overwrite existing files with the same name
- Use "Follow inheritance structure" for complex class hierarchies
- Combine @Data with @Builder for powerful domain models
- If no Lombok is desired, enable "Generate manual getters/setters"

## Troubleshooting

**Problem**: Action not visible/enabled
- **Solution**: Ensure you have a Java file open and the Java plugin is enabled

**Problem**: "No classes found in package"
- **Solution**: Verify the source package name is correct and exists in your project's classpath

**Problem**: Compilation errors in generated classes
- **Solution**: Ensure Lombok is added as a dependency in your project

**Problem**: Missing imports in generated classes
- **Solution**: The plugin automatically generates imports; if missing, please report as a bug

## Advanced Usage

### Batch Generation
The plugin generates all classes in the source package in one operation, making it perfect for:
- Migrating from library DTOs to domain models
- Creating API response objects from library classes
- Generating test fixtures from production classes

### Type Mapping
The plugin intelligently handles:
- Generic types (List<T>, Map<K,V>)
- Nested classes from the source package
- Arrays and primitive types
- Custom types

Enjoy using the Domain Class Generator plugin!
