---
layout: default
class: Macro
title: cat ';' FILEPATH
summary: Read and return the contents of a file or URL
---

## Summary

The `cat` macro reads the contents of a file, directory, or URL and returns it as a string. It's similar to the Unix `cat` command, allowing you to embed file contents directly in your configuration.

## Syntax

```
${cat;<path-or-url>}
```

## Parameters

- `path-or-url` - Path to a file or directory (relative to project), or a URL

## Behavior

- **For files**: Returns the entire file contents as a string, with backslashes escaped
- **For directories**: Returns a list of filenames in the directory
- **For URLs**: Downloads and returns the content (assumes UTF-8 encoding)
- File paths are resolved relative to the project directory
- Returns null if the path doesn't exist and isn't a valid URL

## Examples

Read a text file:
```
license.text=${cat;LICENSE.txt}
```

Read a property file:
```
build.info=${cat;build.properties}
```

List directory contents:
```
resource.files=${cat;resources}
```

Fetch content from a URL:
```
remote.config=${cat;https://example.com/config.txt}
```

Embed a template:
```
header=${cat;templates/header.txt}
```

## Use Cases

- Embedding license text in bundle manifests
- Reading version information from external files
- Loading configuration templates
- Fetching remote configuration or data
- Including file contents in property values
- Listing files in a directory for processing

## Notes

- Backslashes in file content are automatically escaped (useful for Windows paths)
- For large files, consider using file references instead of embedding
- URL fetching requires network access and may fail in restricted environments
- Directory listing returns a simple list of filenames
- UTF-8 encoding is assumed for both files and URLs


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
