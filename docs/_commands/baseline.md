---
layout: default
title: baseline
summary: |
   Compare a newer bundle to a baselined bundle and provide versioning advice
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: #
	   baseline [options]  <[newer jar]> <[older jar]>

#### Options: #
- `[ -a --all ]` Show all, also unchanged
- `[ -d --diff ]` Show any differences
- `[ -f --fixup <string> ]` Output file with fixup info
- `[ -p --packages <string> ]` Packages to baseline (comma delimited)
- `[ -q --quiet ]` Be quiet, only report errors
- `[ -v --verbose ]` On changed, list API changes

<!-- Manual content from: ext/baseline.md --><br /><br />

## Examples
`bnd baseline --diff newer.jar older.jar`
