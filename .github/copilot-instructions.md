# GitHub Copilot Instructions for bnd/bndtools

## Repository Overview

Bnd/Bndtools is a swiss army knife for OSGi development. It creates manifest headers based on analyzing class code, verifies settings, manages project dependencies, diffs jars, and much more. The repository contains:

- **bndlib and core libraries** - The foundation for all OSGi functionality
- **Maven plugins** - Complete set of plugins for Maven users
- **Gradle plugins** - Build plugins for workspace and non-workspace projects
- **Eclipse plugins (Bndtools)** - Full GUI support for Eclipse IDE
- **Command-line tool (bnd)** - Standalone utility with extensive functionality

## Build System

### Requirements
- **Java**: Minimum Java 17 required
- **Build tools**: Gradle and Maven (wrappers included: `./gradlew` and `./mvnw`)

### Build Commands
```bash
# Build Bnd Workspace projects (must run first)
./gradlew :build

# Skip tests for faster builds
./gradlew build -x test -x testOSGi

# Build Gradle plugins
./gradlew :gradle-plugins:build

# Build Maven plugins
./mvnw install

# Publish to dist/bundles
./gradlew :publish
./gradlew :gradle-plugins:publish
./mvnw -Pdist deploy

# Full rebuild cycle (like CI)
./gradlew :build && ./gradlew :gradle-plugins:build && ./.github/scripts/rebuild-build.sh && ./.github/scripts/rebuild-test.sh
```

### Running Tests
```bash
# Run all tests
./gradlew :build

# Run specific test class
./gradlew :biz.aQute.bndall.tests:test --tests "biz.aQute.launcher.AlsoLauncherTest"

# Run single test method
./gradlew :biz.aQute.bndall.tests:test --tests "biz.aQute.launcher.AlsoLauncherTest.testTester"
```

## Code Organization

### Project Structure
- `aQute.libg` - Core library
- `biz.aQute.bnd` - Command-line tool
- `biz.aQute.bndlib` - Main bnd library
- `biz.aQute.bndlib.tests` - Tests for bndlib (historical separation)
- `biz.aQute.bnd.*` - Various bnd subsystems (ant, maven, reporter, etc.)
- `bndtools.*` - Eclipse plugin components
- `gradle-plugins/` - Gradle plugin implementations
- `maven-plugins/` - Maven plugin implementations
- `docs/` - Documentation (Jekyll-based)
- `cnf/` - Bnd workspace configuration

### Module Naming Convention
Modules follow the pattern: `biz.aQute.<functionality>` or `bndtools.<functionality>`

## Coding Standards

### Testing Best Practices

#### Soft Assertions
Use `org.assertj.core.api.SoftAssertions` with `SoftAssertionsExtension` for better test diagnostics:

```java
@ExtendWith(SoftAssertionsExtension.class)
public class MyTest {
    @InjectSoftAssertions
    SoftAssertions softly;
    
    @Test
    public void test1() {
        softly.assertThat("foo bar")
            .contains("bar")
            .doesNotContain("baz");
    }
}
```

**Benefits**: Collects all assertion errors instead of stopping at the first one, especially useful for long tests.

**Limitations**: Works well when assertions don't depend on each other. Mix with hard assertions when needed.

#### Temporary Directories
Use `@InjectTemporaryDirectory` annotation for test temporary folders:

```java
import aQute.bnd.test.jupiter.InjectTemporaryDirectory;

public class MyTest {
    @InjectTemporaryDirectory
    File tmp;
    
    @Test
    public void test1() {
        // Use tmp directory
    }
}
```

Each test method gets a distinct temporary folder that's automatically cleaned up.

#### Collection Assertions
Assert directly on collections for better diagnostics:

```java
// Good: Better error messages
softly.assertThat(warnings).as("i expect warnings").containsExactly("bar");

// Avoid: Less informative
softly.assertThat(warnings.size()).as("warning count").isEqualTo(1);
```

### Java Coding Conventions
- Use Eclipse `.settings` for formatting (included in projects)
- Avoid unnecessary whitespace changes
- Write code compatible with Java 17
- Follow existing code style in the file

