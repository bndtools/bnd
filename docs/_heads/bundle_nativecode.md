---
layout: default
title: Bundle-NativeCode   ::= nativecode  ( ',' nativecode )* ( ',' optional ) ?
class: Header
summary: |
   The Bundle-NativeCode header contains a specification of native code libraries contained in this bundle.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `Bundle-NativeCode: /lib/http.DLL; osname = QNX; osversion = 3.1`

- Pattern: `.*`

### Options 

- `osname` The name of the operating system.
  - Example: `osname=MacOS`

  - Values: `AIX,DigitalUnix,Embos,Epoc32,FreeBSD,HPUX,IRIX,Linux,MacOS,NetBSD,Netware,OpenBSD,OS2,QNX,Solaris,SunOS,VxWorks,Windows95,Win32,Windows98,WindowsNT,WindowsCE,Windows2000,Windows2003,WindowsXP,WindowsVista`

  - Pattern: `.*`


- `osversion` Operating System Version.
  - Example: `osversion=3.1`

  - Pattern: `.*`


- `language` Language ISO 639 code.
  - Example: `language=nl`

  - Pattern: `\p{Upper}{2}`


- `processor` Processor name.
  - Example: `processor=x86`

  - Values: `68k,ARM_LE,arm_le,arm_be,Alpha,ia64n,ia64w,Ignite,Mips,PArisc,PowerPC,Sh4,Sparc,Sparcv9,S390,S390x,V850E,x86,i486,x86-64`

  - Pattern: `.*`


- `selection-filter` The value of this attribute must be a filter expression that indicates if the native code clause should be selected or not.
  - Example: `selection-filter="(com.acme.windowing=win32)"`

  - Pattern: `.*`

<!-- Manual content from: ext/bundle_nativecode.md --><br /><br />

# Bundle-NativeCode

The `Bundle-NativeCode` header specifies native code libraries that are included in the bundle and may be loaded by the OSGi framework. This header lists one or more native code clauses, each describing the path to a native library and optional attributes such as operating system, processor, language, or version constraints.

Example:

```
Bundle-NativeCode: lib/linux-x86/libfoo.so;osname=Linux;processor=x86, lib/win32-x86/foo.dll;osname=Windows;processor=x86
```

A wildcard (`*`) can be used as the last entry to indicate that the bundle can run without native code if no match is found. If a required native library is missing or not found in the JAR, bnd will issue an error.

This header is used for bundles that need to provide platform-specific native libraries alongside Java code.


<hr />
TODO Needs review - AI Generated content
