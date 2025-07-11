---
layout: default
title: repo
summary: |
   Access to the repositories. Provides a number of sub commands to manipulate the repository (see repo help) that provide access to the installed repos for the current project.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: 
	   repo [options]  <sub-cmd> ...

#### Options: 
- `[ -c --cache ]` Include the cache repository
- `[ -f --filerepo <string>* ]` Add a File Repository
- `[ -m --maven ]` Include the maven repository
- `[ -p --project <string> ]` Specify a project
- `[ -r --release <glob> ]` Override the name of the release repository (-releaserepo)
- `[ -w --workspace <string> ]` Workspace (a standalone bndrun file or a sbdirectory of a workspace (default is the cwd)

## Available sub-commands 
-  `copy` -   
-  `diff` - Diff jars (or show tree) 
-  `get` - Get an artifact from a repository. 
-  `index` -   
-  `list` - List all artifacts from the current repositories with their versions 
-  `put` - Put an artifact into the repository after it has been verified. 
-  `refresh` - Refresh refreshable repositories 
-  `repos` - List the current repositories 
-  `sync` -   
-  `topom` -   
-  `versions` - Displays a list of versions for a given bsn that can be found in the current repositories. 

### copy 
#### Synopsis: 
	   copy [options]  <source> <dest> <bsn[:version]...>

##### Options: 
- `[ -d --dry ]` Do not really copy but trace the steps
- `[ -f --filter <string;> ]` 
- `[ -F --force ]` 
- `[ -p --project <string> ]` Identify another project
- `[ -q --quiet ]` 
- `[ -s --standalone <string> ]` A stanalone bndrun file

### diff 
Diff jars (or show tree)

#### Synopsis: 
	   diff [options]  <newer repo> <[older repo]>

##### Options: 
- `[ -a --added ]` Just additions (no removes)
- `[ -A --all ]` Both add and removes
- `[ -d --diff ]` Formatted like diff
- `[ -f --full ]` Show full diff tree (also wen entries are equal)
- `[ -j --json ]` Serialize to JSON
- `[ -r --remove ]` Just removes (no additions)

### get 
Get an artifact from a repository.

#### Synopsis: 
	   get [options]  <bsn> <[range]>

##### Options: 
- `[ -f --from <instruction> ]` 
- `[ -l --lowest ]` 
- `[ -o --output <string> ]` Where to store the artifact

### index 
#### Synopsis: 
	   index [options]  ...


##### Options: 
- `[ -f --from <instruction> ]` A glob expression on the source repo, default is all repos
- `[ -n --name <string> ]` The name of the output file. If not set will show on the console
- `[ -o --output <string> ]` Output file (will be compressed)
- `[ -q --query <string> ]` Optional search term for the list of bsns (given to the repo)
- `[ -Q --quiet ]` No output

### list 
List all artifacts from the current repositories with their versions

#### Synopsis: 
	   list [options] 

##### Options: 
- `[ -f --from <instruction> ]` A glob expression on the source repo, default is all repos
- `[ -n --noversions ]` Do not list the versions, just the bsns
- `[ -q --query <string> ]` Optional search term for the list of bsns (given to the repo)

### put 
Put an artifact into the repository after it has been verified.

#### Synopsis: 
	   put [options]  <<jar>...>

##### Options: 
- `[ -f --force ]` Put in repository even if verification fails (actually, no verification is done).

### refresh 
Refresh refreshable repositories

#### Synopsis: 
	   refresh [options] 

##### Options: 
- `[ -q --quiet ]` 

### repos 
List the current repositories

#### Synopsis: 
	   repos 

### sync 
#### Synopsis: 
	   sync [options]  ...


##### Options: 
- `[ -d --dest <string> ]` 
- `[ -g --gavs <string;> ]` 
- `[ -s --source <string;> ]` 
- `[ -w --workspace <string> ]` 

### topom 
#### Synopsis: 
	   topom [options]  <repo> <name>

##### Options: 
- `[ -d --dependencyManagement ]` Use the dependency management section
- `[ -o --output <string> ]` Output file
- `[ -p --parent <string> ]` The parent of the pom (default none.xml)

### versions 
Displays a list of versions for a given bsn that can be found in the current repositories.

#### Synopsis: 
	   versions  <bsn>

