---
layout: default
title: -exportcontents PACKAGE-SPEC, ( ',' PACKAGE-SPEC )*
class: Project
summary: |
   Exports the given packages but does not try to include them from the class path. The packages should be loaded with alternative means.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-exportcontents=!*impl*,*;version=3.0`

- Pattern: `.*`

<!-- Manual content from: ext/exportcontents.md --><br /><br />

Exports the given packages but does not try to include them from the class path. The packages should be loaded with alternative means. The syntax is similar to the [`Export-Package`](/heads/export_package.html) header.


`Export-Package` = [`-includepackage`](/instructions/includepackage.html) + `-exportcontents` ([source](https://bnd.discourse.group/t/more-information-about-exportcontent/214/2)). 

That is, `Export-Package` will add packages to the bundle, perhaps from (other) `.jar` files on the classpath, and also add those packages to the `Export-Package` manifest header. 

`-exportcontents` will *only* add packages which are *already* part of the bundle to the `Export-Package` manifest header.

```
-exportcontents: com.example.api;
```

See the [packages](/macros/packages.html) macro, which is useful in combination with `-exportcontents`.

## Use Cases

So `-exportcontent` is appropriate for Maven and Gradle (non-Bnd workspace) builds where the content of the bundle is being managed by normal Maven or Gradle means.
