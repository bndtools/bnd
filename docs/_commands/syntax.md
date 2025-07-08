---
layout: default
title: syntax
summary: |
   Access the internal bnd database of keywords and options
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: #
	   syntax [options]  <header|instruction> ...

#### Options: #
- `[ -w --width <int> ]` The width of the printout

<!-- Manual content from: ext/syntax.md --><br /><br />

## Examples

	biz.aQute.bnd (master)$ bnd syntax Bundle-Version
		
	[Bundle-Version]
		The Bundle-Version header specifies the version of this bundle.

		Pattern                               : [0-9]{1,9}(\.[0-9]{1,9}(\.[0-9]{1,9}(\.[0-9A-Za-z_-]+)?)?)?
		Example                               : Bundle-Version: 1.23.4.build200903221000
