---
layout: default
class: Macro
title: map ';' MACRO (';' LIST)* 
summary: Transform each element of a list using a macro function
---

## Summary

The `map` macro applies a transformation function (specified as a macro name) to each element of one or more lists, collecting the results into a new comma-separated list. This is similar to the functional programming map operation.

## Syntax

```
${map;<macro-name>[;<list>...]}
```

## Parameters

- `macro-name` - Name of a macro to invoke for each element (without the `${}` wrapper)
- `list` - One or more semicolon-separated lists to process

## Behavior

- Combines all provided lists into a single list
- For each element, invokes: `${<macro-name>;<element>}`
- Collects all transformation results
- Returns results as a comma-separated string

## Examples

Transform list elements:
```
# Define transformation macro
upper;${toupper;$1}

# Map to uppercase
${map;upper;apple,banana,cherry}
```

Add prefix to each element:
```
# Define prefix macro
add-prefix;modified-$1

${map;add-prefix;file1,file2,file3}
# Returns: "modified-file1,modified-file2,modified-file3"
```

Process file list:
```
# Define file processor
process;${basename;$1}

${map;process;/path/to/file1.jar;/path/to/file2.jar}
# Returns basenames of files
```

Calculate transformations:
```
# Define calculation macro
double;${multiply;$1;2}

${map;double;1,2,3,4,5}
# Returns: "2,4,6,8,10"
```

## Use Cases

- Transforming lists with complex logic
- Applying consistent transformations to all elements
- Building derived lists from source data
- Functional-style list processing
- Batch operations on list elements
- Data transformation pipelines

## Notes

- The invoked macro receives one argument: the element value
- Unlike `${foreach}`, no index is passed to the macro
- The macro must be defined before use (either as a property or built-in)
- Results are joined with commas
- Empty lists produce empty results
- See also: `${foreach}` for transformations that need the element index
- See also: `${apply}` for passing entire lists to a macro



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
