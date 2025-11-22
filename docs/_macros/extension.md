---
layout: default
class: Macro
title: extension ';' PATH
summary: The file extension of the given path or empty string if no extension
---

## Summary

Extract the file extension from a file path, returning the part after the last dot in the filename.

## Syntax

    ${extension;<path>}

## Parameters

- **path**: File path from which to extract the extension

## Behavior

The macro:
1. Normalizes the path (handles different path separators)
2. Extracts the last segment (filename) from the path
3. Returns the part after the last `.` in the filename
4. Returns an empty string if no extension is present

The extension is returned **without** the leading dot.

## Examples

```
# Extract extension from file path
${extension;myfile.txt}                    # Returns: txt
${extension;/path/to/document.pdf}        # Returns: pdf
${extension;archive.tar.gz}               # Returns: gz (only last extension)

# No extension cases
${extension;README}                        # Returns: (empty)
${extension;/path/to/folder/}             # Returns: (empty)

# Multiple dots
${extension;my.file.name.java}            # Returns: java

# Original examples
${extension;abcdef.def}                   # Returns: def
${extension;/foo.bar/abcdefxyz.def}      # Returns: def
${extension;abcdefxyz}                    # Returns: (empty)
${extension;/foo.bar/abcdefxyz}          # Returns: (empty)

# Used with other macros
-include ${extension;${workspace}/.project} == project
```

## Use Cases

1. **File Type Detection**: Determine the type of file being processed
2. **Conditional Logic**: Apply different processing based on file extension
3. **File Filtering**: Select files based on their extensions
4. **Path Construction**: Build new paths with modified extensions

## Related Macros

- [basenameext](basenameext.html) - Extract basename, optionally removing an extension
- [stem](stem.html) - Get filename without extension
- [basename](basename.html) - Get the last path segment



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
