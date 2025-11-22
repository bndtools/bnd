---
layout: default
class: Macro
title: base64 ';' FILE [';' LONG ]
summary: Encode a file's contents as Base64 text
---

## Summary

The `base64` macro reads a file and returns its contents encoded as a Base64 string. This is useful for embedding binary content or file data directly in text-based configuration files.

## Syntax

```
${base64;<file>[;fileSizeLimit]}
```

## Parameters

- `file` - Path to the file to encode (relative to the project directory)
- `fileSizeLimit` (optional) - Maximum file size in bytes (default: 100,000 bytes)

## Behavior

- Reads the entire file into memory
- Encodes the file contents as Base64
- Enforces a size limit to prevent memory issues with large files
- Throws an exception if the file exceeds the size limit
- File path is resolved relative to the current project directory

## Examples

Encode a small binary file:
```
${base64;resources/icon.png}
```

Encode with custom size limit (1MB):
```
${base64;data/config.bin;1000000}
```

Embed a certificate in a property:
```
certificate.data=${base64;certs/ca-cert.pem}
```

## Use Cases

- Embedding small binary resources in manifest files
- Including certificates or keys in configuration
- Encoding binary data for transmission in text formats
- Creating data URIs for embedded content
- Storing binary configuration data in properties files

## Notes

- Default size limit is 100,000 bytes to prevent memory exhaustion
- For large files, consider referencing the file path instead of embedding
- The encoded output will be approximately 33% larger than the original file



---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
