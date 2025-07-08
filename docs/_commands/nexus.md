---
layout: default
title: nexus
summary: |
   Nexus repository command. Provides a number of sub commands to manipulate a Nexus repository.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: #
	   nexus [options]  <sub-cmd> ...

#### Options: #
- `[ -c --compatible <compatible> ]` 
- `[ -s --settings <string> ]` Specify the connection-settings for the HttpClient. Default looks for the normal settings files.
- `[ -u --url <uri> ]` Specify the URL of the Nexus repository.

## Available sub-commands #
-  `createstaging` - Create a staging repository. The profileId specifies a particular profile. If you go to nexus, select the staging profiles, and then select the profile you want to use. The profile id is then in the url. 
-  `delete` - Delete a file in a staging repository by id 
-  `fetch` - Fetch a file to staging repository by id 
-  `files` -   
-  `index` -   
-  `sign` -   
-  `upload` - Upload a file to staging repository by id 

### createstaging #
Create a staging repository. The profileId specifies a particular profile. If you go to nexus, select the staging profiles, and then select the profile you want to use. The profile id is then in the url.

#### Synopsis: #
	   createstaging [options]  <profileId>

##### Options: #
- `[ -d --description <string> ]` 

### delete #
Delete a file in a staging repository by id

#### Synopsis: #
	   delete [options]  <repositoryId> <remotepath_or_gav>

##### Options: #
- `[ -f --force ]` 

### fetch #
Fetch a file to staging repository by id

#### Synopsis: #
	   fetch [options]  <repositoryId> <remotepath_or_gav>

##### Options: #
- `[ -f --force ]` 
- `[ -o --output <file> ]` 

### files #
#### Synopsis: #
	   files [options]  <files...>

##### Options: #
- `[ -e --exclude <string> ]` A resource URI is only include if the include pattern appears in the path and the exclude does not appear
- `[ -i --include <string> ]` A resource URI is only include if the include pattern appears in the path and the exclude does not appear
- `[ -r --relative ]` 

### index #
#### Synopsis: #
	   index [options]  ...


##### Options: #
- `[ -d --depth <int> ]` 
- `[ -n --name <string> ]` 
- `[ -o --output <string> ]` 
- `[ -r --referal <uri> ]` 

### sign #
#### Synopsis: #
	   sign [options]  <path...>

##### Options: #
- `[ -c --command <string> ]` Specify the path to the gpg command. The gpg path can also be specified using the 'gpg' system property or the 'GPG' environment variable. Defaults to 'gpg'.
- `[ -f --from <uri> ]` Specify the URL to a Nexus repository from which to obtain the artifacts to sign. Defaults to signing the specified paths to the sign subcommand.
- `[ -i --include <string> ]` Specify the include pattern for artifacts from the '--from' option. Defaults to '**'.
- `[ -k --key <string> ]` Specify the local-user USER-ID for signing. Defaults to signing with the default key.
- `[ -p --password <string> ]` Specify the passpharse to the gpg command. Defaults to reading stdin for the passphrase.
- `[ -s --show ]` Only compute and display the signatures but do not upload them.
- `[ -t --threads <int> ]` Specify the number of threads to use when downloading, signing, and uploading artifacts. Defaults to one thread.
- `[ -x --xclude <string> ]` Specify the exclude pattern for artifacts from the '--from' option. Defaults to no exclude pattern.

### upload #
Upload a file to staging repository by id

#### Synopsis: #
	   upload [options]  <repositoryId> <remotepath_or_gav> <file>

##### Options: #
- `[ -f --force ]` 

