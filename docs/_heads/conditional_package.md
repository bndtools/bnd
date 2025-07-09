---
layout: default
title: Conditional-Package PACKAGE-SPEC ( ',' PACKAGE-SPEC ) *
class: Header
summary: |
   Recursively add packages from the class path when referred and when they match one of the package specifications.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `Conditional-Package: com.*`

- Values: `${packages}`

- Pattern: `.*`

<!-- Manual content from: ext/conditional_package.md --><br /><br />

This instruction is equal to using [-conditionalpackage](conditionalpackage.html) except for the fact that the header in addition will be copied into the generated bundle manifest (like all headers beginning with a capital letter).
