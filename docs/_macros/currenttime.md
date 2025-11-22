---
layout: default
class: Macro
title: currenttime
summary: Get the current system time as milliseconds since epoch
---

## Summary

The `currenttime` macro returns the current system time as a long integer representing milliseconds since the Unix epoch (January 1, 1970, 00:00:00 UTC). This provides a precise timestamp for build-time operations.

## Syntax

```
${currenttime}
```

## Parameters

None - this macro takes no parameters.

## Behavior

- Returns the current system time in milliseconds
- Uses `System.currentTimeMillis()` internally
- The value represents milliseconds since January 1, 1970, 00:00:00 UTC
- The timestamp is captured at the time the macro is evaluated

## Examples

Capture build time:
```
Build-Time: ${currenttime}
```

Use as a unique identifier component:
```
Build-Id: ${bsn}-${currenttime}
```

Create timestamped filenames:
```
output.file=bundle-${currenttime}.jar
```

Calculate elapsed time (with another timestamp):
```
# In combination with other macros for time calculations
```

## Use Cases

- Recording precise build timestamps
- Creating unique build identifiers
- Timestamping artifacts
- Calculating build durations
- Versioning based on build time
- Creating time-based unique values

## Notes

- The value is in milliseconds, not seconds
- To convert to a readable date format, use the `${long2date}` macro
- The timestamp represents UTC time
- For formatted timestamps, use `${tstamp}` macro instead
- This is different from `${now}` which returns seconds since epoch

---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
