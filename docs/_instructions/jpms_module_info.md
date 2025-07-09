---
layout: default
title: -jpms-module-info modulename [; version=<version>] [; access=OPEN|SYNTHETIC|MANDATED]
class: JPMS
summary: |
   Used to generate the `module-info.class`
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-jpms-module-info: foo.module;version=5.4.1; access="OPEN,SYNTHETIC"`

- Pattern: `.*`

<!-- Manual content from: ext/jpms_module_info.md --><br /><br />

See [jpms](../chapters/330-jpms.html) for an overview and the detailed rules how the `module-info.class` file is
calculated. 

The `-jpms-module-info` instruction is a single parameter

    -jpms-module-info   ::= module-name [ ';version=' VERSION ] access
    access              ::= `;access=' '"' item ( ',' item ) * '"'
    item                ::= 'OPEN' | 'SYNTHETIC' | 'MANDATED'
    
* Key – The key is the module name. If not set, the `Automatic-Module-Name` is used, or if that one is not set, the Bundle-SymbolicName.
* `version` – The version, otherwise the bundle version is used.
* `access` – The access flags. These indicate the access mode of the module.

For example:

    -jpms-module-info: foo.module;version=5.4.1; access="OPEN,SYNTHETIC"
 
 
