# Plugin Build Guide

Complete guide for building the Domain Class Generator plugin in different formats.

---

## 🔨 Build Commands

### 1. Build Plugin JAR (Simple)

```bash
./gradlew jar
```

**Output:** `build/libs/domain-class-generator-1.0.0.jar`

**What it contains:**
- Compiled Kotlin/Java classes
- Resources (plugin.xml, etc.)
- Basic plugin structure

**Use case:** Quick compilation check

---

### 2. Build Instrumented JAR (With PSI)

```bash
./gradlew instrumentCode
```

**Output:** `build/libs/domain-class-generator-1.0.0-instrumented.jar`

**What it contains:**
- All classes from regular JAR
- IntelliJ Platform instrumentation
- PSI stubs and indexes
- Runtime optimizations

**Use case:** Testing with IntelliJ platform features

---

### 3. Build Distribution ZIP (Marketplace)

```bash
./gradlew buildPlugin
```

**Output:** `build/distributions/domain-class-generator-1.0.0.zip`

**What it contains:**
- Complete plugin package
- All necessary JARs
- Searchable options
- Ready for marketplace upload

**Use case:** Publishing to JetBrains Marketplace

---

### 4. Build Everything (Clean Build)

```bash
./gradlew clean build buildPlugin
```

**Outputs:**
- `build/libs/*.jar` - All JAR variants
- `build/distributions/*.zip` - Distribution package

**Use case:** Complete rebuild before release

---

## 📂 Build Artifacts Location

After building, your artifacts are located here:

```
build/
├── libs/
│   ├── domain-class-generator-1.0.0.jar              # Basic JAR
│   ├── domain-class-generator-1.0.0-base.jar         # Base classes
│   ├── domain-class-generator-1.0.0-instrumented.jar # Instrumented code
│   └── domain-class-generator-1.0.0-searchableOptions.jar  # Search index
├── distributions/
│   └── domain-class-generator-1.0.0.zip              # Marketplace ZIP ⭐
└── tmp/
    └── composedJar/
        └── domain-class-generator-1.0.0.jar          # Composed JAR
```

---

## 🎯 Which Build to Use?

| Task | Command | Artifact |
|------|---------|----------|
| **Quick compile check** | `./gradlew jar` | `build/libs/*.jar` |
| **Local testing** | `./gradlew runIde` | Sandbox IDE |
| **Manual installation** | `./gradlew buildPlugin` | `build/distributions/*.zip` |
| **Marketplace upload** | `./gradlew buildPlugin` | `build/distributions/*.zip` ⭐ |
| **Development** | `./gradlew build` | All artifacts |

---

## 🚀 Complete Build & Test Workflow

### Step 1: Clean Build
```bash
./gradlew clean
```

### Step 2: Compile & Test
```bash
./gradlew build
```

### Step 3: Test in IDE
```bash
./gradlew runIde
```

### Step 4: Build Distribution
```bash
./gradlew buildPlugin
```

### Step 5: Verify
```bash
# Check the ZIP was created
dir build\distributions
# Should show: domain-class-generator-1.0.0.zip
```

---

## 📦 Manual Plugin Installation (For Testing)

### Option 1: Install from ZIP
1. Build: `./gradlew buildPlugin`
2. In IntelliJ IDEA: **Settings → Plugins**
3. Click ⚙️ → **Install Plugin from Disk...**
4. Select: `build/distributions/domain-class-generator-1.0.0.zip`
5. Restart IDE

