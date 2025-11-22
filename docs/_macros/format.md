---
layout: default
class: Macro
title: format ';' STRING (';' ANY )*
summary: Print a formatted string using Locale.ROOT, automatically converting variables to the specified format if possible.
---

## Summary

Format a string using printf-style format specifiers with provided arguments, similar to Java's `String.format()`.

## Syntax

    ${format;<format-string>[;<argument>...]}

## Parameters

- **format-string**: A format string with printf-style format specifiers (e.g., `%s`, `%d`, `%f`, `%tF`)
- **argument...**: Zero or more arguments to substitute into the format string

## Behavior

The macro:
1. Processes the format string using Java's `Formatter` syntax
2. Uses `Locale.ROOT` for build reproducibility
3. Automatically converts string arguments to appropriate types based on format specifiers
4. Expands arguments that look like macros (starting with `${`)
5. Supports conditional formatting with `%{<condition>;<true>;<false>}`

### Conditional Syntax

    %{<condition>;<true-value>;<false-value>}

Where `<condition>` is evaluated as truthy (non-empty and not "false").

## Examples

```
# Basic string formatting
${format;Bundle-Version %s;1.0.0}
# Returns: Bundle-Version 1.0.0

# Multiple arguments
${format;%s-%s-%s;com;example;bundle}
# Returns: com-example-bundle

# Number formatting
${format;Port number: %d;8080}
# Returns: Port number: 8080

# Decimal formatting
${format;Progress: %.2f%%;75.5}
# Returns: Progress: 75.50%

# Date/time formatting
${format;%tF;${tstamp}}
# Returns: 2024-01-15 (ISO date format)

${format;%tY-%tm-%td;${tstamp}}
# Returns: 2024-01-15 (custom date format)

# Conditional formatting
${format;%{${debug}};DEBUG-MODE;RELEASE-MODE}
# Returns: DEBUG-MODE if ${debug} is set, otherwise RELEASE-MODE

# Width and padding
${format;ID: %05d;42}
# Returns: ID: 00042

# Combining with other macros
${format;Built on %tF at %tT;${now};${now}}
# Returns: Built on 2024-01-15 at 14:30:45
```

## Format Specifiers

Common format specifiers include:

- `%s` - String
- `%d` - Decimal integer
- `%f` - Floating point
- `%x` - Hexadecimal
- `%o` - Octal
- `%b` - Boolean
- `%tF` - ISO date (yyyy-MM-dd)
- `%tT` - Time (HH:mm:ss)
- `%tY`, `%tm`, `%td` - Year, month, day components

See Java's `Formatter` documentation for complete format specifier syntax.

## Use Cases

1. **Version Strings**: Create formatted version identifiers
2. **Manifest Headers**: Generate complex manifest header values
3. **Date/Time Stamps**: Format timestamps in various formats
4. **Numeric IDs**: Format numbers with padding or specific precision
5. **Conditional Text**: Generate different text based on build properties
6. **Reproducible Builds**: Format values consistently using Locale.ROOT

## Notes

- Uses `Locale.ROOT` for consistent, reproducible formatting across environments
- Follows Java's `String.format()` and `Formatter` syntax
- Arguments are automatically converted to appropriate types
- Arguments that start with `${` are expanded as macros first
- Conditional syntax `%{...}` is bnd-specific (not standard Java)
- For date/time formatting, combine with [tstamp](tstamp.html) or [now](now.html)

## Related Macros

- [tstamp](tstamp.html) - Get timestamp in milliseconds
- [now](now.html) - Get current date/time
- [long2date](long2date.html) - Convert timestamp to formatted date
- [if](if.html) - Conditional macro (simpler alternative for boolean conditions)


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
