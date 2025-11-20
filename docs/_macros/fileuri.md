---
layout: default
class: Macro
title: fileuri ';' PATH
summary: Return a file uri for the specified path. Relative paths are resolved against the domain processor base.
---

## Summary

Convert a file path to a proper file URI (e.g., `file:///path/to/file`).

## Syntax

    ${fileuri;<path>}

## Parameters

- **path**: File path to convert to a URI (can be relative or absolute)

## Behavior

The macro:
1. Resolves relative paths against the processor's base directory
2. Converts the path to a canonical file (resolves symbolic links, normalizes)
3. Returns the file URI representation

The resulting URI follows the `file:///` protocol format with proper escaping of special characters.

The base directory depends on context:
- In a project's `bnd.bnd`: Resolved against the project directory
- In a workspace's `cnf/build.bnd`: Resolved against the workspace directory

## Examples

```
# Current directory URI
${fileuri;.}
# In project: file:///workspace/my.project/
# In workspace: file:///workspace/

# Relative path (resolved against base directory)
${fileuri;src/main/resources/config.xml}
# Returns: file:///project/base/src/main/resources/config.xml

# Absolute path
${fileuri;/home/user/file.txt}
# Returns: file:///home/user/file.txt

# Use in manifest for resource references
Bundle-Resource: ${fileuri;${workspace}/templates/default.html}

# Reference external files
X-Config-Location: ${fileuri;../config/settings.properties}

# Handle spaces and special characters (automatically escaped)
${fileuri;/path/with spaces/file (1).txt}
# Returns: file:///path/with%20spaces/file%20(1).txt
```

## Use Cases

1. **Manifest Headers**: Create proper file URIs for bundle manifest headers
2. **External References**: Reference files outside the bundle with proper URIs
3. **Configuration**: Specify file locations in configuration that requires URIs
4. **Cross-Platform Paths**: Generate platform-independent file URIs
5. **Resource Location**: Document file resource locations in metadata

## Notes

- Relative paths are resolved against the base directory of the processor (project or workspace root)
- The path is canonicalized before conversion (symlinks resolved, `.` and `..` removed)
- Special characters in the path are properly URI-encoded
- The resulting URI uses forward slashes regardless of the platform
- On Windows, drive letters are included in the URI (e.g., `file:///C:/path/file.txt`)

## Related Macros

- [uri](uri.html) - More general URI handling
- [path](path.html) - Convert file paths to absolute paths
- [basedir](basedir.html) - Get the base directory path




---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
