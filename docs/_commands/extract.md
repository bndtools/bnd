---
layout: default
title: extract
summary: |
   Extract files from a JAR file, equivalent jar command x[vf] (syntax supported)
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: #
	   extract [options]  ...


#### Options: #
- `[ -c --cdir <string> ]` Directory where to store
- `[ -f --file <string> ]` Jar file (f option)
- `[ -v --verbose ]` Verbose (v option)

<!-- Manual content from: ext/extract.md --><br /><br />

## Examples

    biz.aQute.bnd (master)$ bnd extract -c generated/tmp generated/biz.aQute.bnd.jar 