### Option 2: Install from JAR (Advanced)
1. Build: `./gradlew instrumentedJar`
2. Copy JAR to IntelliJ plugins directory:
   - **Windows:** `%APPDATA%\JetBrains\IntelliJIdea2025.1\plugins\`
   - **macOS:** `~/Library/Application Support/JetBrains/IntelliJIdea2025.1/plugins/`
   - **Linux:** `~/.local/share/JetBrains/IntelliJIdea2025.1/plugins/`
3. Restart IDE

---

## 🔧 Gradle Tasks Reference

### Essential Tasks

| Task | Description |
|------|-------------|
| `clean` | Delete build directory |
| `compileKotlin` | Compile Kotlin source code |
| `classes` | Compile all classes |
| `processResources` | Copy resources (plugin.xml, etc.) |
| `jar` | Build basic JAR file |
| `instrumentCode` | Instrument classes for IntelliJ Platform |
| `instrumentedJar` | Build instrumented JAR |
| `composedJar` | Build composed JAR with all dependencies |
| `buildSearchableOptions` | Generate search index |
| `buildPlugin` | Build distribution ZIP |
| `runIde` | Launch IDE with plugin installed |
| `test` | Run tests |

### View All Tasks
```bash
./gradlew tasks
```

### View Task Dependencies
```bash
./gradlew buildPlugin --dry-run
```

---

## 🐛 Troubleshooting

### Build Fails - "Cannot find symbol"
```bash
# Clean and rebuild
./gradlew clean build
```

### JAR Missing Classes
```bash
# Use instrumented JAR instead
./gradlew instrumentedJar
```

### ZIP File Too Large
```bash
# Check what's included
unzip -l build/distributions/domain-class-generator-1.0.0.zip
```

### Plugin Won't Load in IDE
```bash
# Verify plugin structure
./gradlew buildPlugin
# Check plugin.xml is in META-INF
unzip -l build/distributions/*.zip | grep plugin.xml
```

---

## 📊 Build Output Sizes (Approximate)

| Artifact | Size | Notes |
|----------|------|-------|
| Basic JAR | ~50-100 KB | Compiled code only |
| Instrumented JAR | ~100-150 KB | With IntelliJ instrumentation |
| Distribution ZIP | ~150-300 KB | Complete package with search index |

---

## 🎓 Understanding JAR Variants

### domain-class-generator-1.0.0-base.jar
- Raw compiled classes
- No instrumentation
- Minimal size

### domain-class-generator-1.0.0.jar
- Standard JAR with resources
- Includes plugin.xml
- Suitable for basic usage

### domain-class-generator-1.0.0-instrumented.jar
- **Recommended for production**
- IntelliJ Platform instrumentation applied
- PSI classes enhanced
- Better runtime performance

### domain-class-generator-1.0.0-searchableOptions.jar
- Search index for Settings
- Makes plugin searchable in IDE
- Included in distribution ZIP

---

## 🚢 Release Build Checklist

Before building for release:

- [ ] Version updated in `build.gradle.kts`
- [ ] `plugin.xml` metadata correct
- [ ] CHANGELOG.md updated
- [ ] All tests pass: `./gradlew test`
- [ ] Clean build: `./gradlew clean`
- [ ] Full build: `./gradlew build`
- [ ] Distribution created: `./gradlew buildPlugin`
- [ ] ZIP file exists: `build/distributions/*.zip`
- [ ] Tested in sandbox: `./gradlew runIde`

---

## 💡 Pro Tips

### Speed Up Builds
```bash
# Add to gradle.properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true
```

### Build Specific Variant
```bash
# Just the JAR
./gradlew jar

# Just instrumented
./gradlew instrumentedJar

# Just the ZIP
./gradlew buildPlugin
```

### Continuous Build (Watch Mode)
```bash
./gradlew build --continuous
```

### Offline Build (Use Cache)
```bash
./gradlew build --offline
```

---

## 📝 Quick Reference

### Most Common Commands

```bash
# Development
./gradlew runIde              # Test in sandbox IDE

# Local Testing
./gradlew buildPlugin         # Build ZIP for manual install

# Release
./gradlew clean build buildPlugin  # Full release build
```

### Distribution ZIP is Located:
```
build/distributions/domain-class-generator-1.0.0.zip
```

### JARs are Located:
```
build/libs/domain-class-generator-1.0.0*.jar
```

---

## 🎯 Quick Answer

**"How do I build a plugin JAR?"**

```bash
./gradlew jar
```

**Output:** `build/libs/domain-class-generator-1.0.0.jar`

**"How do I build for marketplace?"**

```bash
./gradlew buildPlugin
```

**Output:** `build/distributions/domain-class-generator-1.0.0.zip` ⭐

---

**That's it!** You now know how to build the plugin in all formats. For marketplace release, always use the **ZIP file** from `buildPlugin`.
