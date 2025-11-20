---
layout: default
class: Processor
title: native_capability ( ';' ( 'os.name' | 'os.version' | 'os.processor' ) '=' STRING )*
summary: Generate OSGi native capability string for current or specified platform
---

## Summary

The `native_capability` macro generates an OSGi native capability string in the format specified for Provide-Capability or Require-Capability headers. It represents the native platform according to OSGi RFC 188, automatically detecting the current platform or using explicit overrides.

## Syntax

```
${native_capability[;<property>=<value>...]}
```

## Parameters

- `os.name=<value>` (optional) - Override OS name (e.g., "Linux", "Windows 7")
- `os.version=<value>` (optional) - Override OS version (e.g., "3.2.4", "6.1.0")
- `os.processor=<value>` (optional) - Override processor (e.g., "x86-64", "aarch64")

## Behavior

- Automatically detects current platform if no overrides provided
- Generates OSGi native capability clause
- Includes OS name aliases, version, and processor aliases
- Throws exception if required values cannot be determined

## Examples

Auto-detect current platform:
```
${native_capability}
# On Windows 7 x64:
# osgi.native;osgi.native.osname:List<String>="Windows7,Windows 7,Win32";
#   osgi.native.osversion:Version=6.1.0;
#   osgi.native.processor:List<String>="x86-64,amd64,em64t,x86_64"
```

Override OS name:
```
${native_capability;os.name=Linux}
```

Specify complete platform:
```
${native_capability;os.name=Linux;os.version=3.2.4;os.processor=x86-64}
```

Use in capability header:
```
Provide-Capability: ${native_capability}
```

Cross-platform requirement:
```
Require-Capability: ${native_capability;os.name=MacOSX;os.version=10.15.0}
```

## Use Cases

- Declaring native platform capabilities
- Platform-specific bundle requirements
- Cross-compilation targeting
- Native library dependencies
- Platform compatibility specifications
- OSGi native code requirements

## Notes

- Follows OSGi RFC 188 specification
- Automatically provides OS name aliases
- Processor names include common aliases
- Version must be valid OSGi version format
- Throws exception if platform cannot be detected and no overrides provided
- Used with Require-Capability or Provide-Capability headers
- Essential for native code bundles


<hr />
TODO Needs review - AI Generated content

---

**See test cases in [MacroTestsForDocsExamples.java](https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/test/test/MacroTestsForDocsExamples.java)**
