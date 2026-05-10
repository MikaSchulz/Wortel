# AGENTS.md - Wortel Project Guide

## Project Overview
**Wortel** is a Java application built with Gradle (Kotlin DSL). It's organized under the `org.example` package with a minimal initial structure consisting of a `Main` class that serves as the entry point.

- **Build System**: Gradle with Kotlin DSL (`build.gradle.kts`)
- **Language**: Java
- **Package Root**: `org.example`
- **Version**: 1.0-SNAPSHOT
- **Repository**: Maven Central

## Architecture & Key Files

### Project Structure
```
src/main/java/org/example/     - Application source code
src/main/resources/            - Resource files (currently empty)
src/test/java/                 - Test source code
build.gradle.kts               - Build configuration (dependencies, plugins, tasks)
settings.gradle.kts            - Project name configuration
gradle/wrapper/                - Gradle wrapper for consistent builds across environments
```

### Main Components
- **Main.java**: Entry point with a basic `main()` method. Currently references a custom `IO` class wrapper for output (intended for abstraction of println operations).

## Build & Development Workflow

### Essential Commands
| Task | Command |
|------|---------|
| Clean build | `./gradlew clean build` |
| Compile only | `./gradlew compileJava` |
| Run tests | `./gradlew test` |
| Run application | `./gradlew run` (requires `application` plugin or custom run task) |
| Build without tests | `./gradlew build -x test` |

**Note**: The project currently lacks a `run` task or `application` plugin. If adding one, use the Gradle Application Plugin in `build.gradle.kts`.

### Testing Framework
- **Framework**: JUnit 5 (Jupiter)
- **Test Location**: `src/test/java/org/example/`
- **Dependency Management**: Tests are managed via `testImplementation` and `testRuntimeOnly`
- **Test Execution**: Configured with `useJUnitPlatform()` in the test task

## Key Patterns & Conventions

### Adding Dependencies
Use `build.gradle.kts` with:
```kotlin
dependencies {
    implementation("group:artifact:version")           // Main dependencies
    testImplementation("group:artifact:version")       // Test-only dependencies
}
```
Always use Maven Central repository (pre-configured in `repositories`). Version pinning via Gradle BOM is supported.

### Code Organization
- Classes are organized in `org.example` package
- Follow standard Java naming: PascalCase for classes, camelCase for methods/variables
- Utility classes (like `IO`) should be placed in `src/main/java/org/example/` and provide abstracted operations (e.g., `IO.println()` wraps System.out)

### Build Configuration
- **Kotlin DSL**: Project uses modern Gradle Kotlin DSL syntax (`.kts` files)
- **No subprojects**: Currently a single-module project
- **Minimal plugins**: Only `java` plugin is applied; add more as features expand

## Common Tasks for Agents

### Adding a New Feature
1. Create classes in `src/main/java/org/example/`
2. Write corresponding tests in `src/test/java/org/example/`
3. Run `./gradlew test` to validate
4. If new external dependency needed, add to `build.gradle.kts` and run `./gradlew refresh`

### Implementing the IO Utility
The `Main` class references `IO.println()` but the class doesn't exist. When implementing:
- Create `src/main/java/org/example/IO.java`
- Provide static methods: `println(String msg)` → calls `System.out.println()`
- This abstraction allows easier testing and logging integration later

### IDE Configuration
- **Primary IDE**: IntelliJ IDEA (evidenced by `.idea/` config)
- **Gradle Integration**: IntelliJ handles Gradle wrapper automatically
- The project includes Gradle wrapper (`./gradlew`) for cross-platform scripting

## Development Considerations

- **No external dependencies yet**: Keep it minimal when adding libraries; justify each dependency
- **Single module scope**: This is a standalone application, not a multi-module project
- **Test isolation**: Each test should be independent; use `testImplementation` for test-only libraries
- **Gradle Wrapper Usage**: Always use `./gradlew` instead of global `gradle` command to ensure version consistency

## External Integrations
Currently no external integrations. When adding external libraries (APIs, databases, logging), update `build.gradle.kts` and document the integration in project documentation.

