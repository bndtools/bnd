---
layout: default
class: Macro
title: foreach ';' MACRO (';' LIST)* 
summary: Iterate over a list, calling a macro for each element with value and index
---

## Summary

The `foreach` macro iterates over one or more lists and invokes a specified macro for each element, passing both the element value and its index (0-based) as arguments. The results are collected and returned as a comma-separated list.

## Syntax

```
${foreach;<macro-name>[;<list>...]}
```

## Parameters

- `macro-name` - Name of a macro to invoke for each element (without the `${}` wrapper)
- `list` - One or more semicolon-separated lists to iterate over

## Behavior

- Combines all provided lists into a single list
- Iterates through each element with a 0-based index
- For each element, invokes: `${<macro-name>;<element>;<index>}`
- Collects all macro results
- Returns results as a comma-separated string

## Examples

Transform each element:
```
# Define a macro to wrap elements
my-macro;$1-modified

# Use foreach
${foreach;my-macro;apple,banana,cherry}
# Invokes: ${my-macro;apple;0}, ${my-macro;banana;1}, ${my-macro;cherry;2}
```

Number items in a list:
```
# Define numbering macro
num;$2: $1

${foreach;num;first,second,third}
# Returns: "0: first,1: second,2: third"
```

Process file list:
```
# Define file processing macro
process-file;processed-$1

${foreach;process-file;${lsr;src;*.java}}
```

Generate indexed output:
```
# Define template macro
item-template;<item index="$2">$1</item>

${foreach;item-template;red,green,blue}
# Returns XML items with indices
```

## Use Cases

- Transforming lists with complex logic
- Generating indexed output
- Applying templates to list elements
- Creating numbered or labeled items
- Building structured output from lists
- Mapping values with context (index)

## Notes

- The invoked macro receives two arguments: the value and the index (0-based)
- The macro must be defined before use (either as a property or built-in macro)
- Results are joined with commas
- Index starts at 0 for the first element
- Empty lists produce empty results
- See also: `${map}` for simpler transformations without index
- See also: `${apply}` for passing entire lists to a macro

---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
