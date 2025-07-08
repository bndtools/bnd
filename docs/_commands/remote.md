---
layout: default
title: remote
summary: |
   Communicates with the remote agent
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: #
	   remote [options]  ...


#### Options: #
- `[ -h --host <string> ]` Specify the host to commicate with, default is 'localhost'
- `[ -p --port <int> ]` Specify the port to commicate with, default is 29998

## Available sub-commands #
-  `distro` -   
-  `framework` -   
-  `install` - Install/update the specified bundle. 
-  `list` - List the bundles installed in the remote framework 
-  `ping` -   
-  `revisions` -   
-  `start` - Start the specified bundles 
-  `stop` - Stop the specified bundles 
-  `uninstall` - Uninstall the specified bundles 

### distro #
#### Synopsis: #
	   distro [options]  <bsn> <[version]>

##### Options: #
- `[ -c --copyright <string> ]` The Bundle-Copyright header
- `[ -d --description <string> ]` The Bundle-Description header
- `[ -l --license <string> ]` The Bundle-License header
- `[ -o --output <string> ]` Output name
- `[ -v --vendor <string> ]` The Bundle-Vendor header
- `[ -x --xml ]` Generate xml instead of a jar with manifest

### framework #
#### Synopsis: #
	   framework 

### install #
Install/update the specified bundle.

#### Synopsis: #
	   install [options]  <filespec...>

##### Options: #
- `[ -l --location <string;> ]` By default the location is 'manual:<bsn>'. You can specify multiple locations when installing multiple bundles

### list #
List the bundles installed in the remote framework

#### Synopsis: #
	   list [options]  ...


##### Options: #
- `[ -j --json ]` Specify to return the output as JSON

### ping #
#### Synopsis: #
	   ping 

### revisions #
#### Synopsis: #
	   revisions  <bundleid...>

### start #
Start the specified bundles

#### Synopsis: #
	   start  <bundleId...>

### stop #
Stop the specified bundles

#### Synopsis: #
	   stop  <bundleId...>

### uninstall #
Uninstall the specified bundles

#### Synopsis: #
	   uninstall  <bundleId...>

