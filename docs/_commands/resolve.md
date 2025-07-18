---
layout: default
title: resolve
summary: |
   Resolve a number of bndrun files (either standalone or based on the workspace) and print the bundles 
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: 
	   resolve [options]  <<path>...>

#### Options: 
- `[ -b --bundles ]` Print out the bundles
- `[ -d --dot ]` Create a dependency file
- `[ -e --exclude <string;> ]` Exclude files by pattern
- `[ -f --files ]` Print out the bundle files
- `[ -o --optionals ]` Show the optionals
- `[ -p --project <string> ]` Identify another project
- `[ -q --quiet ]` Quiet
- `[ -r --runorder <runorder> ]` Override the -runorder
- `[ -u --urls ]` Print out the bundle urls
- `[ -v --verbose ]` prints more processing information
- `[ -w --workspace <string> ]` Use the following workspace
- `[ -W --write ]` Write -runbundles instruction back to the file
- `[ -x --xchange ]` Fail on changes

## Available sub-commands 
-  `dot` - Create a dot file 
-  `repos` -   
-  `resolve` - Resolve a bndrun file 
-  `validate` - Resolve a repository index against a base to determine if the index is 'complete' 

### dot 
Create a dot file

#### Synopsis: 
	   dot [options]  <bndrun-file>

##### Options: 
- `[ -o --output <string> ]` Send to file
- `[ -q --quiet ]` Quiet
- `[ -r --runorder <runorder> ]` Override the -runorder

### repos 
#### Synopsis: 
	   repos [options]  ...


##### Options: 
- `[ -w --workspace <string> ]` 

### resolve 
Resolve a bndrun file

#### Synopsis: 
	   resolve [options]  <<path>...>

##### Options: 
- `[ -b --bundles ]` Print out the bundles
- `[ -d --dot ]` Create a dependency file
- `[ -e --exclude <string;> ]` Exclude files by pattern
- `[ -f --files ]` Print out the bundle files
- `[ -o --optionals ]` Show the optionals
- `[ -p --project <string> ]` Identify another project
- `[ -q --quiet ]` Quiet
- `[ -r --runorder <runorder> ]` Override the -runorder
- `[ -u --urls ]` Print out the bundle urls
- `[ -v --verbose ]` prints more processing information
- `[ -w --workspace <string> ]` Use the following workspace
- `[ -W --write ]` Write -runbundles instruction back to the file
- `[ -x --xchange ]` Fail on changes

### validate 
Validate an OBR file by trying to resolve each entry against itself

#### Synopsis: 
	   validate [options]  <[index-path]>

##### Options: 
- `[ -a --all ]` Include all output details
- `[ -c --capabilities <parameters> ]` Specify a set of capabilities provided by the base
- `[ -C --core <osgi_core> ]` Specify the framework version used as part of the base, [R4_0_1 R4_2_1 R4_3_0 R4_3_1 R5_0_0 R6_0_0 R7_0_0 R8_0_0]
- `[ -e --ee <ee> ]` Specify the execution environment used as part of the base, default is JavaSE_1_8
- `[ -f --failedshow ]` Show resolution failed errors
- `[ -p --packages <parameters> ]` Specify a set of packages provided by the base
- `[ -s --system <string> ]` Specify a system file used as the base (more commonly referred to as a 'distro')
- `[ -u --unused ]` Show any unused entries. This will only try to resolve the workspace entries and then list the entries in the repos that are not used
- `[ -x --xref ]` List cross reference: mimssing-req -> resources*

