# Code style

The project uses EditorConfig and Spotless to keep formatting consistent.

## General rules

- UTF-8 encoding and LF line endings.
- A newline at the end of every file.
- No trailing whitespace (Markdown is excluded to preserve intentional line breaks).
- Java uses 4 spaces for indentation.
- HTML, CSS, JavaScript, JSON, YAML, and XML use 2 spaces for indentation.

## Java formatting

Java sources are formatted with Google Java Format in AOSP mode so Java indentation
uses 4 spaces. Spotless also removes unused imports. Formatting is mechanical and
must not change application behavior.

Apply formatting:

```shell
./gradlew spotlessApply
```

Check formatting without changing files:

```shell
./gradlew spotlessCheck
```

Run the complete test suite and create the JaCoCo report:

```shell
./gradlew clean test jacocoTestReport
```

The HTML coverage report is generated at
`build/reports/jacoco/test/html/index.html`.

The build enforces a minimum overall line coverage of 75% through
`jacocoTestCoverageVerification`.
