---
layout: default
title: -nosubstitution
summary: |
   Setting this to true disables package substitution globally (default is false). That means, that bnd does not calculate Import-Package references for packages exported by the current bundle.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-nosubstitution=true`

- Values: `true,false`

- Pattern: `true|false|TRUE|FALSE`

<!-- Manual content from: ext/nosubstitution.md --><br /><br />

See [Subsitution](/chapters/170-versioning.html#substitution) to learn more about package Substition (a key aspect of OSGi allowing that a package can be both exported and imported).

Note, that package substitution behaviour is enabled by default in bnd for backward-compatibilty. But recommended to disable for most cases these days via `-nosubstitution:true`.


## Example

```
-nosubstitution:true
-exportcontents: *
Import-Package: *
```

This ensures that no `Import-Package` is calculated for any package in `Export-Package`. This is equivalent to `Export-Package: *;-noimport:=true` or `-exportcontents: *;-noimport:=true`.

The advantage is that `-nosubstitution:true` is a global switch, thus a developer cannot forget to add `-noimport:=true` in case of a more fine-grained `Export-Package` declaration with multiple packages. 

## Why is it useful?

It is useful for library authors who **never** want to have any exported packages in `Import-Package`.
Often, library authors who are just using bnd to generate OSGi metadata in their build scripts (e.g. via maven or gradle plugins) but are otherwise unfamiliar with bnd and OSGi, need a simple way to disable this substitution behavior, because it can lead to surprising resolution results (or failures) after deployment (because other bundles providing the same package are pulled in). This is often undesirable in a pure "library" use-case.

This instruction helps making bnd / OSGi adoption easier for library projects just wanting to provide OSGi metadata.
