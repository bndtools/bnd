---
layout: default
class: Macro
title: tstamp ( ';' DATEFORMAT ( ';' TIMEZONE ( ';' LONG )? )? )?
summary: Generate a formatted timestamp
---

## Summary

The `tstamp` macro creates a formatted timestamp string using a custom date format pattern, timezone, and optional timestamp value.

## Syntax

```
${tstamp[;<format>[;<timezone>[;<millis>]]]}
```

## Parameters

- `format` (optional) - Date format pattern (default: "yyyyMMddHHmm")
- `timezone` (optional) - Timezone ID (default: "UTC")
- `millis` (optional) - Timestamp in milliseconds (default: current time)

## Behavior

- Formats current time by default
- Uses SimpleDateFormat patterns
- Default format: "yyyyMMddHHmm"
- Default timezone: UTC
- All parameters are optional

## Examples

Default format (UTC):
```
${tstamp}
# Returns: "202411201030" (example)
```

Custom format:
```
${tstamp;yyyy-MM-dd HH:mm:ss}
# Returns: "2024-11-20 10:30:45"
```

With timezone:
```
${tstamp;yyyy-MM-dd HH:mm;America/New_York}
# Returns: "2024-11-20 05:30"
```

ISO 8601 format:
```
${tstamp;yyyy-MM-dd'T'HH:mm:ss'Z'}
# Returns: "2024-11-20T10:30:45Z"
```

Specific timestamp:
```
${tstamp;yyyy-MM-dd;;1700000000000}
# Formats the provided timestamp
```

Build timestamp in manifest:
```
Build-Timestamp: ${tstamp;yyyy-MM-dd HH:mm:ss z;UTC}
```

## Use Cases

- Creating build timestamps
- Generating unique version qualifiers
- Formatting dates for manifests
- Creating timestamped file names
- Documentation generation
- Build reproducibility tracking

## Notes

- Uses Java SimpleDateFormat patterns
- Default timezone is UTC
- For current time in millis, use `${currenttime}`
- For conversion from millis, use `${long2date}`
- Format patterns are locale-independent
- Common patterns: yyyy (year), MM (month), dd (day), HH (hour 24), mm (minute), ss (second)
- See also: `${currenttime}`, `${long2date}`, `${now}`



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
