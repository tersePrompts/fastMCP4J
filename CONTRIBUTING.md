# Contributing to FastMCP4J

Thank you for your interest in contributing to FastMCP4J. This document provides guidelines for contributing to the project.

## Development Environment

### Prerequisites

- **Java 17+** - The project requires Java 17 or higher
- **Maven 3.8+** - Build and dependency management

### Setting Up

1. Clone the repository:
   ```bash
   git clone https://github.com/tersePrompts/fastMCP4J.git
   cd fastMCP4J
   ```

2. Build the project:
   ```bash
   mvn clean install
   ```

3. Run tests to verify your setup:
   ```bash
   mvn test
   ```

## Running Tests

Run the full test suite:
```bash
mvn test
```

Run a specific test class:
```bash
mvn test -Dtest=FastMCPTest
```

Run a specific test method:
```bash
mvn test -Dtest=FastMCPTest#testBuildServerFromClass
```

## Code Style

FastMCP4J follows a concise, pragmatic coding style:

### General Principles

- **Terse, direct code** - Prefer concise naming over verbose descriptions
- **Lombok everywhere** - Use `@Data`, `@Builder`, `@Value`, `@Slf4j` to reduce boilerplate
- **No comments** - Comments only when logic is genuinely non-obvious
- **Composition over inheritance** - Favor composition patterns
- **Immutable by default** - Use records and `@Value` for data classes

### Code Structure

- Pure Java 17+ (no Spring, no Jakarta EE)
- Stateless design (no session management, no state machines)
- Minimal scaffolding - avoid abstract base classes unless necessary
- Each public class has a corresponding test class

### Example

```java
@Value
public class ToolMeta {
    String name;
    String description;
    Method method;
    boolean async;
}
```

### Testing Guidelines

- Use JUnit 5
- No mocking frameworks - use real instances
- Test names follow the pattern: `testMethodName_Scenario_ExpectedBehavior()`
- Every feature file should have a corresponding test file

## Pull Request Process

1. **Fork the repository** and create a feature branch from `main`
2. **Make your changes** following the code style guidelines
3. **Add tests** for new functionality or bug fixes
4. **Ensure all tests pass**: `mvn test`
5. **Update documentation** if your changes affect user-facing behavior
6. **Commit your changes** with clear, concise messages
7. **Push to your fork** and submit a pull request

### Pull Request Guidelines

- Keep PRs focused and minimal
- Reference related issues in the description
- Ensure CI checks pass before requesting review
- Be responsive to review feedback

## Code of Conduct

### Our Pledge

We are committed to providing a welcoming and inclusive environment for all contributors.

### Our Standards

- Be respectful and considerate
- Use welcoming and inclusive language
- Be constructive when providing feedback
- Focus on what is best for the community

### Enforcement

Project maintainers reserve the right to remove or edit comments that violate these standards. Instances of abusive behavior may be reported to maintainers for review.

## Areas of Focus

We welcome contributions in these areas:

- Authentication/authorization
- MCP Providers and transforms
- Storage backends
- Additional transport options
- Documentation improvements
- Bug fixes and performance improvements

## License

By contributing to FastMCP4J, you agree that your contributions will be licensed under the MIT License.
