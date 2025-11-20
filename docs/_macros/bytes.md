---
layout: default
class: Macro
title: bytes ( ';' LONG )*
summary: Format byte a count into human-readable size unit
---

## Summary

The `bytes` macro converts a byte count (as long integer) into human-readable format with appropriate size units (b, Kb, Mb, Gb, etc.). It automatically selects the most appropriate unit by dividing by 1024 until the value is less than 1024. 
Supports formatting a list of inputs.

## Syntax

```
${bytes;<number>*}
```

## Parameters

- `number` - One or multiple numeric values representing byte counts (as long integers)

## Behavior

- Converts a byte count to the most appropriate unit (b, Kb, Mb, Gb, Tb, Pb, Eb, Zb, Yb, Bb, Geopbyte)
- Automatically selects units by repeatedly dividing by 1024
- Rounds results to one decimal place
- multiple values supported. Outputs are concatenated by space.

## Examples

Format a small byte count:
```
${bytes;1024}
# Returns: "1.0 Kb"
```

Format megabytes:
```
${bytes;5242880}
# Returns: "5.0 Mb" (5 * 1024 * 1024 bytes)
```

Format gigabytes:
```
${bytes;10737418240}
# Returns: "10.0 Gb"
```

Format multiple
```
${bytes;1048576;10000048576}
# Returns: "1.0 Mb 9.3 Gb"
```

## Use Cases

- Displaying file sizes in human-readable format
- Reporting bundle or artifact sizes
- Formatting memory or disk usage metrics
- Creating readable size reports in build logs
- Displaying download or upload sizes

## Notes

- Uses 1024 as the divisor (binary units), not 1000 (decimal units)
- Values are rounded to one decimal place
- The unit abbreviations use 'b' for bytes and 'Kb', 'Mb', etc.
- Supports very large sizes up to "Geopbyte"


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
