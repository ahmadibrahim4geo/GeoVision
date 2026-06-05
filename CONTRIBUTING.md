# Contributing to GEO Vision

Thank you for your interest in contributing to GEO Vision! We appreciate your help in making this project better.

## Table of Contents
- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Contribution Process](#contribution-process)
- [Code Style Guide](#code-style-guide)
- [Testing Requirements](#testing-requirements)
- [Reporting Bugs](#reporting-bugs)
- [Requesting Features](#requesting-features)

---

## Code of Conduct

This project adheres to a [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

### Our Pledge
We are committed to providing a welcoming and inclusive environment for all contributors, regardless of age, body size, disability, ethnicity, gender identity and expression, level of experience, nationality, personal appearance, race, religion, or sexual identity and orientation.

---

## Getting Started

### Prerequisites
- **Kotlin 2.0.21+**
- **Java JDK 17+**
- **Android SDK 26+** (minimum)
- **Android Studio 2024.1+** (recommended)
- **Git**

### Fork & Clone
```bash
# 1. Fork the repository on GitHub
# 2. Clone your fork locally
git clone https://github.com/YOUR-USERNAME/GEO-Vision.git
cd GEO-Vision

# 3. Add upstream remote
git remote add upstream https://github.com/ORIGINAL-OWNER/GEO-Vision.git
```

---

## Development Setup

### 1. Configure Local Properties
```bash
# Create local.properties file
touch local.properties

# Add your Android SDK path:
echo "sdk.dir=/path/to/Android/SDK" >> local.properties

# For ESRI variant, add (optional):
echo "arcgis.key=YOUR_ARCGIS_LICENSE_KEY" >> local.properties
```

### 2. Build the Project
```bash
# Build debug variant
./gradlew build

# Or build specific variant
./gradlew buildDebugCore      # Core without ESRI
./gradlew buildDebugEsri      # With ESRI/ArcGIS
```

### 3. Run Tests
```bash
# Unit tests
./gradlew test

# Android instrumented tests
./gradlew connectedAndroidTest

# With coverage report
./gradlew testDebugUnitTest --project-prop kotlinCoverage=true
```

### 4. Set Up IDE
**Android Studio:**
1. Open project → File → Open → Select GEO-Vision folder
2. Wait for Gradle sync
3. Configure SDK: File → Project Structure → SDK Location
4. Enable Kotlin plugin: Preferences → Plugins

---

## Contribution Process

### Step 1: Create a Branch
```bash
# Update your fork with latest changes
git fetch upstream
git rebase upstream/main

# Create feature branch
git checkout -b feature/add-new-format-parser
# Or bugfix branch
git checkout -b bugfix/fix-memory-leak
```

### Branch Naming Conventions
- **Feature**: `feature/description-of-feature`
- **Bugfix**: `bugfix/issue-number-short-description`
- **Documentation**: `docs/description`
- **Performance**: `perf/improvement-description`
- **Refactor**: `refactor/component-name`

### Step 2: Make Your Changes
- Write clean, well-documented code
- Follow the [Code Style Guide](#code-style-guide)
- Ensure all tests pass
- Add tests for new functionality

### Step 3: Commit Your Work
```bash
# Stage changes
git add .

# Commit with descriptive message
git commit -m "Add KML style color parsing

- Implement parseKmlColor() method
- Support aabbggrr format conversion
- Add unit tests for color conversion
- Fixes #123"
```

**Commit Message Guidelines:**
- Use present tense: "Add feature" not "Added feature"
- Use imperative mood: "Move cursor" not "Moves cursor"
- Limit first line to 72 characters
- Reference issues and pull requests liberally
- Explain WHAT and WHY, not HOW

### Step 4: Push & Create Pull Request
```bash
# Push to your fork
git push origin feature/add-new-format-parser

# Then create Pull Request on GitHub
# - Title: Clear, descriptive title
# - Description: What, Why, How, Testing
# - Link related issues: "Fixes #123"
```

**Pull Request Template:**
```markdown
## Description
Brief description of changes

## Related Issues
Fixes #123
Related to #456

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Documentation update
- [ ] Performance improvement

## Testing Performed
- [ ] Unit tests added
- [ ] Manual testing on:
  - [ ] Android 8.0
  - [ ] Android 10
  - [ ] Android 15
- [ ] Large file testing (>100MB)
- [ ] Dark mode compatibility

## Screenshots (if UI change)
[Add screenshots here]

## Checklist
- [ ] Code follows style guide
- [ ] All tests pass
- [ ] Documentation updated
- [ ] No breaking changes
```

### Step 5: Respond to Review Feedback
1. Check comments from reviewers
2. Make requested changes
3. Commit changes: `git commit -m "Address review feedback"`
4. Push: `git push origin feature/...`
5. Don't force-push unless instructed

### Step 6: Merge
Once approved:
1. Maintainer will squash and merge
2. Your feature branch will be deleted
3. Celebrate! 🎉

---

## Code Style Guide

### Kotlin Standards

#### Naming Conventions
```kotlin
// Classes: PascalCase
class GeoJsonParser { }
data class FeatureRow { }

// Functions/Variables: camelCase
fun parseGeoJson() { }
val featureCount = 100

// Constants: SCREAMING_SNAKE_CASE
const val MAX_FEATURES = 100_000
private const val TAG = "GeoJsonParser"

// Enum members: UPPER_CASE
enum class ParseErrorType {
    NONE, ACCESS_DENIED, CORRUPTED
}
```

#### Code Organization
```kotlin
// File structure:
package com.geovision.mobile.data

// 1. Imports
import android.util.JsonReader
import kotlinx.coroutines.ensureActive

// 2. File-level declarations
private const val TAG = "GeoJsonParser"
private const val MAX_FEATURES = 100_000

// 3. Main class/object
object GeoJsonParser {
    // 3a. Nested data classes
    data class ParseResult(...)
    
    // 3b. Constants
    companion object {
        const val DEFAULT_CRS = "EPSG:4326"
    }
    
    // 3c. Public functions (API)
    suspend fun streamParse(...): ParseResult { }
    
    // 3d. Private functions (helpers)
    private fun validateGeometry(...) { }
}
```

#### String Handling
```kotlin
// Use raw strings for JSON/XML
val json = """
    {
        "type": "Feature",
        "geometry": { }
    }
""".trimIndent()

// Use string templates
val message = "Loaded $featureCount features"

// For Arabic/Multi-language
val label = context.getString(R.string.layer_name, name)
```

#### Error Handling
```kotlin
// Prefer try-catch with specific types
try {
    val file = File(path)
    file.readBytes()
} catch (e: FileNotFoundException) {
    AppLogger.error(TAG, "File not found", e)
} catch (e: IOException) {
    AppLogger.error(TAG, "IO error", e)
}

// Use Result type for functions
fun parseFile(file: File): Result<List<Feature>> {
    return try {
        Result.success(parser.parse(file))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

#### Comments & Documentation
```kotlin
/**
 * Parses GeoJSON file with streaming for memory efficiency.
 *
 * Reads GeoJSON token-by-token, yielding every 50 features to maintain
 * UI responsiveness. Supports up to 100K features per file.
 *
 * @param reader Input stream to parse
 * @param filePath Original file path (for logging)
 * @return ParseResult with features, CRS, and extent
 * @throws JsonException if JSON is malformed
 * @throws IOException if read error occurs
 *
 * @see GeoJsonParser.ParseResult
 */
suspend fun streamParse(reader: Reader, filePath: String = ""): ParseResult {
    // Implementation
}

// Arabic comments for internal team knowledge:
// تحسين الأداء: استخدام streaming للملفات الكبيرة
```

#### Coroutine Best Practices
```kotlin
// Always check cancellation in loops
while (hasMore) {
    coroutineContext.ensureActive()  // Check cancellation
    
    // Process
    kotlinx.coroutines.yield()  // Allow other tasks
}

// Use suspend modifiers
suspend fun parseFile(file: File): ParseResult { }

// Handle cancellation gracefully
try {
    val result = withTimeout(30.seconds) {
        parser.parse(file)
    }
} catch (e: TimeoutCancellationException) {
    AppLogger.warn(TAG, "Parse timeout")
}
```

---

## Testing Requirements

### Minimum Coverage
- **Parsers**: 85%+ coverage
- **Data Models**: 80%+ coverage  
- **UI Components**: 60%+ coverage
- **Utilities**: 90%+ coverage

### Test File Naming
```
src/test/java/com/geovision/mobile/data/GeoJsonParserTest.kt
src/androidTest/java/com/geovision/mobile/ui/MapScreenTest.kt
```

### Unit Test Template
```kotlin
import org.junit.Test
import org.junit.Before
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.*
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class GeoJsonParserTest {
    
    private lateinit var parser: GeoJsonParser
    private lateinit var context: Context
    
    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
        parser = GeoJsonParser
    }
    
    @Test
    fun testParseSampleGeoJson() {
        // Given
        val json = """{"type":"FeatureCollection","features":[]}"""
        val reader = StringReader(json)
        
        // When
        val result = runBlocking {
            parser.streamParse(reader, "test.geojson")
        }
        
        // Then
        assertThat(result.features, hasSize(0))
        assertThat(result.error, nullValue())
    }
    
    @Test
    fun testParseInvalidJson() {
        // Given
        val json = "{invalid json"
        val reader = StringReader(json)
        
        // When
        val result = runBlocking {
            parser.streamParse(reader)
        }
        
        // Then
        assertThat(result.error, notNullValue())
        assertThat(result.errorType, 
            `is`(GeoJsonParser.ParseErrorType.PARSER_ERROR))
    }
    
    @Test
    fun testParseLargeFile() {
        // Given - Create file with 50K features
        // When - Parse and time
        // Then - Verify memory usage < 200MB
    }
}
```

### Testing Checklist
Before submitting PR, verify:
- [ ] All unit tests pass: `./gradlew test`
- [ ] All instrumented tests pass: `./gradlew connectedAndroidTest`
- [ ] Code coverage acceptable: `./gradlew testDebugUnitTestCoverage`
- [ ] No lint warnings: `./gradlew lint`
- [ ] Builds successfully: `./gradlew build`
- [ ] Manual testing on real device/emulator
- [ ] Testing on multiple Android versions

---

## Reporting Bugs

### Before Creating an Issue
1. Check [existing issues](https://github.com/YOUR-USERNAME/GEO-Vision/issues)
2. Check [discussions](https://github.com/YOUR-USERNAME/GEO-Vision/discussions)
3. Update to latest version
4. Try to reproduce consistently

### Bug Report Template
```markdown
## Description
Brief description of what's not working

## Steps to Reproduce
1. Open GEO Vision
2. Load file: [specify format and size]
3. Perform action: [describe action]
4. Observe: [what happens]

## Expected Behavior
[What should happen instead]

## Screenshots / Logs
[Attach relevant screenshots or logs]

## Device Information
- Android Version: [e.g., Android 12]
- Device: [e.g., Samsung Galaxy S21]
- RAM: [e.g., 4GB]
- GEO Vision Version: [e.g., 1.0.0]

## File Information (if relevant)
- Format: [GeoJSON, Shapefile, etc.]
- Size: [e.g., 50MB]
- Feature Count: [e.g., 10,000]
- Encoding: [e.g., UTF-8]

## Additional Context
[Any other relevant information]
```

---

## Requesting Features

### Feature Request Template
```markdown
## Description
Brief description of the feature

## Motivation
Why is this feature needed? What problem does it solve?

## Proposed Solution
How should this feature work?

## Alternatives Considered
Other approaches to solving this problem

## Example Use Case
Real-world scenario where this feature helps

## Additional Context
Links to related issues, mockups, specifications
```

---

## Priority Areas for Contribution

### 🔴 High Priority
- [ ] Fix memory leaks in GdbParser
- [ ] Add comprehensive unit tests (aim for 80%+ coverage)
- [ ] Performance optimization for >100MB files
- [ ] Security audit of file path handling

### 🟠 Medium Priority
- [ ] Vector data editing
- [ ] Advanced layer styling UI
- [ ] Attribute table with filtering
- [ ] Feature search functionality

### 🟡 Low Priority
- [ ] UI/UX improvements
- [ ] Dark mode enhancements
- [ ] Additional language translations
- [ ] Documentation improvements

---

## Questions or Need Help?

- 💬 **Questions**: Use [GitHub Discussions](https://github.com/YOUR-USERNAME/GEO-Vision/discussions)
- 🐛 **Bug**: Report on [Issues](https://github.com/YOUR-USERNAME/GEO-Vision/issues)
- 📧 **Email**: [Contact information]
- 📖 **Docs**: Check [README.md](README.md) and documentation

---

Thank you for contributing to GEO Vision! 🙏

*Last Updated: January 2026*
