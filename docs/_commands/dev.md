---
layout: default
title: dev
summary: |
   Live coding. Run a .bndrun in the OSGi launcher, and continously rebuild all projects in the workspace when changes are detected. If no bndrun is specified, the current project is used for the run specification
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: 
	   dev [options]  <[bndrun]>

#### Options: 
- `[ -e --exclude <string;> ]` Exclude files by pattern
- `[ -f --force ]` Force non-incremental
- `[ -p --parallel ]` Build in parallel (Experimental)
- `[ -P --project <string> ]` Path to another project than the current project. Only valid if no bndrun is specified
- `[ -s --synctime <long> ]` 
- `[ -t --test ]` Build for test
- `[ -v --verbose ]` prints more processing information
- `[ -V --verify ]` Verify all the dependencies before launching (runpath, runbundles)
- `[ -w --workspace <string> ]` Use the following workspace

