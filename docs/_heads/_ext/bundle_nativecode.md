---
layout: default
class: Header
title: Bundle-NativeCode   ::= nativecode  ( ',' nativecode )* ( ',' optional ) ?
summary: The Bundle-NativeCode header contains a specification of native code libraries contained in this bundle. 
---

# Bundle-NativeCode

The `Bundle-NativeCode` header specifies native code libraries that are included in the bundle and may be loaded by the OSGi framework. This header lists one or more native code clauses, each describing the path to a native library and optional attributes such as operating system, processor, language, or version constraints.

Example:

```
Bundle-NativeCode: lib/linux-x86/libfoo.so;osname=Linux;processor=x86, lib/win32-x86/foo.dll;osname=Windows;processor=x86
```

A wildcard (`*`) can be used as the last entry to indicate that the bundle can run without native code if no match is found. If a required native library is missing or not found in the JAR, bnd will issue an error.

This header is used for bundles that need to provide platform-specific native libraries alongside Java code.