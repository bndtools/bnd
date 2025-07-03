---
layout: default
title: grep
summary: |
   Grep the manifest of bundles/jar files. 
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: #
	   grep [options]  <pattern> <file...>

#### Options: #
- `[ -b --bsn ]` Search in bsn
- `[ -e --exports ]` Search in exports
- `[ -h --headers <string>* ]` Set header(s) to search, can be wildcarded. The default is all headers (*).
- `[ -i --imports ]` Search in imports
- `[ -r --resources <string>* ]` Search path names of resources. No resources are included unless expressly specified.

<!-- Manual content from: ext/grep.md --><br /><br />

## Examples
    biz.aQute.bnd (master)$ bnd grep -h "*" "settings" generated/*.jar
                generated/biz.aQute.bnd.jar :      Private-Package ...ute.lib.[settings]...

   
