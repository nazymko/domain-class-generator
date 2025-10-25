# Changelog

All notable changes to the Domain Class Generator plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-01-XX

### Initial Release

#### Added
- **Code Generation Engine**
  - Generate domain classes from library classes
  - Generate enums with constructor patterns preserved
  - Support for generic types (List<T>, Map<K,V>, etc.)
  - Automatic import collection and management

- **Enum Support**
  - Proper enum syntax generation (not as classes)
  - Enum constant declarations with initialization arguments
  - Constructor generation from enum patterns
  - Field declarations derived from constructor parameters
  - Import collection for enum constructor parameter types

- **Dependency Management**
  - Recursive dependency collection for complete domain isolation
  - Superclass hierarchy tracking
  - Field type dependency resolution
  - Single class mode generates all dependencies (not just selected class)
  - Prevents coupling with external library classes

- **Lombok Integration**
  - Configurable Lombok annotations:
    - @Data - Complete data class with getters, setters, equals, hashCode, toString
    - @Builder - Builder pattern for object construction
    - @Getter / @Setter - Individual accessor generation
    - @NoArgsConstructor / @AllArgsConstructor - Constructor variants
    - @ToString - String representation
    - @EqualsAndHashCode - Equality methods
  - Smart annotation placement on both classes and enums
  - Automatic import generation for selected annotations

- **JavaDoc Generation**
  - Optional JavaDoc documentation (configurable in UI)
  - Class-level documentation with:
    - Source library class reference
    - Fully qualified name of source
    - Superclass information (when applicable)
    - Plugin name and version (dynamically retrieved)
    - Author information from vendor metadata
  - Clean generated code without verbose field-level docs

- **User Interface**
  - Compact, intuitive configuration dialog
  - Real-time validation with immediate feedback
  - Detected context auto-population:
    - Current class detection at cursor
    - Source package auto-fill
    - Smart target package suggestions
  - Native JetBrains package chooser integration
  - Generation scope selection (single class vs entire package)
  - Keyboard shortcut: **Ctrl+Shift+E** (Windows/Linux) or **Cmd+Shift+E** (Mac)

- **Smart Features**
  - Inheritance structure preservation across generated classes
  - Package-based organization
  - Progress indicators for long operations
  - Background task execution (doesn't block UI)
  - Automatic file formatting
  - Opens generated files in editor

- **Quality & Performance**
  - EDT-safe operations (no UI blocking)
  - Proper PSI threading (read/write actions)
  - Fast string-based package suggestions (no slow file I/O on UI thread)
  - Progress tracking for batch generation
  - Error handling with user-friendly notifications

#### Technical Details
- Minimum IntelliJ version: 2023.0 (build 230)
- Maximum tested version: 2025.1
- Compatible with: IntelliJ IDEA Community and Ultimate
- Language: Kotlin 2.1.0
- JVM Target: Java 17
- Requires: com.intellij.java plugin (bundled)

#### Architecture Highlights
- Clean separation of concerns (Actions → Services → Generators → Utils)
- Project-level service for scoped operations
- Stateless utility objects for PSI manipulation
- DTO pattern for configuration management
- Dependency injection via IntelliJ service locator

### Documentation
- Comprehensive CLAUDE.md for AI-assisted development
- JETBRAINS_PLUGIN_CREATION_GUIDE_LESSONS_LEARNED.md with real-world lessons
- Inline code documentation
- Architecture decision records

---

## Future Enhancements (Planned)

### Version 1.1.0 (Tentative)
- Kotlin data class generation support
- Java Record class support (Java 14+)
- Custom field mapping configuration
- Preview generated code before writing
- Batch operations with multiple source packages
- Configuration presets/templates

### Version 1.2.0 (Tentative)
- MapStruct mapper generation between library and domain classes
- Support for custom annotations beyond Lombok
- Field exclusion/inclusion filters
- Type conversion rules
- Integration with popular frameworks (Spring, Hibernate, etc.)

---

## Support & Contribution

- **Issues**: Report bugs and feature requests on [GitHub Issues](https://github.com/nazymko/domain-class-generator/issues)
- **Discussions**: Join community discussions on [GitHub Discussions](https://github.com/nazymko/domain-class-generator/discussions)
- **Documentation**: Visit our [Wiki](https://github.com/nazymko/domain-class-generator/wiki)

---

## License

This plugin is released under the MIT License. See LICENSE file for details.

---

**Note**: This changelog will be updated with each release. Always check the latest version for the most up-to-date information.
