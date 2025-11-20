---
layout: default
class: Macro
title: toclassname ';' FILES
summary: Convert file paths to fully qualified class names
---

## Summary

The `toclassname` macro converts a list of file paths (`.class` or `.java` files) to fully qualified class names by removing the extension and converting path separators to dots.

## Syntax

```
${toclassname;<file-paths>}
```

## Parameters

- `file-paths` - Comma-separated list of file paths ending in `.class` or `.java`

## Behavior

- Removes `.class` or `.java` extension
- Converts path separators (`/`) to dots (`.`)
- Generates warning for files without proper extension
- Filters out invalid paths
- Returns comma-separated list of class names

## Examples

Convert class file paths:
```
${toclassname;com/example/Main.class,com/example/Util.class}
# Returns: "com.example.Main,com.example.Util"
```

Convert Java source paths:
```
${toclassname;org/test/TestCase.java}
# Returns: "org.test.TestCase"
```

Use with file lists:
```
classes=${toclassname;${lsr;bin;*.class}}
```

Mixed extensions:
```
${toclassname;com/Foo.java,com/Bar.class}
# Returns: "com.Foo,com.Bar"
```

## Use Cases

- Converting build output paths to class names
- Generating class lists for manifests
- Processing compilation results
- Creating package lists from files
- Build automation and scripting
- Class path analysis

## Notes

- Only processes `.class` and `.java` files
- Invalid paths generate warnings but don't fail
- Path separator is always `/` (converted to `.`)
- Returns FQN (Fully Qualified Names)
- See also: `${toclasspath}` for inverse operation



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
