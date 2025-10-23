# Domain Class Generator - IntelliJ IDEA Plugin

A powerful IntelliJ IDEA plugin that generates domain classes from library classes with Lombok annotations support.

## Features

- **Generate Domain Classes**: Automatically generate domain classes from any library package
- **Inheritance Support**: Maintains superclass/subclass relationships in generated classes
- **Lombok Integration**: Add Lombok annotations (@Data, @Builder, @Getter, @Setter, etc.) to generated classes
- **Configurable UI**: Easy-to-use dialog for package selection and annotation configuration
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

1. Open any Java file in your project
2. Press **Ctrl+Shift+E** (or **Cmd+Shift+E** on Mac)
3. Configure generation:
   - **Source Package**: Enter the full package name of library classes (e.g., `com.library.models`)
   - **Target Package**: Enter where to generate domain classes (e.g., `com.myapp.domain`)
   - **Lombok Annotations**: Select desired annotations
   - **Options**: Configure inheritance and accessor generation
4. Click **OK**

The plugin will:
- Scan all classes in the source package (including subpackages)
- Generate domain classes in the target package
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

### With Inheritance

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

- IntelliJ IDEA 2025.1 or higher
- Java 21 or higher
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
