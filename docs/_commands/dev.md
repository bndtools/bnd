---
layout: default
title: dev
summary: |
   Experimental: Live coding. Run 1..n .bndrun files in the OSGi launcher, and continously rebuild all projects in the workspace when changes are detected. If no bndrun is specified, the current project is used for the run specification. An initial full build is done when one project not built is detected.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: 
	   dev [options]  <[bndrun...]>

#### Options: 
- `[ -e --exclude <string;> ]` Exclude files by pattern
- `[ -f --force ]` Force non-incremental
- `[ -p --parallel ]` Do the initial full build in parallel (Experimental)
- `[ -P --project <string> ]` Path to another project than the current project. Only valid if no bndrun is specified
- `[ -s --synctime <long> ]` 
- `[ -t --test ]` Build for test
- `[ -v --verbose ]` prints more processing information
- `[ -V --verify ]` Verify all the dependencies before launching (runpath, runbundles)
- `[ -w --workspace <string> ]` Use the following workspace

