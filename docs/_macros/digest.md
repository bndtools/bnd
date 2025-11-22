---
layout: default
class: Macro
title: digest ';' ALGORITHM ';' FILE
summary: Calculate a cryptographic digest (hash) of a file
---

## Summary

The `digest` macro computes a cryptographic hash of a file using a specified algorithm (such as MD5, SHA-1, SHA-256, etc.). The result is returned as a hexadecimal string.

## Syntax

```
${digest;<algorithm>;<file>}
```

## Parameters

- `algorithm` - The digest algorithm name (e.g., "MD5", "SHA-1", "SHA-256", "SHA-512")
- `file` - Path to the file to hash (relative to project directory)

## Behavior

- Reads the entire file and computes its digest
- Uses Java's MessageDigest API, so any supported algorithm can be used
- Returns the digest as a lowercase hexadecimal string
- File path is resolved relative to the project directory
- Throws an exception if the algorithm is not supported or file cannot be read

## Examples

Calculate MD5 hash:
```
file.md5=${digest;MD5;target/mybundle.jar}
```

Calculate SHA-256 hash:
```
file.sha256=${digest;SHA-256;LICENSE.txt}
```

Calculate SHA-512 for verification:
```
Bundle-Digest-SHA512: ${digest;SHA-512;${@}}
```

Multiple checksums:
```
md5.checksum=${digest;MD5;artifact.jar}
sha1.checksum=${digest;SHA-1;artifact.jar}
sha256.checksum=${digest;SHA-256;artifact.jar}
```

## Use Cases

- Generating checksums for artifact verification
- Creating file integrity manifests
- Implementing build reproducibility checks
- Generating unique identifiers based on file content
- Security verification and tamper detection
- Creating content-addressable storage keys

## Notes

- Common algorithms include: MD5, SHA-1, SHA-256, SHA-384, SHA-512
- The algorithm name is case-insensitive
- MD5 and SHA-1 are considered weak for security purposes; prefer SHA-256 or stronger
- The output is always lowercase hexadecimal
- For large files, this reads the entire file into memory for hashing
- See also: `${md5}` and `${sha1}` macros for specific algorithms


---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
