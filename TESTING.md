# Testing the Domain Class Generator Plugin

This guide will help you test the Domain Class Generator plugin.

## Quick Start

### 1. Build and Run the Plugin

```bash
# Launch IntelliJ IDEA with the plugin installed
./gradlew runIde
```

This will open a new IntelliJ IDEA instance with your plugin installed.

### 2. Create a Test Project

In the sandbox IDE:

1. Create a new Java project or open an existing one
2. Ensure you have a `src/main/java` directory

### 3. Create Example Library Classes

Create the following test classes to simulate a library package:

#### File: `src/main/java/com/library/models/BaseEntity.java`
```java
package com.library.models;

import java.util.Date;

public class BaseEntity {
    private Long id;
    private Date createdAt;
    private Date updatedAt;
}
```

#### File: `src/main/java/com/library/models/User.java`
```java
package com.library.models;

import java.util.List;

public class User extends BaseEntity {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> roles;
}
```

#### File: `src/main/java/com/library/models/Product.java`
```java
package com.library.models;

import java.math.BigDecimal;

public class Product extends BaseEntity {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
}
```

#### File: `src/main/java/com/library/models/Order.java`
```java
package com.library.models;

import java.math.BigDecimal;
import java.util.List;

public class Order extends BaseEntity {
    private User user;
    private List<Product> products;
    private BigDecimal totalAmount;
    private String status;
}
```

### 4. Generate Domain Classes

1. Open any Java file (e.g., `User.java`)
2. Press **Ctrl+Shift+E** (or **Cmd+Shift+E** on Mac)
3. A dialog will appear:
   - **Source Package**: Enter `com.library.models`
   - **Target Package**: Enter `com.myapp.domain`
   - **Lombok Annotations**: Check `@Data` and `@Builder`
   - **Options**: Keep "Follow inheritance structure" checked
4. Click **OK**

### 5. Verify Generated Classes

The plugin will create domain classes in `src/main/java/com/myapp/domain/`:

- `BaseEntity.java`
- `User.java`
- `Product.java`
- `Order.java`

#### Expected Result for `User.java`:
```java
package com.myapp.domain;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class User extends BaseEntity {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> roles;
}
```

## Test Scenarios

### Scenario 1: Basic Generation with @Data
- Source Package: `com.library.models`
- Target Package: `com.myapp.domain`
- Annotations: `@Data` only
- Expected: Classes with @Data annotation

### Scenario 2: Builder Pattern
- Source Package: `com.library.models`
- Target Package: `com.myapp.dto`
- Annotations: `@Data`, `@Builder`
- Expected: Classes with both annotations

### Scenario 3: No Lombok, Manual Accessors
- Source Package: `com.library.models`
- Target Package: `com.myapp.pojo`
- Annotations: None
- Options: Enable "Generate manual getters/setters"
- Expected: Classes with explicit getter/setter methods

### Scenario 4: No Inheritance
- Source Package: `com.library.models`
- Target Package: `com.myapp.flat`
- Annotations: `@Data`
- Options: Disable "Follow inheritance structure"
- Expected: Classes without extends clause

### Scenario 5: Complex Annotations
- Source Package: `com.library.models`
- Target Package: `com.myapp.entities`
- Annotations: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@ToString`, `@EqualsAndHashCode`
- Expected: Classes with all selected annotations

## Validation Checks

After generation, verify:

1. **Package Declaration**: Correct target package
2. **Imports**: All necessary imports are present
3. **Lombok Annotations**: Selected annotations are added
4. **Inheritance**: Extends clause matches source (if enabled)
5. **Fields**: All non-static fields from source are present
6. **Types**: Field types are correct (including generics)
7. **No Compilation Errors**: Generated code compiles successfully

## Common Issues

### Issue: "No classes found in package"
**Cause**: Source package doesn't exist or is empty
**Solution**: Verify the package name and ensure classes exist

### Issue: Compilation errors in generated classes
**Cause**: Missing Lombok dependency
**Solution**: Add Lombok to your project:
```xml
<!-- Maven -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.30</version>
    <scope>provided</scope>
</dependency>
```

```kotlin
// Gradle
dependencies {
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
}
```

### Issue: Plugin not visible
**Cause**: Plugin not loaded or Java file not open
**Solution**: Ensure you're in a Java file and the plugin is installed

## Debug Mode

To debug the plugin:

1. Set breakpoints in your Kotlin code
2. Run with debug:
   ```bash
   ./gradlew runIde --debug-jvm
   ```
3. Attach debugger to port 5005

## Performance Testing

Test with large packages:

1. Create a package with 50+ classes
2. Generate domain classes
3. Verify progress indicator appears
4. Check all classes are generated correctly
5. Monitor memory usage in IDE

## Advanced Testing

### Test Circular Dependencies
Create classes with circular references and verify plugin handles them gracefully.

### Test Generic Types
Create classes with complex generics:
```java
public class Repository<T extends BaseEntity> {
    private List<T> entities;
    private Map<String, T> entityMap;
}
```

### Test Inner Classes
Create classes with inner classes and verify handling.

### Test Annotations on Fields
Create source classes with field annotations and verify they're preserved or handled correctly.

## Automated Testing

Consider creating unit tests:

```kotlin
class DomainGeneratorTest : BasePlatformTestCase() {

    fun testGenerateSimpleClass() {
        // Create test file
        val file = myFixture.addFileToProject(
            "src/test/java/TestClass.java",
            """
            package test;
            public class TestClass {
                private String field;
            }
            """.trimIndent()
        )

        // Test generation logic
        // Assert results
    }
}
```

## Feedback

After testing, note:
- What works well
- What could be improved
- Any bugs or issues
- Performance concerns
- Feature requests

## Next Steps

1. Test all scenarios above
2. Try with your own library classes
3. Report any issues
4. Suggest improvements
5. Share with team for feedback
