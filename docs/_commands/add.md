---
layout: default
title: add
summary: |
   Add a project, workspace, plugin or template fragment to the workspace
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: 
	   add  <what> ...

## Available sub-commands 
-  `fragment` - Add template fragment(s) to the current workspace. 
-  `plugin` - Add a plugin 
-  `project` - Create a project in the current workspace 
-  `workspace` - Create a bnd workspace in the current folder. The workspace can also be inialized with a set of template fragments. 

### fragment 
Add template fragment(s) to the current workspace. Leave the name empty to see a list of available templates at the index.\n\nExample:\n bnd add fragment osgi gradle 

#### Synopsis: 
	   fragment [options]  <[name]...>

##### Options: 
- `[ -i --index <string> ]` Optional: URL of an alternative template fragment index, for testing purposes. Default is: https://raw.githubusercontent.com/bndtools/workspace-templates/master/index.bnd

### plugin 
Add a plugin

#### Synopsis: 
	   plugin [options]  <[name]> ...

##### Options: 
- `[ -a --alias <string> ]` 
- `[ -f --force ]` 

### project 
Create a project in the current workspace

#### Synopsis: 
	   project  <name> ...

### workspace 
Create a bnd workspace in the current folder. The workspace can also be inialized with a set of template fragments.

Example:
 bnd add workspace -f osgi -f gradle 'myworkspace'

See https://bnd.bndtools.org/chapters/620-template-fragments.html for more information.

#### Synopsis: 
	   workspace [options]  <name> ...

##### Options: 
- `[ -f --fragment <string>* ]` Specify template fragment(s) by name to install together with the created workspace. Fragments are identified by the 'name' attribute in the index. Specify multiple fragments by repeating the -f option. To see a list of available templates use 'bnd add fragment' without arguments.
- `[ -i --index <string> ]` Optional: URL of an alternative template fragment index, for testing purposes. Default is: https://raw.githubusercontent.com/bndtools/workspace-templates/master/index.bnd