### Commit Message Format
```
Short summary (max 50 chars) in imperative mood

Optional detailed explanation separated by empty line.
Include context and reasoning for changes.

Fixes #123
```

## API Compatibility

- **Strict backward compatibility** - Thousands of test cases verify this
- **OSGi semantic versioning** - Package versions marked explicitly
- **Baselining tool** - Used to prevent breaking changes
- Major versions kept to minimum

### Current Branches
- `master` - Main development branch (Java 17+)
- `classic` - Continuation of 6.4 on Java 1.8
- `next` - Release branch to Maven Central

## Eclipse Plugin Development

### Launching Bndtools
Use `.bndrun` files from `bndtools.core` project:
- `bndtools.cocoa.macosx.x86_64.bndrun` - macOS Intel
- `bndtools.cocoa.macosx.aarch64.bndrun` - macOS Apple Silicon
- `bndtools.gtk.linux.x86_64.bndrun` - Linux
- `bndtools.win32.x86.bndrun` - Windows

Right-click → "Run As" → "Bnd OSGi Run Launcher"

### Adding Error/Warning Markers
1. Customize bnd error with Location details object
2. Create `BuildErrorDetailsHandler` (extend `AbstractBuildErrorDetailsHandler`)
3. Implement `generateMarkerData()` to return MarkerData objects
4. Register in `plugin.xml` extension point

### Using ExtensionFacade
Bridge between Eclipse extension registry and OSGi Declarative Services:
- **Factory mode**: Direct instantiation of extensions
- **Proxy mode**: Dynamic proxy for lifecycle management
- Enables dynamic restart of implementation bundles without workbench restart

## Documentation

- Documentation in `docs/` directory
- Jekyll-based site at https://bnd.bndtools.org
- Instructions documented in `docs/_instructions/`
- Changes/releases documented in wiki

## Contribution Workflow

### Git Triangular Workflow
1. Fork the main repo: https://github.com/bndtools/bnd
2. Clone and set up remotes:
```bash
git clone https://github.com/bndtools/bnd.git
cd bnd
git remote add fork git@github.com:YOUR-USERNAME/bnd.git
git config remote.pushdefault fork
git config push.default simple
```
3. Create feature branch: `XXX-description` (XXX = issue number)
4. Submit pull request from your fork

### Pull Request Guidelines
- Include unit tests for changes
- Run full build before submitting
- Reference issues in PR description
- Keep commits focused and logical
- Squash commits before merge
- Sign commits with `Signed-off-by:` line

### Testing Requirements
- Submit unit tests for all changes
- Most projects have existing test cases
- `biz.aQute.bndlib` tests are in `biz.aQute.bndlib.tests` project
- All tests must pass before merge

## Common Patterns

### OSGi Bundle Development
- Manifest headers generated automatically from bytecode analysis
- Use annotations for component definitions (Declarative Services)
- Follow OSGi semantic versioning strictly

### Workspace Structure
The bnd workspace is self-contained:
- No specific external setups required
- Contains all build information
- Can be built with bnd CLI, Gradle, or Maven
- `cnf/` directory contains workspace configuration

## Helpful Commands

```bash
# Check code quality (CodeQL runs on PRs automatically)
# Rebuild with built artifacts
./.github/scripts/rebuild-build.sh
./.github/scripts/rebuild-test.sh

# Generate documentation
./.github/scripts/docs.sh

# Update Gradle wrapper
gradle wrapper --gradle-version X.XX
```

## Key Files
- `CONTRIBUTING.md` - Detailed contribution guidelines
- `DEV_README.md` - Development tips and tricks
- `README.md` - Project overview and getting started
- `build.gradle` - Root Gradle build configuration
- `pom.xml` - Maven build configuration

## Resources
- Main documentation: https://bnd.bndtools.org
- Eclipse plugin: https://bndtools.org
- Issue tracker: https://github.com/bndtools/bnd/issues
- Discourse: https://bnd.discourse.group

## Notes for Code Generation
- Prefer existing libraries over adding new dependencies
- Maintain consistency with existing code style
- Consider backward compatibility impact
- Include appropriate test coverage
- Document public APIs thoroughly
- Use soft assertions for test diagnostics
- Clean up temporary resources properly
