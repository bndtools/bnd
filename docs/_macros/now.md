---
layout: default
class: Macro
title: now ( 'long' | DATEFORMAT )
summary: Get current date and time in various formats
---

## Summary

The `now` macro returns the current date and time. It can return the raw Date object, milliseconds since epoch, or a formatted date string.

## Syntax

```
${now[;<format>]}
```

## Parameters

- `format` (optional) - Either "long" for milliseconds, a date format pattern, or omit for default Date format

## Behavior

- No argument: Returns default Date string representation
- "long": Returns milliseconds since Unix epoch
- Format pattern: Returns formatted date string using SimpleDateFormat

## Examples

Get current time in millis:
```
${now;long}
# Returns: 1700000000000 (example)
```

Custom format:
```
${now;yyyy-MM-dd HH:mm:ss}
# Returns: "2024-11-20 10:30:45"
```

ISO format:
```
${now;yyyy-MM-dd'T'HH:mm:ss'Z'}
# Returns: "2024-11-20T10:30:45Z"
```

Default format:
```
${now}
# Returns: "Wed Nov 20 10:30:45 UTC 2024"
```

Build timestamp:
```
Build-Time: ${now;yyyy-MM-dd HH:mm z}
```

## Use Cases

- Build timestamps
- Current time logging
- Time-based conditionals
- Unique identifiers with timestamps
- Build metadata

## Notes

- Uses system time zone unless specified in format
- Format uses SimpleDateFormat patterns
- "long" returns milliseconds as number
- Default returns Java Date.toString() format
- See also: `${tstamp}` for more control
- See also: `${currenttime}` for millis only




---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
