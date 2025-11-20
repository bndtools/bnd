---
layout: default
class: Analyzer
title: sha1 ';' RESOURCE
summary: Calculate SHA-1 digest of a resource in the bundle
---

## Summary

The `sha1` macro calculates the SHA-1 hash of a resource that exists within the current bundle JAR. The result can be returned as either Base64 (default) or hexadecimal encoding.

## Syntax

```
${sha1;<resource-path>[;<encoding>]}
```

## Parameters

- `resource-path` - Path to a resource within the bundle (e.g., "META-INF/MANIFEST.MF")
- `encoding` (optional) - Output encoding: "hex" for hexadecimal, "base64" for Base64 (default)

## Behavior

- Locates the resource within the bundle being built
- Calculates the SHA-1 digest of the resource contents
- Returns the digest in the specified encoding (Base64 by default)
- Throws an exception if the resource is not found

## Examples

Get SHA-1 of a resource (Base64):
```
${sha1;META-INF/services/com.example.Service}
# Returns Base64-encoded SHA-1
```

Get SHA-1 in hexadecimal format:
```
${sha1;config/application.properties;hex}
# Returns: "356a192b7913b04c54574d18c28d46e6395428ab" (example)
```

Hash manifest file:
```
manifest.sha1=${sha1;META-INF/MANIFEST.MF;hex}
```

Verify resource integrity:
```
Bundle-ResourceSHA1: ${sha1;important-config.xml}
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
- SHA-1 is stronger than MD5 but still not recommended for security-critical applications
- Default encoding is Base64
- Hex encoding produces lowercase hexadecimal strings
- Throws FileNotFoundException if resource doesn't exist
- See also: `${md5}` and `${digest}` for other hash algorithms



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
