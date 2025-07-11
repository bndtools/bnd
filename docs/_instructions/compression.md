---
layout: default
title: -compression DEFLATE | STORE
class: Builder
summary: |
   Set the compression level for the generated JAR, the default is DEFLATE
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-compression: STORE`

- Values: `DEFLATE,STORE`

- Pattern: `.*`

### Directives 

- `DEFLATE`
  - Values: `DEFLATE`

  - Pattern: `\QDEFLATE\E`


- `STORE`
  - Values: `STORE`

  - Pattern: `\QSTORE\E`

<!-- Manual content from: ext/compression.md --><br /><br />

When a Jar is build it has default DEFLATE compression with the default level. Using this instruction you 
can override the compression method.

    -compression: STORE


    
