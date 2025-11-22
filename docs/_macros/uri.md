---
layout: default
class: Processor
title: uri ';' URI (';' URI)?
summary: Resolve a uri against a base uri.
---

## Summary

Resolve a URI against a base URI, handling relative URIs and file scheme URIs appropriately.

## Syntax

    ${uri;<uri>[;<base-uri>]}

## Parameters

- **uri**: URI to resolve (can be relative or absolute)
- **base-uri**: (Optional) Base URI to resolve against. Defaults to the processor's base URI.

## Behavior

The macro resolves URIs based on these rules:
1. If the URI is absolute (has a scheme like `http:`, `https:`) **and not** a `file:` scheme, returns it unchanged
2. If the URI is relative or uses the `file:` scheme:
   - Resolves it against the specified base URI (if provided)
   - Or resolves it against the processor's base URI (project or workspace root)
3. Returns the resolved URI

The base URI depends on context when not explicitly provided:
- In a project's `bnd.bnd`: Project directory URI
- In a workspace's `cnf/build.bnd`: Workspace directory URI

## Examples

```
# Current directory URI
${uri;.}
# In project: file:///workspace/my.project/
# In workspace: file:///workspace/

# Resolve relative path
${uri;src/main/resources}
# Returns: file:///project/base/src/main/resources

# Resolve against custom base
${uri;config/settings.xml;file:///opt/app/}
# Returns: file:///opt/app/config/settings.xml

# Absolute HTTP URI (returned unchanged)
${uri;https://example.com/resource}
# Returns: https://example.com/resource

# File URI (resolved against base)
${uri;file:./local/path}
# Returns: file:///project/base/local/path

# Parent directory reference
${uri;../config}
# Resolved relative to base

# Use in configuration
Bundle-DocURL: ${uri;docs/index.html;${Bundle-Site}}

# Reference workspace files
config-location = ${uri;cnf/config.properties}
```

## Use Cases

1. **URI Resolution**: Convert relative URIs to absolute
2. **Configuration**: Resolve configuration file locations
3. **Documentation**: Create proper URIs for documentation references
4. **Resource Linking**: Link to external or internal resources
5. **Cross-Platform**: Generate platform-independent URIs

## Notes

- Absolute URIs (except `file:`) are returned unchanged
- Relative URIs and `file:` scheme URIs are resolved against base
- Default base is the processor's base (project or workspace root)
- Handles `.` (current) and `..` (parent) directory references
- Result is a proper URI with appropriate scheme
- For file-specific URI conversion, see [fileuri](fileuri.html)

## Related Macros

- [fileuri](fileuri.html) - Convert file paths to file URIs
- [path](path.html) - Convert to absolute file paths
- [basedir](basedir.html) - Get the base directory path


<hr />
TODO Needs review - AI Generated content

---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
