---
layout: default
class: Processor
title: -readme BOOLEAN | project | workspace | PROPERTIES
summary: Keep the readme file of a project/workspace up-to-date
---

The `-readme` instruction keeps up-to-date the readme file of a project/workspace with the information
derived from the *.bnd files, the manifest and the component/metatype declarations.

The following properties are supported:

| Key                   		| Default       		| Description                                                                                       		      	|
|-------------------------	|-----------------	|-------------------------------------------------------------------------------------------------------------	|
| `include`               	| **true**         	| Stores the readme file in the bundle (only for project)                                                                   			|
| `path`                  	| **./readme.md** 	| Path to the readme file of the project to update                                                                      			 	|
| `workspace-path`                  	| **cnf/../readme.md** 	| Path to the readme file of the workspace to update                                                                      			 	|
| `show-title`             	| **true**          	| Shows a title section                                                                                 		  	|
| `show-description`      	| **true**          	| Shows a short description under the title  (only for project)                                                             			|
| `show-components`         	| **true**          	| Shows a section documenting the components provided by the bundle (only for project)                           		   	|
| `show-contents`         	| **true**          	| Shows a section referencing all the projects in the current workspace (only for workspace)                           		   	|
| `show-contacts`         	| **true**         	| Shows a section listing the contacts                                                                  		   	|
| `show-license`          	| **true**         	| Shows a section for the licences                                                                      		   	|
| `show-copyright`        	| **true**         	| Shows a section for the copyright                                                                     		   	|
| `show-vendor`           	| **true**         	| Shows the vendor information in the footer                                                            		   	|
| `show-all`              	| **true**          	| Shows the version in the footer                                                                       		   	|
| `contacts-message`      	| **""**            	| Shows a custom message under the contact section                                                       		  	|
| `components-message`      	| **""**            	| Shows a custom message under the component section (only for project)                                                      		  	|
| `components-index`        	| **1**             	| Indicates the index at which the component section will be inserted (starting from 0)                        		  	|

If not indicated, all the show-* properties can be set to true, false, project (true for project only) or workspace (true for workspace only), eg:

* Will only show a title for the projects and the workspace:

	`-readme: show-title=true, show-all=false`
	
 * Will only show a title for the projects:

	`-readme: show-title=project, show-all=false`
	
 * Will show nothing; because components section is only for projects (error):

	`-readme: show-components=workspace, show-all=false`
 

The `-readme` instruction will attempt to extract its information in the following way:

* **name**:

	Directly from the `Bundle-Name` header.

* **description**:
	
	Directly from the `Bundle-Description` header.

* **components**:

	From the components and metatype xml declarations.

* **contacts**:

	From the `Bundle-Developers` header using the name, email and role parameters, eg:

		Bundle-Developers: \
			identifier; \
			email=xxx@yyy.zzz; \
			name="Xxx Yyy"; \
			roles="architect,developer"

	The identifier will be used as the contact name or email if no name or email parameter is provided.
  
* **license**:

	From the `Bundle-License` header using the id, the description and the link parameters, eg:

		Bundle-License: \
			Apache-2.0; \
			description="Apache License, Version 2.0"; \
			link="http://www.apache.org/licenses/LICENSE-2.0"

* **copyright**:

	Directly from the `Bundle-Copyright` header.

* **vendor**:

	From the `Bundle-Vendor` header, if the value ends with a HTTP or HTTPS url then the
	vendor name will be a link to this url, eg:

		Bundle-Vendor:  VendorName http://vendor.org/

* **version**:
	
	Directly from the `Bundle-Version` header.

### Note

* In case of a multi modules project, each sub bnd file must be sync with a different readme file, eg:

	bnd.bnd:

		-sub: true
		-readme: true

	api.bnd:

		-readme: path={.}/readme.api.md

	provider.bnd:

		-readme: path={.}/readme.provider.md

* The `path` property can follow the syntax of the `-includeresource` instruction in order to specify the readme file location in the bundle, eg:

		-readme: path='folder/readme.md=readme.api.md'

* The readme file can still be edited manually.

## Example

The following bnd file example:

		Bundle-Name: My Bundle Name
		Bundle-Description: My Bundle Description
		Bundle-Version: 1.0.0
		Bundle-Vendor: Vendor http://vendor.org/
		Bundle-Copyright: Copyright (c) Vendor SARL (2000, ${tstamp;yyyy}) and others. All Rights Reserved.
		Bundle-License:  Apache-2.0; \
			description="Apache License, Version 2.0"; \
			link="http://www.apache.org/licenses/LICENSE-2.0"
		Bundle-Developers: \
			myid; \
			email=xxx@yyy.zzz; \
			name="Xxx Yyy"; \
			roles="architect,developer",\
			myid2; \
			email=aaa@bbb.ccc; \
			name="Aaa Bbb" \

		-readme: \
			components-index=1,\
			contacts-message="Please, feel free to contact us: "

Generates the following readme.md file at the root of the project:

><bnd-name>
>
># My Bundle Name</bnd-name>
>
><bnd-gen>
>
>My Bundle Description</bnd-gen>
><bnd-gen>
>
># Components
>
>## com.test - `enabled`
>
>### Services
>
>This component provides the following services:
>
>|Service	|
>|---	|
>|com.api.Test	|
>
>### Properties
>
>This component has the following properties:
>
>|Name	|Type	|Description	|Default	|
>|---	|---	|---	|---	|
>|lambda	|String	|A lambda property	|""	|
>|one	|String	|A one property	|"one"	|
>|two	|Integer	|	|	|
>|three	|Float[]	|	|3.0, 4.0	|
>
>### Configuration
>
>This component `must be` configured with the following Pids:
>
>* **Pid:	com.test.pid**	-	`factory`
>
>	Properties:	lambda, one
>
>
>### Lifecycle
>
>This component is a `delayed component` with a `bundle` scope, it will be activated if needed as soon as all its requirements will be satisfied.
>
></bnd-gen>
>
><bnd-gen>
>
># Contacts
>
>Please, feel free to contact us: 
>
>|Name	|Contact	|Roles	|
>|---	|---	|---	|
>|Xxx Yyy	|[xxx@yyy.zzz](mailto:xxx@yyy.zzz)	|architect, developer	|
>|Aaa Bbb	|[aaa@bbb.ccc](mailto:aaa@bbb.ccc)	|	|
></bnd-gen>
>
><bnd-gen>
>
># License
>
>[Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0)
>
>Apache License, Version 2.0</bnd-gen>
>
><bnd-copyright>
>
># Copyright
>
>Copyright (c) Vendor SARL (2000, 2017) and others. All Rights Reserved.</bnd-copyright>
>
><bnd-foot>
>
>---
>[Vendor ](http://vendor.org/) - version 1.0.0</bnd-foot>
>
>