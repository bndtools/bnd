---
layout: default
title: digest
summary: |
   Digests a number of files
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: #
	   digest [options]  <file...>

#### Options: #
- `[ -a --algorithm <alg>* ]` Specify the algorithms
- `[ -b --b64 ]` Show base64 output
- `[ -h --hex ]` Show hex output (default)
- `[ -n --name ]` Show the file name
- `[ -p --process ]` Show process info

<!-- Manual content from: ext/digest.md --><br /><br />

## Examples
    biz.aQute.bnd (master)$ bnd digest generated/biz.aQute.bnd.jar 
    16B415286B53FA499BD7B2684A93924CA7C198C8
