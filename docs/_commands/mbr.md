---
layout: default
title: mbr
summary: |
   Maintain Maven Bnd Repository GAV files
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: 
	   mbr [options]  ...


#### Options: 
- `[ -j --json ]` Output to json instead of human readable when possible

## Available sub-commands 
-  `check` - For each archive in the index, show the available higher versions 
-  `repos` - List the repositories in this workspace 
-  `update` - For each archive in the index, update to a higher version if available in the repository 
-  `verify` - Verify the repositories, this checks if a GAV is defined in multiple repositories or if there are multiple revisions for the same program 

### check 
For each archive in the index, show the available higher versions

#### Synopsis: 
	   check [options]  <archive-glob...>

##### Options: 
- `[ -r --repo <[i> ]` Select the repositories by index (see list for getting the index)
- `[ -s --scope <scope> ]` Specify the scope of the selected version: all, micro (max), minor (max), major (max)
- `[ -S --snapshotlike ]` Include snapshot like versions like -SNAPSHOT, -rc1, -beta12. These are skipped for updated by default

### repos 
List the repositories in this workspace

#### Synopsis: 
	   repos 

### update 
For each archive in the index, update to a higher version if available in the repository

#### Synopsis: 
	   update [options]  <archive-glob...>

##### Options: 
- `[ -d --dry ]` 
- `[ -r --repo <[i> ]` Select the repositories by index (see list for getting the index)
- `[ -s --scope <scope> ]` Specify the scope of the selected version: all, micro (max), minor (max), major (max)
- `[ -S --snapshotlike ]` Include snapshot like versions like -SNAPSHOT, -rc1, -beta12. These are skipped for updated by default

### verify 
Verify the repositories, this checks if a GAV is defined in multiple repositories or if there are multiple revisions for the same program

#### Synopsis: 
	   verify [options]  <archive-glob...>

##### Options: 
- `[ -r --repo <[i> ]` Select the repositories by index (see list for getting the index)

