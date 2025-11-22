---
layout: default
class: Macro
title: long2date
summary: Convert a millisecond timestamp to a readable date string
---

## Summary

The `long2date` macro converts a long integer representing milliseconds since the Unix epoch (January 1, 1970, 00:00:00 UTC) into a human-readable date string.

## Syntax

```
${long2date;<milliseconds>}
```

## Parameters

- `milliseconds` - A long integer representing milliseconds since epoch

## Behavior

- Parses the input as a long integer
- Converts to a formatted date string
- Returns "not a valid long" if parsing fails
- Uses a standard date format for output

## Examples

Convert current time:
```
${long2date;${currenttime}}
# Returns: "Wed Nov 20 10:30:45 UTC 2024" (example)
```

Convert stored timestamp:
```
Build-Date: ${long2date;1700000000000}
# Returns human-readable date
```

Format file modification time:
```
Modified: ${long2date;${fmodified;${@}}}
```

Display build timestamp:
```
${long2date;${tstamp}}
```

## Use Cases

- Converting timestamps to readable dates
- Displaying build times in manifests
- Formatting file modification times
- Debugging timestamp values
- Creating human-readable logs
- Documentation generation with dates

## Notes

- Input must be milliseconds (not seconds)
- Returns "not a valid long" for invalid input
- Date format is standard Java Date.toString() format
- The output format is not customizable in this macro
- For custom date formatting, use `${tstamp}` macro
- See also: `${currenttime}` for getting current time in milliseconds
- See also: `${tstamp}` for formatted timestamps
- See also: `${now}` for seconds since epoch




---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
