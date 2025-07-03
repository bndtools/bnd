---
layout: default
title: -metatypeannotations-options SELECTORS
class: Builder
summary: |
   Restricts the use of Metatype Annotation to a minimum version.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-metatypeannotations-options: version;minimum=1.2.0`

- Values: `(inherit|felixExtensions|extender|nocapabilities|norequirements|version)`

- Pattern: `.*`

<!-- Manual content from: ext/metatypeannotations_options.md --><br /><br />

```properties
-metatypeannotations-options: version;minimum=1.2.0
```

Analogous to `-dsannotations-options`, this will also restrict the use of OSGi Metatype annotations to minimum 1.2.0 version. The version number denotes that the users can use any version equal to or higher than 1.2.0, provided that the users have the Metatype annotations included on the build path.
