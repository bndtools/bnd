---
layout: default
class: Analyzer
title: md5 ';' RESOURCE
summary: Calculate MD5 digest of a resource in the bundle
---

## Summary

The `md5` macro calculates the MD5 hash of a resource that exists within the current bundle JAR. The result can be returned as either Base64 (default) or hexadecimal encoding.

## Syntax

```
${md5;<resource-path>[;<encoding>]}
```

## Parameters

- `resource-path` - Path to a resource within the bundle (e.g., "META-INF/MANIFEST.MF")
- `encoding` (optional) - Output encoding: "hex" for hexadecimal, "base64" for Base64 (default)

## Behavior

- Locates the resource within the bundle being built
- Calculates the MD5 digest of the resource contents
- Returns the digest in the specified encoding (Base64 by default)
- Throws an exception if the resource is not found

## Examples

Get MD5 of a resource (Base64):
```
${md5;META-INF/services/com.example.Service}
# Returns Base64-encoded MD5
```

Get MD5 in hexadecimal format:
```
${md5;config/application.properties;hex}
# Returns: "5d41402abc4b2a76b9719d911017c592" (example)
```

Hash manifest file:
```
manifest.md5=${md5;META-INF/MANIFEST.MF;hex}
```

Verify resource integrity:
```
Bundle-ResourceMD5: ${md5;important-config.xml}
```

## Use Cases

- Verifying resource integrity in bundles
- Generating resource checksums for validation
- Creating content-based identifiers
- Implementing resource version checking
- Integrity verification in manifests
- Detecting resource tampering

## Notes

- Resource path is relative to the bundle root
- Only works with resources that are part of the bundle being built
- MD5 is considered cryptographically weak; use for checksums only, not security
- Default encoding is Base64
- Hex encoding produces lowercase hexadecimal strings
- Throws FileNotFoundException if resource doesn't exist
- See also: `${sha1}` and `${digest}` for other hash algorithms



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
