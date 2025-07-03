---
layout: default
title: com
summary: |
   Commands to verify and check the communications settings for the http client.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: #
	   com  ...


## Available sub-commands #
-  `clear` - Clear the cached file that is associated with the givenURI 
-  `info` - Show the information used by the Http Client to get aremote file 
-  `settings` - Show the bnd -connection-settings 

### clear #
Clear the cached file that is associated with the givenURI

#### Synopsis: #
	   clear  ...


### info #
Show the information used by the Http Client to get aremote file

#### Synopsis: #
	   info [options]  <url...>

##### Options: #
- `[ -c --cached ]` Use the cache option. If this option is not used then thefile will be refreshed
- `[ -o --output <string> ]` Save the remote content in the given file. If you use '.'then	the file shown to the output

### settings #
Show the bnd -connection-settings

#### Synopsis: #
	   settings  ...


